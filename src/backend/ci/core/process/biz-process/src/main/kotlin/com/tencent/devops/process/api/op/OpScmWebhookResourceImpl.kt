package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineWebhookUpgradeService
import com.tencent.devops.process.trigger.scm.WebhookGrayService

@RestResource
class OpScmWebhookResourceImpl(
    private val pipelineWebhookUpgradeService: PipelineWebhookUpgradeService,
    private val webhookGrayService: WebhookGrayService
) : OpScmWebhookResource {

    override fun updateProjectNameAndTaskId(): Result<Boolean> {
        pipelineWebhookUpgradeService.updateProjectNameAndTaskId()
        return Result(true)
    }

    override fun updateWebhookSecret(scmType: String): Result<Boolean> {
        pipelineWebhookUpgradeService.updateWebhookSecret(ScmType.valueOf(scmType))
        return Result(true)
    }

    override fun updateWebhookEventInfo(
        projectId: String?
    ): Result<Boolean> {
        pipelineWebhookUpgradeService.updateWebhookEventInfo(
            projectId = projectId
        )
        return Result(true)
    }

    override fun updateWebhookProjectName(projectId: String?, pipelineId: String?): Result<Boolean> {
        pipelineWebhookUpgradeService.updateWebhookProjectName(projectId, pipelineId)
        return Result(true)
    }

    override fun addGrayRepoWhite(scmCode: String, pac: Boolean, serverRepoNames: List<String>): Result<Boolean> {
        webhookGrayService.addGrayRepoWhite(scmCode = scmCode, serverRepoNames = serverRepoNames, pac = pac)
        return Result(true)
    }

    override fun removeGrayRepoWhite(scmCode: String, pac: Boolean, serverRepoNames: List<String>): Result<Boolean> {
        webhookGrayService.removeGrayRepoWhite(scmCode = scmCode, serverRepoNames = serverRepoNames, pac = pac)
        return Result(true)
    }

    override fun isGrayRepoWhite(scmCode: String, pac: Boolean, serverRepoName: String): Result<Boolean> {
        return Result(
            webhookGrayService.isGrayRepoWhite(scmCode = scmCode, serverRepoName = serverRepoName, pac = pac)
        )
    }

    override fun addGrayRepoBlack(scmCode: String, serverRepoNames: List<String>): Result<Boolean> {
        webhookGrayService.addGrayRepoBlack(scmCode = scmCode, serverRepoNames = serverRepoNames)
        return Result(true)
    }

    override fun removeGrayRepoBlack(scmCode: String, serverRepoNames: List<String>): Result<Boolean> {
        webhookGrayService.removeGrayRepoBlack(scmCode = scmCode, serverRepoNames = serverRepoNames)
        return Result(true)
    }

    override fun isGrayRepoBlack(scmCode: String, serverRepoName: String): Result<Boolean> {
        return Result(webhookGrayService.isGrayRepoBlack(scmCode, serverRepoName))
    }

    override fun updateGrayRepoWeight(scmCode: String, weight: String): Result<Boolean> {
        webhookGrayService.updateGrayRepoWeight(scmCode = scmCode, weight = weight)
        return Result(true)
    }

    override fun getGrayRepoWeight(scmCode: String): Result<String> {
        return Result(webhookGrayService.getGrayRepoWeight(scmCode))
    }
}
