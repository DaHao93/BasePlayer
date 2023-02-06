package com.bighao.player.listener;

/**
 * Created by Hao on 2023/2/4
 * 播放进度监听
 */
public interface OnPlayerProgressListener {
    /**
     * @param progress 当前播放进度
     * @param duration 总时长
     */
    void onProgress(long progress, long duration);

    /**
     * 缓冲进度
     *
     * @param bufferProgress 缓冲进度
     */
    void onBufferingUpdate(long bufferProgress);
}
