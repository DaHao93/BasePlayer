package com.bighao.player.view;

import com.bighao.player.dict.PlayerStateEnum;

/**
 * Created by Hao on 2023/2/4
 */
public interface IPlayerInterface {

    void onPrepared();

    void onCompletion();

    void onPlayerStateChange(PlayerStateEnum stateEnum);

    void onVideoSizeChanged(int width, int height);

    void onInfo(int what, int extra);

    void onError(int what, int extra);

    void onBufferingUpdate(int bufferProgress);

}
