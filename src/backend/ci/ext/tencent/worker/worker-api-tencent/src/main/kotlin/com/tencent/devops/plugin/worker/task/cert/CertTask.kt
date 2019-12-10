package com.tencent.devops.plugin.worker.task.cert

import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.api.dispatch.VMResourceApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.task.ITask
import java.io.File

@TaskClassType(classTypes = [IosCertInstallElement.classType])
class CertTask : ITask() {

    override fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {
        val osName = System.getProperty("os.name").toLowerCase()
        if (!osName.startsWith("mac")) {
            throw TaskExecuteException(
                errorMsg = "It's not mac os platform",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD
            )
        }

        // 安装
        val vmPassword = VMResourceApi().getVmByPipeline().data?.vmPassword ?: ""
        /*
        val vmPassword = if (isWorker()) {
            Client.get(BuildVMResource::class).getVmByPipeLine(buildVariables.buildId, buildVariables.vmSeqId)
        }
        else {
            Client.get(BuildAgentVMResource::class).getVmByPipeLine(
                    getProjectId(),
                    buildVariables.buildId,
                    getAgentId(),
                    getAgentSecretKey(),
                    buildVariables.vmSeqId
                    )
        }.data?.vmPassword?:""
        */
        CertInstaller.install(buildVariables.buildId, buildTask.params ?: mapOf(), vmPassword)
    }
}