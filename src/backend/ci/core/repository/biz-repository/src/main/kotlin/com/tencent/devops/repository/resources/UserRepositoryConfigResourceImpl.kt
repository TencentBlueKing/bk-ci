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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.UserRepositoryConfigResource
import com.tencent.devops.repository.pojo.RepositoryConfig
import com.tencent.devops.repository.pojo.enums.RepositoryConfigStatusEnum
import com.tencent.devops.scm.config.GitConfig
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRepositoryConfigResourceImpl @Autowired constructor(
    private val gitConfig: GitConfig
) : UserRepositoryConfigResource {

    companion object {
        // 后续需改造数据库字段
        val DOC_URL_MAP = mapOf(
            ScmType.GITHUB.name to "/docs/markdown/Devops//UserGuide/Setup/guidelines-bkdevops-githubapp.md"
        )
    }

    override fun list(): Result<List<RepositoryConfig>> {
        val managers = ScmType.values().map {
            val status = when {
                it == ScmType.GITHUB && gitConfig.githubClientId.isBlank() ->
                    RepositoryConfigStatusEnum.DEPLOYING

                it == ScmType.CODE_GIT && gitConfig.clientId.isBlank() ->
                    RepositoryConfigStatusEnum.DISABLED

                else ->
                    RepositoryConfigStatusEnum.OK
            }
            RepositoryConfig(
                scmType = it,
                name = I18nUtil.getCodeLanMessage(
                    messageCode = "TRIGGER_TYPE_${it.name}",
                    defaultMessage = it.name
                ),
                status = status,
                docUrl = DOC_URL_MAP[it.name] ?: ""
            )
        }
        return Result(managers)
    }
}
