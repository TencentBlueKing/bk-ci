package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.op.OpAiUserAuditResource
import com.tencent.devops.ai.pojo.AiPromptInfo
import com.tencent.devops.ai.pojo.UserLlmConfigInfo
import com.tencent.devops.ai.service.AiPromptService
import com.tencent.devops.ai.service.UserLlmConfigService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAiUserAuditResourceImpl @Autowired constructor(
    private val promptService: AiPromptService,
    private val userLlmConfigService: UserLlmConfigService
) : OpAiUserAuditResource {
    override fun listPrompts(): Result<List<AiPromptInfo>> = Result(promptService.listAllForOp())
    override fun deletePrompt(promptId: String): Result<Boolean> = Result(promptService.deleteForOp(promptId))
    override fun listLlmConfigs(): Result<List<UserLlmConfigInfo>> = Result(userLlmConfigService.listAllForOp())
    override fun deleteLlmConfig(userId: String): Result<Boolean> = Result(userLlmConfigService.delete(userId))
}
