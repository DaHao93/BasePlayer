package com.bighao.player.dict;

/**
 * Created by Hao on 2023/2/4
 */
public enum PlayerStateEnum {

    STATE_IDLE(-1, "空闲"),
    STATE_NORMAL(0, "正常"),
    STATE_PREPARING(1, "准备中"),
    STATE_LOADING(2, "加载中"),
    STATE_PLAYING(3, "播放中"),
    STATE_PAUSE(4, "暂停"),
    STATE_COMPLETE(5, "播放完成"),
    STATE_ERROR(6, "错误");

    private int value;
    private String table;

    PlayerStateEnum(int value, String table) {
        this.value = value;
        this.table = table;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}
