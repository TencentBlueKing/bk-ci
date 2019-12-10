package com.tencent.devops.plugin.worker.task.unity3d

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.task.ITask
import java.io.File

class Unity3dBuildTask : ITask() {
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        var version = ""
        buildVariables.buildEnvs.forEach { if (it.name == "unity") version = it.version }
        if (version.isEmpty()) throw TaskExecuteException(
            errorMsg = "unity version is empty",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )

        val platFormList = try {
            JsonUtil.to<List<String>>(taskParams["platform"]!!)
        } catch (t: Throwable) {
            taskParams["platform"]!!.split(",")
        }
        platFormList.forEach { platform ->
            val argument = Validator.getArgument(buildVariables.buildId, taskParams, workspace, platform.trim())
            argument.version = version
            Builder(argument).run(workspace, buildVariables)
        }
    }
}
