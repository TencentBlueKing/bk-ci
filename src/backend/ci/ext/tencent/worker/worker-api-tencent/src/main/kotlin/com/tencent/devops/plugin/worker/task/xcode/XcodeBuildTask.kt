package com.tencent.devops.plugin.worker.task.xcode

import com.tencent.devops.common.pipeline.element.XcodeBuildElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import java.io.File

@TaskClassType(classTypes = [XcodeBuildElement.classType])
class XcodeBuildTask : ITask() {
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val argument = Validator.validate(taskParams, workspace, buildVariables)
        Builder(argument).build(buildVariables, workspace)
    }
}