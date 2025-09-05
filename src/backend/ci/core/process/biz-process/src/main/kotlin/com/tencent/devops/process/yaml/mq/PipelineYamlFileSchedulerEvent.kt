package com.tencent.devops.process.yaml.mq

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.repository.pojo.Repository
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "yaml文件调度事件")
@Event(StreamBinding.PIPELINE_YAML_FILE_SCHEDULER)
data class PipelineYamlFileSchedulerEvent(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "代码库", required = true)
    val repository: Repository,
    @get:Schema(title = "事件ID", required = true)
    val eventId: Long,
    @get:Schema(title = "已执行的文件路径", required = true)
    val filePath: String? = null,
    @get:Schema(title = "已执行的文件类型", required = false)
    val fileType: YamlFileType? = null
) : IEvent()
