package com.tencent.devops.process.engine.utils

object PauseRedisUtils {
    val BS_PAUSE_TASK_REDIS_KEY = "_bkTaskPauseTag_"
    fun getPauseRedisKey(buildId: String, taskId: String): String {
        return "$BS_PAUSE_TASK_REDIS_KEY:$buildId:$taskId"
    }
}
