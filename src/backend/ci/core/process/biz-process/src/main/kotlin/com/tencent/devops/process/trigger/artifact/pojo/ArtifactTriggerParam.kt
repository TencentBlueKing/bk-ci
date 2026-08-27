package com.tencent.devops.process.trigger.artifact.pojo

import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactMetadataFilter
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType

/**
 * 制品触发的用户配置参数（变量替换后）。
 *
 * 来源于用户在插件里填写的配置，匹配阶段与触发时抽取的实际事实 [ArtifactFactParam] 做比对。
 * 本类的字段均为变量替换后的最终值，且多值的 glob 表达式已切成列表，条件层直接使用。
 */
data class ArtifactTriggerParam(
    val repository: ArtifactRepositoryType,
    val watchPipeline: List<String>,
    val watchRootPath: List<String>,
    val kind: ArtifactKind?,
    val artifactsName: List<String>,
    val artifactsNameIgnore: List<String>,
    val paths: List<String>,
    val pathsIgnore: List<String>,
    val image: String?,
    val tags: List<String>,
    val tagsIgnore: List<String>,
    val metadata: List<ArtifactMetadataFilter>?
)
