package com.tencent.devops.process.notify.command

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.command.CmdContext
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting

data class BuildNotifyContext(
    override var cmdFlowSeq: Int = 0, // 命令序号
    override val variables: Map<String, String>, // 变量
    override val watcher: Watcher, // 监控对象
    override val executeCount: Int = 1, // 执行次数
    val pipelineSetting: PipelineSetting,
    val notifyValue: MutableMap<String, String>,
    val buildStatus: BuildStatus,
    val projectId: String,
    val pipelineId: String,
    val buildId: String
) : CmdContext(
    executeCount = executeCount,
    variables = variables,
    cmdFlowSeq = cmdFlowSeq,
    watcher = watcher
)
