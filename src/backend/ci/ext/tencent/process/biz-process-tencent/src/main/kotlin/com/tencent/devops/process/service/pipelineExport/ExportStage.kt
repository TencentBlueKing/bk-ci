package com.tencent.devops.process.service.pipelineExport

import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v2.stageCheck.PreFlow
import com.tencent.devops.process.yaml.v2.stageCheck.PreStageCheck
import com.tencent.devops.process.yaml.v2.stageCheck.PreStageReviews
import com.tencent.devops.process.yaml.v2.stageCheck.ReviewVariable

object ExportStage {

    fun getV2StageFromModel(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext
    ): List<PreStage> {
        val stages = mutableListOf<PreStage>()
        allInfo.model.stages.drop(1).forEach { stage ->
            if (stage.finally) {
                return@forEach
            }
            val pipelineExportV2YamlConflictMapItem =
                PipelineExportV2YamlConflictMapItem(
                    stage = PipelineExportV2YamlConflictMapBaseItem(
                        id = stage.id,
                        name = stage.name
                    )
                )
            val jobs = ExportJob.getV2JobFromStage(
                allInfo = allInfo,
                context = context,
                stage = stage,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
            ) ?: return@forEach
            val tags = mutableListOf<String>()
            val stageTagsMap = allInfo.stageTags?.map {
                it.id to it.stageTagName
            }?.toMap() ?: emptyMap()
            stage.tag?.forEach {
                val tagName = stageTagsMap[it]
                if (!tagName.isNullOrBlank()) tags.add(tagName)
            }
            stages.add(
                PreStage(
                    name = stage.name,
                    label = tags.ifEmpty { null },
                    ifField = when (stage.stageControlOption?.runCondition) {
                        StageRunCondition.CUSTOM_CONDITION_MATCH -> stage.stageControlOption?.customCondition
                        StageRunCondition.CUSTOM_VARIABLE_MATCH -> {
                            val ifString =
                                ExportCondition.parseNameAndValueWithAnd(
                                    context = context,
                                    nameAndValueList = stage.stageControlOption?.customVariables,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                                )
                            if (stage.stageControlOption?.customVariables?.isEmpty() == true) null
                            else ifString
                        }
                        StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                            val ifString = ExportCondition.parseNameAndValueWithOr(
                                context = context,
                                nameAndValueList = stage.stageControlOption?.customVariables,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            )
                            if (stage.stageControlOption?.customVariables?.isEmpty() == true) null
                            else ifString
                        }
                        else -> null
                    },
                    fastKill = if (stage.fastKill == true) true else null,
                    jobs = jobs,
                    checkIn = getCheckInForStage(stage),
                    // TODO 暂时不支持准出和gates的导出
                    checkOut = null
                )
            )
        }
        return stages
    }

    fun getV2FinalFromStage(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        stage: Stage
    ): Map<String, PreJob>? {
        if (stage.finally) {
            val pipelineExportV2YamlConflictMapItem =
                PipelineExportV2YamlConflictMapItem(
                    stage = PipelineExportV2YamlConflictMapBaseItem(
                        id = stage.id,
                        name = stage.name
                    )
                )
            return ExportJob.getV2JobFromStage(
                allInfo = allInfo,
                context = context,
                stage = stage,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
            )
        }
        return null
    }

    private fun getCheckInForStage(stage: Stage): PreStageCheck? {
        val reviews = PreStageReviews(
            flows = stage.checkIn?.reviewGroups?.map { PreFlow(it.name, it.reviewers) },
            variables = stage.checkIn?.reviewParams?.associate {
                it.key to ReviewVariable(
                    label = it.chineseName ?: it.key,
                    type = when (it.valueType) {
                        ManualReviewParamType.TEXTAREA -> "TEXTAREA"
                        ManualReviewParamType.ENUM -> "SELECTOR"
                        ManualReviewParamType.MULTIPLE -> "SELECTOR-MULTIPLE"
                        ManualReviewParamType.BOOLEAN -> "BOOL"
                        else -> "INPUT"
                    },
                    default = it.value,
                    values = it.options?.map { mit -> mit.key },
                    description = it.desc
                )
            },
            description = stage.checkIn?.reviewDesc
        )
        if (reviews.flows.isNullOrEmpty()) {
            return null
        }
        return PreStageCheck(
            reviews = reviews,
            gates = null,
            timeoutHours = stage.checkIn?.timeout
        )
    }
}
