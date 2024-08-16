package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "作业实例基本信息")
data class JobInstance(
    @get:Schema(title = "作业实例名称", required = true)
    val name: String,
    @get:Schema(
        title = "作业状态码", description = "1.未执行; 2.正在执行; 3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; " +
        "7.等待用户; 8.手动结束; 9.状态异常; 10.步骤强制终止中; 11.步骤强制终止成功", required = true
    )
    val status: Int,
    @get:Schema(title = "作业创建时间", description = "Unix时间戳，单位毫秒", required = true)
    val createTime: Long,
    @get:Schema(title = "开始执行时间", description = "Unix时间戳，单位毫秒", required = true)
    val startTime: Long,
    @get:Schema(title = "执行结束时间", description = "Unix时间戳，单位毫秒", required = true)
    val endTime: Long,
    @get:Schema(title = "总耗时", description = "单位毫秒", required = true)
    val totalTime: Int,
    @get:Schema(title = "作业实例ID", required = true)
    val jobInstanceId: Long,
    @get:Schema(title = "业务ID", required = true)
    val bkBizId: Long,
    @get:Schema(title = "资源范围类型", description = "biz - 业务，biz_set - 业务集", required = true)
    var bkScopeType: String,
    @get:Schema(title = "资源范围ID", description = "与bk_scope_type对应, 表示业务ID或者业务集ID", required = true)
    var bkScopeId: String
)