package com.tencent.devops.process.enums

enum class BuildReplayStatus {
    /**
     * 无法回放
     */
    CANNOT_REPLAY,
    /**
     * 可回放
     */
    CAN_REPLAY,

    /**
     * 回放成功
     */
    REPLAY_SUCCESS,

    /**
     * 回放中
     * webhook 回放构建是异步触发
     */
    REPLAYING,

    /**
     * 回放失败
     */
    REPLAY_FAILED
}