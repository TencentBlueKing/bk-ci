package com.tencent.devops.process.trigger.artifact.condition

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactMetadataFilter
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactMetadataOperator
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.trigger.artifact.pojo.ArtifactConditionContext

/**
 * 元数据条件：按 key/operator/value 比对制品元数据。
 *
 * 运算符见 [ArtifactMetadataOperator]。
 * 不同键之间为「且」，同一键的多个条件之间为「或」。
 * 键不存在时：NE 命中，EQ、CONTAINS 不命中；EXISTS 不命中，NOT_EXISTS 命中。
 * 配置为空时默认通过（不限制）。
 */
class MetadataCondition : ArtifactCondition {
    override fun match(context: ArtifactConditionContext): Boolean {
        val filters = context.triggerParam.metadata ?: return true
        if (filters.isEmpty()) return true
        val metadata = context.factParam.metadata
        val matched = filters.groupBy { it.key }.all { (_, keyFilters) ->
            keyFilters.any { matchFilter(it, metadata) }
        }
        if (!matched) {
            context.response.failedReason = I18Variable(
                code = WebhookI18nConstants.BK_ARTIFACT_METADATA_NOT_MATCH,
                params = listOf()
            ).toJsonStr()
        }
        return matched
    }

    private fun matchFilter(filter: ArtifactMetadataFilter, metadata: Map<String, Any?>): Boolean {
        val hasKey = metadata.containsKey(filter.key)
        val actual = metadata[filter.key]?.toString()
        return when (filter.operator) {
            ArtifactMetadataOperator.EQ -> actual == filter.value
            ArtifactMetadataOperator.NE -> actual != filter.value
            ArtifactMetadataOperator.CONTAINS -> actual?.contains(filter.value) == true
            ArtifactMetadataOperator.EXISTS -> hasKey
            ArtifactMetadataOperator.NOT_EXISTS -> !hasKey
        }
    }
}
