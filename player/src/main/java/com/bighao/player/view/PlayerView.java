package com.bighao.player.view;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bighao.player.IPlayer;
import com.bighao.player.R;
import com.bighao.player.dict.PlayerStateEnum;
import com.bighao.player.iface.ExoMediaInterface;
import com.bighao.player.iface.MediaInterface;
import com.bighao.player.listener.OnPlayerProgressListener;
import com.bighao.player.listener.OnPlayerStatusListener;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Hao on 2023/2/4
 */
public class PlayerView extends FrameLayout implements IPlayer, IPlayerInterface {

    private String mVideoUrl;
    private PlayerTextureView mTextureView;

    //宽高比
    public int mWidthRatio = 0;
    public int mHeightRatio = 0;

    private FrameLayout mTextureViewContainer;

    private MediaInterface mMediaInterface;

    private PlayerStateEnum mStateType;//播放器状态

    private long mCurrentPosition;//当前进度
    private long mTotalTimeDuration;//总视频进度
    private long mCachePosition;//缓存当前播放进度，切换清晰度时候使用

    private ProgressTimerTask mProgressTimerTask;
    private Timer mUpdateProgressTimer;

    private OnPlayerStatusListener mStatusListener;//播放状态回调
    private OnPlayerProgressListener mProgressListener;//播放进度回调

    public PlayerView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public PlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        View contentView = LayoutInflater.from(context).inflate(R.layout.layout_player_view, this);

        mTextureViewContainer = contentView.findViewById(R.id.player_surface_container);

