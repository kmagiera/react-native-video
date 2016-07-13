package com.brentvatne.react;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.uimanager.events.RCTEventEmitter;

public class ReactVideoEventDispatcher {

  public static final String EVENT_PROGRESS = "onVideoProgress";
  public static final String EVENT_LOAD_START = "onVideoLoadStart";
  public static final String EVENT_LOAD = "onVideoLoad";
  public static final String EVENT_ERROR = "onVideoError";
  public static final String EVENT_SEEK = "onVideoSeek";
  public static final String EVENT_END = "onVideoEnd";
  public static final String EVENT_STALLED = "onPlaybackStalled";
  public static final String EVENT_RESUME = "onPlaybackResume";
  public static final String EVENT_READY_FOR_DISPLAY = "onReadyForDisplay";

  public static final String[] DIRECT_EVENTS = new String[] {
          EVENT_PROGRESS,
          EVENT_LOAD_START,
          EVENT_LOAD,
          EVENT_ERROR,
          EVENT_SEEK,
          EVENT_END,
          EVENT_STALLED,
          EVENT_RESUME,
          EVENT_READY_FOR_DISPLAY,
  };

  private static final String EVENT_PROP_FAST_FORWARD = "canPlayFastForward";
  private static final String EVENT_PROP_SLOW_FORWARD = "canPlaySlowForward";
  private static final String EVENT_PROP_SLOW_REVERSE = "canPlaySlowReverse";
  private static final String EVENT_PROP_REVERSE = "canPlayReverse";
  private static final String EVENT_PROP_STEP_FORWARD = "canStepForward";
  private static final String EVENT_PROP_STEP_BACKWARD = "canStepBackward";

  private static final String EVENT_PROP_DURATION = "duration";
  private static final String EVENT_PROP_PLAYABLE_DURATION = "playableDuration";
  private static final String EVENT_PROP_CURRENT_TIME = "currentTime";
  private static final String EVENT_PROP_SEEK_TIME = "seekTime";
  private static final String EVENT_PROP_NATURALSIZE = "naturalSize";
  private static final String EVENT_PROP_WIDTH = "width";
  private static final String EVENT_PROP_HEIGHT = "height";
  private static final String EVENT_PROP_ORIENTATION = "orientation";

  private static final String EVENT_PROP_ERROR = "error";
  private static final String EVENT_PROP_WHAT = "what";
  private static final String EVENT_PROP_EXTRA = "extra";

  private static class ProgressEvent extends Event<ProgressEvent> {

    private final float mCurrentTime;
    private final float mPlayableDuration;

    public ProgressEvent(int viewId, long timestampMs, float currentTime, float playableDuration) {
      super(viewId, timestampMs);
      mCurrentTime = currentTime;
      mPlayableDuration = playableDuration;
    }

    @Override
    public String getEventName() {
      return EVENT_PROGRESS;
    }

    @Override
    public short getCoalescingKey() {
      // All progress events events for a given view can be coalesced.
      return 0;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
      rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
    }

    private WritableMap serializeEventData() {
      WritableMap eventData = Arguments.createMap();
      eventData.putDouble(EVENT_PROP_CURRENT_TIME, mCurrentTime);
      eventData.putDouble(EVENT_PROP_PLAYABLE_DURATION, mPlayableDuration);
      return eventData;
    }
  }

  private static class StateEvent extends Event<StateEvent> {

    private final String mEventName;

    public StateEvent(int viewId, long timestampMs, String eventName) {
      super(viewId, timestampMs);
      mEventName = eventName;
    }

    @Override
    public String getEventName() {
      return mEventName;
    }

    @Override
    public boolean canCoalesce() {
      return false;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
      rctEventEmitter.receiveEvent(getViewTag(), getEventName(), null);
    }
  }

  private static class LoadStartEvent extends Event<LoadStartEvent> {

    private final String mUri;
    private final String mSrcType;
    private final boolean mIsNetwork;

    public LoadStartEvent(int viewId, long timestampMs, String uri, String srcType, boolean isNetwork) {
      super(viewId, timestampMs);
      mUri = uri;
      mSrcType = srcType;
      mIsNetwork = isNetwork;
    }

    @Override
    public String getEventName() {
      return EVENT_LOAD_START;
    }

    @Override
    public boolean canCoalesce() {
      return false;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
      WritableMap src = Arguments.createMap();
      src.putString(ReactVideoViewManager.PROP_SRC_URI, mUri);
      src.putString(ReactVideoViewManager.PROP_SRC_TYPE, mSrcType);
      src.putBoolean(ReactVideoViewManager.PROP_SRC_IS_NETWORK, mIsNetwork);
      WritableMap eventData = Arguments.createMap();
      eventData.putMap(ReactVideoViewManager.PROP_SRC, src);
      rctEventEmitter.receiveEvent(getViewTag(), getEventName(), eventData);
    }
  }

  private static class LoadEvent extends Event<LoadEvent> {

    private final float mCurrentTime;
    private final float mDuration;
    private final int mWidth;
    private final int mHeight;

    public LoadEvent(int viewId, long timestampMs, float duration, float currentTime, int width, int height) {
      super(viewId, timestampMs);
      mDuration = duration;
      mCurrentTime = currentTime;
      mWidth = width;
      mHeight = height;
    }

    @Override
    public String getEventName() {
      return EVENT_LOAD;
    }

