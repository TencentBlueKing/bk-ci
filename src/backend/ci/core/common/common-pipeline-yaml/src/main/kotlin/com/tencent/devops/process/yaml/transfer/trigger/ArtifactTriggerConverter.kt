package com.tencent.devops.process.yaml.transfer.trigger

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactMetadataFilter
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerInput
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactMetadataOperator
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactTriggerEventType
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.v3.models.TriggerType
import com.tencent.devops.process.yaml.v3.models.on.ArrivedMetadata
import com.tencent.devops.process.yaml.v3.models.on.ArrivedRule
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import org.springframework.stereotype.Component

/**
 * 制品到达触发器转换器（统一框架的样板实现）。
 *
 * YAML 形态（统一「触发器 -> 事件类型」结构）：
 * - 单触发器：`on.artifact.arrived`
 * - 多触发器：`on[].type = artifact` + `arrived`
 */
@Component
class ArtifactTriggerConverter : TriggerConverter {

    companion object {
        // 制品到达事件类型；新增其它事件类型只需在本转换器内扩展，无需改动 TriggerOn/PreTriggerOnV3
        private const val EVENT_ARRIVED = "arrived"
    }

    override val triggerType: TriggerType = TriggerType.ARTIFACT

    override fun support(element: Element): Boolean = element is ArtifactTriggerElement

    override fun yaml2Elements(triggerOn: TriggerOn, elements: MutableList<Element>) {
        val arrivedRaw = triggerOn.events?.get(EVENT_ARRIVED) ?: return
        val rule = JsonUtil.anyTo(arrivedRaw, object : TypeReference<ArrivedRule>() {})
        elements.add(buildArtifactElement(rule))
    }

    override fun elements2Yaml(
        elements: List<Element>,
        aspectWrapper: PipelineTransferAspectWrapper
    ): List<TriggerOn> {
        return elements.filterIsInstance<ArtifactTriggerElement>().map { element ->
            aspectWrapper.setModelElement4Model(element, PipelineTransferAspectWrapper.AspectType.BEFORE)
            val input = element.data.input
            val arrived = ArrivedRule(
                id = element.stepId,
                name = element.name,
                enable = element.elementEnabled().nullIfDefault(true),
                repository = input.repository.value,
                watchPipeline = input.watchPipeline.nonEmptyOrNull(),
                watchRootPath = input.watchRootPath?.ifBlank { null },
                kind = input.kind?.value.nullIfDefault("file"),
                artifactsName = input.artifactsName?.takeIf { it.isNotBlank() }?.split(","),
                artifactsNameIgnore = input.artifactsNameIgnore?.takeIf { it.isNotBlank() }?.split(","),
                paths = input.paths?.takeIf { it.isNotBlank() }?.split(","),
                pathsIgnore = input.pathsIgnore?.takeIf { it.isNotBlank() }?.split(","),
                image = input.image?.ifBlank { null },
                tags = input.tags?.takeIf { it.isNotBlank() }?.split(","),
                tagsIgnore = input.tagsIgnore?.takeIf { it.isNotBlank() }?.split(","),
                metadata = input.metadata.nonEmptyOrNull()?.map {
                    ArrivedMetadata(
                        key = it.key,
                        operator = it.operator.value,
                        value = it.value
                    )
                }
            )
            TriggerOn(events = mapOf(EVENT_ARRIVED to arrived))
        }
    }

    private fun buildArtifactElement(rule: ArrivedRule): ArtifactTriggerElement {
        return ArtifactTriggerElement(
            name = rule.name ?: "制品到达触发",
            stepId = rule.id,
            data = ArtifactTriggerData(
                input = ArtifactTriggerInput(
                    repository = rule.repository?.let { ArtifactRepositoryType.valueOf(it.uppercase()) }
                        ?: ArtifactRepositoryType.PIPELINE,
                    watchPipeline = rule.watchPipeline.nonEmptyOrNull(),
                    watchRootPath = rule.watchRootPath,
                    kind = ArtifactKind.parse(rule.kind),
                    eventType = ArtifactTriggerEventType.ARRIVED,
                    artifactsName = rule.artifactsName.nonEmptyOrNull()?.join(),
                    artifactsNameIgnore = rule.artifactsNameIgnore.nonEmptyOrNull()?.join(),
                    paths = rule.paths.nonEmptyOrNull()?.join(),
                    pathsIgnore = rule.pathsIgnore.nonEmptyOrNull()?.join(),
                    image = rule.image,
                    tags = rule.tags.nonEmptyOrNull()?.join(),
                    tagsIgnore = rule.tagsIgnore.nonEmptyOrNull()?.join(),
                    metadata = rule.metadata?.map {
                        ArtifactMetadataFilter(
                            key = it.key,
                            operator = ArtifactMetadataOperator.parse(it.operator),
                            value = it.value
                        )
                    }
                )
            )
        ).checkTriggerElementEnable(rule.enable) as ArtifactTriggerElement
    }

    private fun Element.checkTriggerElementEnable(enabled: Boolean?): Element {
        if (additionalOptions == null) {
            additionalOptions = ElementAdditionalOptions(runCondition = RunCondition.PRE_TASK_SUCCESS)
        }
        additionalOptions!!.enable = enabled ?: true
        return this
    }

    private fun List<String>.join() = this.joinToString(separator = ",")

    private fun <T> List<T>?.nonEmptyOrNull() = this?.ifEmpty { null }
}
