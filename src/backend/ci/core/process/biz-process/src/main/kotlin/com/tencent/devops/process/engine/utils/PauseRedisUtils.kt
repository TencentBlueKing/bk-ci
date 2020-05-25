package com.tencent.devops.process.engine.utils

object PauseRedisUtils {
    val BS_PAUSE_TASK_REDIS_KEY = "_bkTaskPauseTag_"
    val BS_PAUSE_ELEMENT_REDIS_KEY = "_bkTaskPauseElement_"
    fun getPauseRedisKey(buildId: String, taskId: String): String {
        return "$BS_PAUSE_TASK_REDIS_KEY:$buildId:$taskId"
    }

    fun getPauseElementRedisKey(buildId: String, taskId: String): String {
        return "$BS_PAUSE_ELEMENT_REDIS_KEY:$buildId:$taskId"
    }
}