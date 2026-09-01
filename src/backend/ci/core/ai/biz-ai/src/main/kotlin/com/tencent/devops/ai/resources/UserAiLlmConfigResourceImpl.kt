package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.user.UserAiLlmConfigResource
import com.tencent.devops.ai.pojo.UserLlmConfigInfo
import com.tencent.devops.ai.pojo.UserLlmConfigUpsertRequest
import com.tencent.devops.ai.service.UserLlmConfigService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAiLlmConfigResourceImpl @Autowired constructor(
    private val userLlmConfigService: UserLlmConfigService
) : UserAiLlmConfigResource {

    override fun get(userId: String): Result<UserLlmConfigInfo?> {
        return Result(userLlmConfigService.get(userId))
    }

    override fun upsert(
        userId: String,
        request: UserLlmConfigUpsertRequest
    ): Result<UserLlmConfigInfo> {
        return Result(userLlmConfigService.upsert(userId, request))
    }

    override fun delete(userId: String): Result<Boolean> {
        return Result(userLlmConfigService.delete(userId))
    }
}
