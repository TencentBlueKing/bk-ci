package com.tencent.devops.process.service.task.copy

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceRel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyTaskFactory @Autowired constructor(
    private val pipelineDependencyAnalyzeService: PipelineDependencyAnalyzeService
) {
    fun buildSubPipelineResourceRels(
        projectId: String,
        details: List<PipelineBatchTaskDetail>
    ): List<PipelineCopyTaskResourceRel> {
        val relations = mutableSetOf<PipelineCopyTaskResourceRel>()
        details.filterNot { it.subPipeline }.forEach { detail ->
            collectSubPipelineResourceRels(
                projectId = projectId,
                taskId = detail.taskId,
                parentPipelineId = detail.pipelineId,
                parentPipelineName = detail.pipelineName,
                visitedPipelineIds = mutableSetOf(detail.pipelineId),
                relations = relations
            )
        }
        return relations.toList()
    }

    private fun collectSubPipelineResourceRels(
        projectId: String,
        taskId: String,
        parentPipelineId: String,
        parentPipelineName: String,
        visitedPipelineIds: MutableSet<String>,
        relations: MutableSet<PipelineCopyTaskResourceRel>
    ) {
        pipelineDependencyAnalyzeService.analysisDirectSubPipelineDependency(
            projectId = projectId,
            pipelineId = parentPipelineId
        ).filter {
            // 跨项目子流水线不纳入当前项目复制任务
            it.projectId == projectId
        }.forEach { resource ->
            relations.add(
                PipelineCopyTaskResourceRel(
                    taskId = taskId,
                    projectId = projectId,
                    pipelineId = parentPipelineId,
                    pipelineName = parentPipelineName,
                    resourceType = PipelineDependentResourceType.PIPELINE,
                    resourceId = resource.resourceId,
                    resourceName = resource.resourceName
                )
            )
            if (visitedPipelineIds.add(resource.resourceId)) {
                collectSubPipelineResourceRels(
                    projectId = projectId,
                    taskId = taskId,
                    parentPipelineId = resource.resourceId,
                    parentPipelineName = resource.resourceName,
                    visitedPipelineIds = visitedPipelineIds,
                    relations = relations
                )
            }
        }
    }
}
