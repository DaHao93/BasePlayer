package com.bighao.player;

import android.view.TextureView;

import com.bighao.player.listener.OnPlayerProgressListener;
import com.bighao.player.listener.OnPlayerStatusListener;

/**
 * Created by Hao on 2023/2/4
 */
public interface IPlayer {

    void setVideoUrl(String videoUrl);

    void setVideoUrl(String videoUrl, long time);

    /**
     * 开始播放视频
     */
    void startVideo();

    /**
     * 暂停
     */
    void onPause();

    /**
     * 继续播放
     */
    void onContinue();

    /**
     * 重新加载
     */
    void onReload();

    /**
     * 重播
     */
    void onPlayAgain();

    /**
     * 释放
     */
    void release();

    /**
     * 设置播放进度
     */
    void seekTo(long time);

    /**
     * 设置播放倍速
     */
    void setSpeed(float speed);

    /**
     * 设置声音
     */
    void setVolume(float leftVolume, float rightVolume);

    void setVideoSize(int width, int height);

    TextureView getTextureView();

    String getVideoUrl();

    /**
     * 播放进度监听
     */
    void setOnPlayerProgressListener(OnPlayerProgressListener onPlayerProgressListener);

    /**
     * 播放状态监听
     */
    void setOnPlayerStatusListener(OnPlayerStatusListener onPlayerStatusListener);


    /**
     * 获取当前播放时长
     *
     * @return 毫秒
     */
    long getCurrentPosition();

    /**
     * 获取视频总时长
     *
     * @return 毫秒
     */
    long getTotalTimeDuration();

    void startProgressTimer();

    void cancelProgressTimer();
}
