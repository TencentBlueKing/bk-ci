package com.tencent.devops.process.service

import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildExtService
import com.tencent.devops.process.utils.PIPELINE_TURBO_TASK_ID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBuildExtTencentService @Autowired constructor(
    private val pipelineContextService: PipelineContextService
) : PipelineBuildExtService {

    override fun buildExt(task: PipelineBuildTask, variables: Map<String, String>): Map<String, String> {
        val taskType = task.taskType
        val extMap = pipelineContextService.buildContext(
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            stageId = task.stageId,
            containerId = task.containerId,
            taskId = task.taskId,
            variables = variables,
            executeCount = task.executeCount
        )

        // 仅仅只是为了兼容长尾业务流水线脚本里仍然有用到这 2 个变量，实际没有任何用途,这个是早期版本无脑硬塞的后果。
        // 现在仍有很多长尾业务流水线继续在用export xxx=${turbo.task.id} ，如果没有变量，脚本会报错 xxx=${turbo.task.id}: 坏的替换
        // 难推动长尾业务去修改成熟的流水线，所以只能继续兼容，后续如果有机会再删除这 2 个变量
        if (taskType.contains("linuxPaasCodeCCScript") || taskType.contains("linuxScript")) {
            extMap.putIfAbsent(PIPELINE_TURBO_TASK_ID, "")
            extMap.putIfAbsent("turbo.task.id", "")
        }

        return extMap
    }

    override fun endBuild(task: PipelineBuildTask) = Unit
}
