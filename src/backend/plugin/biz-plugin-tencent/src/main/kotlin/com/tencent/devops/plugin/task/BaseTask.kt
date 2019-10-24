package com.tencent.devops.plugin.task

import com.tencent.devops.plugin.pojo.TaskData

interface BaseTask<T : TaskData> {
    fun taskDataClass(): Class<T>
    fun process(taskData: T)
}