        mStateType = PlayerStateEnum.STATE_IDLE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mWidthRatio != 0 && mHeightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) mHeightRatio) / mWidthRatio);
            setMeasuredDimension(specWidth, specHeight);
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void setVideoUrl(String videoUrl) {
        setVideoUrl(videoUrl, 0);
    }

    @Override
    public void setVideoUrl(String videoUrl, long time) {
        this.mVideoUrl = videoUrl;
        this.mCachePosition = time;

        onPlayerStateChange(PlayerStateEnum.STATE_NORMAL);

        this.mMediaInterface = new ExoMediaInterface(this);
    }

    @Override
    public void startVideo() {
        addTextureView();
        if (getContext() instanceof Activity) {
            //保持屏幕常亮
            Activity activity = (Activity) getContext();
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        onPlayerStateChange(PlayerStateEnum.STATE_PREPARING);
    }

    @Override
    public void onPause() {
        if (mStateType == PlayerStateEnum.STATE_PLAYING) {
            if (null != mMediaInterface) {
                mMediaInterface.pause();
                onPlayerStateChange(PlayerStateEnum.STATE_PAUSE);
            }
        }
    }

    @Override
    public void onContinue() {
        if (mStateType == PlayerStateEnum.STATE_PAUSE) {
            if (null != mMediaInterface) {
                mMediaInterface.start();
                onPlayerStateChange(PlayerStateEnum.STATE_PLAYING);
            }
        }
    }

    @Override
    public void onReload() {
        mCachePosition = mCurrentPosition;
        onPlayerStateChange(PlayerStateEnum.STATE_NORMAL);
        mTextureViewContainer.removeAllViews();
        startVideo();
    }

    @Override
    public void onPlayAgain() {
        onPlayerStateChange(PlayerStateEnum.STATE_NORMAL);
        mTextureViewContainer.removeAllViews();
        startVideo();
    }

    @Override
    public void release() {
        onPlayerStateChange(PlayerStateEnum.STATE_NORMAL);

        mTextureViewContainer.removeAllViews();
        if (getContext() instanceof Activity) {
            //取消屏幕常亮
            Activity activity = (Activity) getContext();
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        cancelProgressTimer();
    }

    @Override
    public void seekTo(long time) {
        if (mStateType == PlayerStateEnum.STATE_PLAYING || mStateType == PlayerStateEnum.STATE_PAUSE) {
            if (null != mMediaInterface) {
                mMediaInterface.seekTo(time);
            }
        }
    }

    @Override
    public void setSpeed(float speed) {
        if (null != mMediaInterface) {
            mMediaInterface.setSpeed(speed);
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (null != mMediaInterface) {
            mMediaInterface.setVolume(leftVolume, rightVolume);
        }
    }

    @Override
    public void setVideoSize(int width, int height) {
        if (null != mTextureView) {
            mTextureView.setVideoSize(width, height);
        }
    }

    @Override
    public TextureView getTextureView() {
        return mTextureView;
    }

    @Override
    public String getVideoUrl() {
        return mVideoUrl;
    }

    @Override
    public void setOnPlayerProgressListener(OnPlayerProgressListener onPlayerProgressListener) {
        this.mProgressListener = onPlayerProgressListener;
    }

    @Override
    public void setOnPlayerStatusListener(OnPlayerStatusListener onPlayerStatusListener) {
        this.mStatusListener = onPlayerStatusListener;
    }

    @Override
    public long getCurrentPosition() {
        return mCurrentPosition;
    }

    @Override
    public long getTotalTimeDuration() {
        return mTotalTimeDuration;
    }

    private void addTextureView() {
        if (null != mTextureView) {
            mTextureViewContainer.removeView(mTextureView);
        }
        mTextureView = new PlayerTextureView(getContext().getApplicationContext());
        mTextureView.setSurfaceTextureListener(mMediaInterface);
        LayoutParams layoutParams =
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        mTextureViewContainer.addView(mTextureView, layoutParams);
    }

    @Override
    public void onPrepared() {
        if (null != mMediaInterface) {
            mMediaInterface.start();
        }
    }

    @Override
    public void onCompletion() {
        onPlayerStateChange(PlayerStateEnum.STATE_COMPLETE);
    }

    @Override
    public void onPlayerStateChange(PlayerStateEnum stateEnum) {
        if (stateEnum == PlayerStateEnum.STATE_NORMAL) {
            if (null != mMediaInterface) {
                mMediaInterface.release();
            }
        } else if (stateEnum == PlayerStateEnum.STATE_PREPARING) {
            mCurrentPosition = 0;
        } else if (stateEnum == PlayerStateEnum.STATE_LOADING) {
            //如果暂停时候切换进度，这里自动播放
            if (mStateType == PlayerStateEnum.STATE_PAUSE) {
                if (null != mMediaInterface) {
                    mMediaInterface.start();
                }
            }
        } else if (stateEnum == PlayerStateEnum.STATE_PLAYING) {
            //切换清晰度的时候跳转进度
            if (mCachePosition != 0 && null != mMediaInterface) {
                mMediaInterface.seekTo(mCachePosition);
                mCachePosition = 0;
            }
            startProgressTimer();
        } else if (stateEnum == PlayerStateEnum.STATE_PAUSE) {
            startProgressTimer();
        } else if (stateEnum == PlayerStateEnum.STATE_COMPLETE) {
            cancelProgressTimer();
        } else if (stateEnum == PlayerStateEnum.STATE_ERROR) {
            cancelProgressTimer();
        }
        this.mStateType = stateEnum;
        if (null != mStatusListener) {
            mStatusListener.onPlayerStatus(mStateType);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        if (null != mTextureView) {
            mTextureView.setVideoSize(width, height);
        }
    }

    @Override
    public void onInfo(int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            if (mStateType == PlayerStateEnum.STATE_PREPARING) {
                //开始渲染图像，真正进入playing状态
                onPlayerStateChange(PlayerStateEnum.STATE_PLAYING);
            }
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            onPlayerStateChange(PlayerStateEnum.STATE_LOADING);
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            onPlayerStateChange(PlayerStateEnum.STATE_PLAYING);
        }
    }

    @Override
    public void onError(int what, int extra) {
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            onPlayerStateChange(PlayerStateEnum.STATE_ERROR);
            mMediaInterface.release();
        }
    }

    @Override
    public void onBufferingUpdate(int bufferProgress) {
        if (0 != bufferProgress) {
            float percent = bufferProgress / 100f;
            if (null != mProgressListener) {
                mProgressListener.onBufferingUpdate((long) (getDuration() * percent));
            }
        }
    }

    @Override
    public void startProgressTimer() {
        cancelProgressTimer();
        mUpdateProgressTimer = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        mUpdateProgressTimer.schedule(mProgressTimerTask, 0, 300);
    }

    @Override
    public void cancelProgressTimer() {
        if (null != mUpdateProgressTimer) {
            mUpdateProgressTimer.cancel();
        }
        if (null != mProgressTimerTask) {
            mProgressTimerTask.cancel();
        }
    }

    private class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (mStateType == PlayerStateEnum.STATE_PLAYING || mStateType == PlayerStateEnum.STATE_PAUSE) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentPosition = getCurrentPositionWhenPlaying();
                        mTotalTimeDuration = getDuration();
                        if (null != mProgressListener) {
                            mProgressListener.onProgress(getCurrentPositionWhenPlaying(), getDuration());
                        }
                    }
                });
            }
        }
    }

    private long getCurrentPositionWhenPlaying() {
        long position = 0;
        if (mStateType == PlayerStateEnum.STATE_PLAYING || mStateType == PlayerStateEnum.STATE_PAUSE || mStateType == PlayerStateEnum.STATE_PREPARING) {
            try {
                position = mMediaInterface.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    private long getDuration() {
        long duration = 0;
        try {
            duration = mMediaInterface.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }
}
