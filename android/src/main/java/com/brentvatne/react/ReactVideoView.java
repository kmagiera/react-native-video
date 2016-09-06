package com.brentvatne.react;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ThemedReactContext;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

import java.util.HashMap;
import java.util.Map;

public class ReactVideoView extends ScalableVideoView implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener, LifecycleEventListener {

    private static int sWakelockSemaphore = 0;

    private ThemedReactContext mThemedReactContext;
    private ReactVideoEventDispatcher mEventDispatcher;

    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;

    private String mSrcUriString = null;
    private String mSrcType = "mp4";
    private boolean mSrcIsNetwork = false;
    private boolean mSrcIsAsset = false;
    private ScalableType mResizeMode = ScalableType.LEFT_TOP;
    private boolean mRepeat = false;
    private boolean mPaused = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private float mRate = 1.0f;
    private boolean mPlayInBackground = false;

    private boolean mMediaPlayerValid = false; // True if mMediaPlayer is in prepared, started, paused or completed state.
    private int mVideoDuration = 0;
    private int mVideoBufferedDuration = 0;

    private float mLastReportedPosition = Float.NaN;
    private float mLastReportedDuration = Float.NaN;

    private boolean mStopped = false;
    private boolean mHasSetWakelock = false;

    private final ReactApplicationContext mAppContext;

