/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.user.UserAiPromptResource
import com.tencent.devops.ai.pojo.AiPromptCreate
import com.tencent.devops.ai.pojo.AiPromptInfo
import com.tencent.devops.ai.pojo.AiPromptUpdate
import com.tencent.devops.ai.pojo.SlashPromptVO
import com.tencent.devops.ai.service.AiPromptService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

/** 提示词管理 API 接口实现。 */
@RestResource
class UserAiPromptResourceImpl @Autowired constructor(
    private val aiPromptService: AiPromptService
) : UserAiPromptResource {

    override fun create(
        userId: String,
        promptCreate: AiPromptCreate
    ): Result<AiPromptInfo> {
        return Result(aiPromptService.createPrompt(userId, promptCreate))
    }

    override fun list(userId: String): Result<List<AiPromptInfo>> {
        return Result(aiPromptService.listPrompts(userId))
    }

    override fun listAll(
        userId: String,
        projectId: String?
    ): Result<List<SlashPromptVO>> {
        return Result(aiPromptService.listAllPrompts(userId, projectId))
    }

    override fun update(
        userId: String,
        promptId: String,
        promptUpdate: AiPromptUpdate
    ): Result<Boolean> {
        return Result(
            aiPromptService.updatePrompt(userId, promptId, promptUpdate)
        )
    }

    override fun delete(userId: String, promptId: String): Result<Boolean> {
        return Result(aiPromptService.deletePrompt(userId, promptId))
    }
}
