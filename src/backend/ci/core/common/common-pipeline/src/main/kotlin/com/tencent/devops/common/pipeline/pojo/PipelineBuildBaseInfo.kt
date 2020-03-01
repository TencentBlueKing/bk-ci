package com.tencent.devops.common.pipeline.pojo

/**
 * @Description
 * @Date 2020/2/27
 * @Version 1.0
 */
data class PipelineBuildBaseInfo(
    val projectCode: String,
    val pipelineId: String,
    val buildIdList: List<String>
)