    public ReactVideoView(ThemedReactContext themedReactContext, ReactApplicationContext appCtx) {
        super(themedReactContext);

        mAppContext = appCtx;
        mThemedReactContext = themedReactContext;
        mEventDispatcher = new ReactVideoEventDispatcher(this, themedReactContext);
        themedReactContext.addLifecycleEventListener(this);

        initializeMediaPlayerIfNeeded();
        setSurfaceTextureListener(this);

        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayerValid) {
                    sendProgressEvent();
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 250);
                    }
                }
            }
        };
    }

    private void initializeMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayerValid = false;
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        }
    }

    private void startReportingProgress() {
        mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);
        mProgressUpdateHandler.post(mProgressUpdateRunnable);
    }

    private void sendProgressEvent() {
        float position = mMediaPlayer.getCurrentPosition() / 1000.0f;
        float duration = mVideoBufferedDuration / 1000.0f;
        if (mLastReportedPosition != position || mLastReportedDuration != duration) {
            mLastReportedPosition = position;
            mLastReportedDuration = duration;
            mEventDispatcher.dispatchProgressEvent(position, duration);
        }
        if (mStopped) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void setSrc(final String uriString, final String type, final boolean isNetwork, final boolean isAsset) {
        mSrcUriString = uriString;
        mSrcType = type;
        mSrcIsNetwork = isNetwork;
        mSrcIsAsset = isAsset;

        mMediaPlayerValid = false;
        mVideoDuration = 0;
        mVideoBufferedDuration = 0;

        initializeMediaPlayerIfNeeded();
        mMediaPlayer.reset();

        try {
            if (isNetwork) {
                // Use the shared CookieManager to access the cookies
                // set by WebViews inside the same app
                CookieManager cookieManager = CookieManager.getInstance();

                Uri parsedUrl = Uri.parse(uriString);
                Uri.Builder builtUrl = parsedUrl.buildUpon();

                String cookie = cookieManager.getCookie(builtUrl.build().toString());

                Map<String, String> headers = new HashMap<String, String>();

                if (cookie != null) {
                    headers.put("Cookie", cookie);
                }

                setDataSource(mThemedReactContext, parsedUrl, headers);
            } else if (isAsset) {
                if (uriString.startsWith("content://")) {
                    Uri parsedUrl = Uri.parse(uriString);
                    setDataSource(mThemedReactContext, parsedUrl);
                } else {
                    setDataSource(uriString);
                }
            } else {
                setRawData(mThemedReactContext.getResources().getIdentifier(
                        uriString,
                        "raw",
                        mThemedReactContext.getPackageName()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        mEventDispatcher.dispatchLoadStartEvent(uriString, type, isNetwork);

        prepareAsync(this);

    }

    public void setResizeModeModifier(final ScalableType resizeMode) {
        mResizeMode = resizeMode;

        if (mMediaPlayerValid) {
            setScalableType(resizeMode);
            invalidate();
        }
    }

    public void setRepeatModifier(final boolean repeat) {
        mRepeat = repeat;

        if (mMediaPlayerValid) {
            setLooping(repeat);
        }
    }

    public void setPausedModifier(final boolean paused) {
        mPaused = paused;

        if (!mMediaPlayerValid) {
            return;
        }

        if (mPaused) {
            if (mMediaPlayer.isPlaying()) {
                pause();
                setWakeLock(false);
            }
        } else {
            if (!mMediaPlayer.isPlaying()) {
                start();
                setWakeLock(true);
            }
        }
        startReportingProgress();
    }

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;

        if (!mMediaPlayerValid) {
            return;
        }

        if (mMuted) {
            setVolume(0, 0);
        } else {
            setVolume(mVolume, mVolume);
        }
    }

    public void setVolumeModifier(final float volume) {
        mVolume = volume;
        setMutedModifier(mMuted);
    }

    public void setRateModifier(final float rate) {
        mRate = rate;

        if (mMediaPlayerValid) {
            // TODO: Implement this.
            Log.e(ReactVideoViewManager.REACT_CLASS, "Setting playback rate is not yet supported on Android");
        }
    }

    public void applyModifiers() {
        setResizeModeModifier(mResizeMode);
        setRepeatModifier(mRepeat);
        setPausedModifier(mPaused);
        setMutedModifier(mMuted);
//        setRateModifier(mRate);
    }

    public void setPlayInBackground(final boolean playInBackground) {
        mPlayInBackground = playInBackground;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mStopped) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            return;
        }
        mMediaPlayerValid = true;
        mVideoDuration = mp.getDuration();

        setWakeLock(true);

        mEventDispatcher.dispatchLoadEvent(
                mVideoDuration / 1000.0f,
                mp.getCurrentPosition() / 1000.0f,
                mp.getVideoWidth(),
                mp.getVideoHeight());

        applyModifiers();
        startReportingProgress();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mEventDispatcher.dispatchErrorEvent(what, extra);
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                mEventDispatcher.dispatchStalledEvent();
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                mEventDispatcher.dispatchResumeEvent();
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                mEventDispatcher.dispatchReadyForDisplayEvent();
                break;

            default:
        }
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mVideoBufferedDuration = (int) Math.round((double) (mVideoDuration * percent) / 100.0);
    }

    @Override
    public void seekTo(int msec) {
        if (mMediaPlayerValid) {
            mEventDispatcher.dispatchSeekEvent(getCurrentPosition() / 1000.0f, msec / 1000.0f);

            super.seekTo(msec);
            startReportingProgress();
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        startReportingProgress();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        setWakeLock(false);
        mEventDispatcher.dispatchEndEvent();
    }

    private void setWakeLock(boolean wakeLockEnabled) {
        if (mAppContext.getCurrentActivity() != null && mAppContext.getCurrentActivity().getWindow() != null) {
            if (wakeLockEnabled && !mHasSetWakelock) {
                mHasSetWakelock = true;
                sWakelockSemaphore++;

            } else if (!wakeLockEnabled && mHasSetWakelock) {
                mHasSetWakelock = false;
                sWakelockSemaphore--;
            }
            if (sWakelockSemaphore == 1) {
                mAppContext.getCurrentActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else if (sWakelockSemaphore == 0) {
                mAppContext.getCurrentActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        setWakeLock(false);
        mMediaPlayerValid = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setSrc(mSrcUriString, mSrcType, mSrcIsNetwork, mSrcIsAsset);
    }

    @Override
    public void onHostPause() {
        if (mMediaPlayer != null && !mPlayInBackground) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostDestroy() {
    }

    public void drop() {
        mStopped = true;
    }
}
