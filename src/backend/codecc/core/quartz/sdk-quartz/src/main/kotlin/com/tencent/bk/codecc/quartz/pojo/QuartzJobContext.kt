package com.tencent.bk.codecc.quartz.pojo

import java.util.*

data class QuartzJobContext(
        var jobName: String?,
        val beanName: String,
        var shardNum: Int?,
        var nodeNum: Int?,
        var scheduledFireTime: Date?,
        var jobCustomParam: Map<String, Any>?

)