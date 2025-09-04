package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "文件分发的信息")
data class FileDistributeReq(
    @get:Schema(title = "源文件列表", required = true)
    val fileSourceList: List<FileSource>,
    @get:Schema(title = "文件传输目标路径", required = true)
    val fileTargetPath: String,
    @get:Schema(title = "传输模式", description = "1：严谨模式, 2：强制模式, 默认2")
    val transferMode: Int = 2,
    @get:Schema(title = "执行目标", required = true)
    val executeTarget: ExecuteTarget,
    @get:Schema(title = "机器执行帐号别名", description = "从账号页面获取，与accountId必须存在一个，同时存在时，accountId优先。")
    val accountAlias: String = "user00",
    @get:Schema(title = "机器执行帐号ID", description = "与accountAlias必须存在一个，同时存在时，accountId优先。")
    val accountId: Long?,
    @get:Schema(title = "文件分发超时时间", description = "单位：秒，默认7200秒，取值范围1-86400。")
    val timeout: Long = 7200,
    @get:Schema(title = "自定义作业名称")
    val taskName: String?
)