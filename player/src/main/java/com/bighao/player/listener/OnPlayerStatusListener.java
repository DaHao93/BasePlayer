package com.bighao.player.listener;

import com.bighao.player.dict.PlayerStateEnum;

/**
 * Created by Hao on 2023/2/4
 * 播放器状态监听
 */
public interface OnPlayerStatusListener {
    void onPlayerStatus(PlayerStateEnum state);
}