    @Override
    public short getCoalescingKey() {
      // All load events events for a given view can be coalesced.
      return 0;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
      WritableMap naturalSize = Arguments.createMap();
      naturalSize.putInt(EVENT_PROP_WIDTH, mWidth);
      naturalSize.putInt(EVENT_PROP_HEIGHT, mHeight);
      if (mWidth > mHeight)
        naturalSize.putString(EVENT_PROP_ORIENTATION, "landscape");
      else
        naturalSize.putString(EVENT_PROP_ORIENTATION, "portrait");

      WritableMap eventData = Arguments.createMap();
      eventData.putDouble(EVENT_PROP_DURATION, mDuration);
      eventData.putDouble(EVENT_PROP_CURRENT_TIME, mCurrentTime);
      eventData.putMap(EVENT_PROP_NATURALSIZE, naturalSize);
      // TODO: Actually check if you can.
      eventData.putBoolean(EVENT_PROP_FAST_FORWARD, true);
      eventData.putBoolean(EVENT_PROP_SLOW_FORWARD, true);
      eventData.putBoolean(EVENT_PROP_SLOW_REVERSE, true);
      eventData.putBoolean(EVENT_PROP_REVERSE, true);
      eventData.putBoolean(EVENT_PROP_FAST_FORWARD, true);
      eventData.putBoolean(EVENT_PROP_STEP_BACKWARD, true);
      eventData.putBoolean(EVENT_PROP_STEP_FORWARD, true);
      rctEventEmitter.receiveEvent(getViewTag(), getEventName(), eventData);
    }
  }

  private static class ErrorEvent extends Event<ErrorEvent> {

    private final int mWhat;
    private final int mExtra;

    public ErrorEvent(int viewId, long timestampMs, int what, int extra) {
      super(viewId, timestampMs);
      mWhat = what;
      mExtra = extra;
    }

    @Override
    public String getEventName() {
      return EVENT_ERROR;
    }

    @Override
    public boolean canCoalesce() {
      return false;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
      WritableMap eventData = Arguments.createMap();
      eventData.putInt(EVENT_PROP_WHAT, mWhat);
      eventData.putInt(EVENT_PROP_EXTRA, mExtra);
      WritableMap event = Arguments.createMap();
      event.putMap(EVENT_PROP_ERROR, eventData);
      rctEventEmitter.receiveEvent(getViewTag(), getEventName(), eventData);
    }
  }

  private static class SeekEvent extends Event<SeekEvent> {

    private final float mCurrentTime;
    private final float mSeekTime;

    public SeekEvent(int viewId, long timestampMs, float currentTime, float seekTime) {
      super(viewId, timestampMs);
      mCurrentTime = currentTime;
      mSeekTime = seekTime;
    }

    @Override
    public String getEventName() {
      return EVENT_SEEK;
    }

    @Override
    public short getCoalescingKey() {
      // All seek events events for a given view can be coalesced.
      return 0;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
      WritableMap eventData = Arguments.createMap();
      eventData.putDouble(EVENT_PROP_CURRENT_TIME, mCurrentTime);
      eventData.putDouble(EVENT_PROP_SEEK_TIME, mSeekTime);
      rctEventEmitter.receiveEvent(getViewTag(), getEventName(), eventData);
    }
  }

  private final ReactVideoView mView;
  private final EventDispatcher mEventDispatcher;

  public ReactVideoEventDispatcher(ReactVideoView view, ReactContext context) {
    mView = view;
    mEventDispatcher = context.getNativeModule(UIManagerModule.class).getEventDispatcher();
  }

  public void dispatchProgressEvent(float currentPosition, float playableDuration) {
    mEventDispatcher.dispatchEvent(
            new ProgressEvent(mView.getId(), SystemClock.nanoTime(), currentPosition, playableDuration));
  }

  public void dispatchEndEvent() {
    mEventDispatcher.dispatchEvent(
            new StateEvent(mView.getId(), SystemClock.nanoTime(), EVENT_END));
  }

  public void dispatchStalledEvent() {
    mEventDispatcher.dispatchEvent(
            new StateEvent(mView.getId(), SystemClock.nanoTime(), EVENT_STALLED));
  }

  public void dispatchResumeEvent() {
    mEventDispatcher.dispatchEvent(
            new StateEvent(mView.getId(), SystemClock.nanoTime(), EVENT_RESUME));
  }

  public void dispatchReadyForDisplayEvent() {
    mEventDispatcher.dispatchEvent(
            new StateEvent(mView.getId(), SystemClock.nanoTime(), EVENT_READY_FOR_DISPLAY));
  }

  public void dispatchLoadStartEvent(String uri, String srcType, boolean isNetwork) {
    mEventDispatcher.dispatchEvent(
            new LoadStartEvent(mView.getId(), SystemClock.nanoTime(), uri, srcType, isNetwork));
  }

  public void dispatchLoadEvent(float duration, float currentTime, int width, int height) {
    mEventDispatcher.dispatchEvent(
            new LoadEvent(mView.getId(), SystemClock.nanoTime(), duration, currentTime, width, height));
  }

  public void dispatchErrorEvent(int what, int extra) {
    mEventDispatcher.dispatchEvent(
            new ErrorEvent(mView.getId(), SystemClock.nanoTime(), what, extra));
  }

  public void dispatchSeekEvent(float currentTime, float seekTime) {
    mEventDispatcher.dispatchEvent(
            new SeekEvent(mView.getId(), SystemClock.nanoTime(), currentTime, seekTime));
  }
}
