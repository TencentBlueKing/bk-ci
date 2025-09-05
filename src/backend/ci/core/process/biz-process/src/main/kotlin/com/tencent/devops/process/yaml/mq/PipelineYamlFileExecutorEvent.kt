package com.tencent.devops.process.yaml.mq

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.repository.pojo.Repository
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "yaml文件执行事件")
@Event(StreamBinding.PIPELINE_YAML_FILE_EXECUTOR)
data class PipelineYamlFileExecutorEvent(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "代码库", required = true)
    val repository: Repository,
    @get:Schema(title = "事件ID", required = true)
    val eventId: Long,
    @get:Schema(title = "文件路径", required = true)
    val filePath: String,
    @get:Schema(title = "变更分支", required = true)
    val ref: String
) : IEvent()
