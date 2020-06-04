package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext

interface IScheduleTask {

    fun executeTask(quartzJobContext: QuartzJobContext)
}