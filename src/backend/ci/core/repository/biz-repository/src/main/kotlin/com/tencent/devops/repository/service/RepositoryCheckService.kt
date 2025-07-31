/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.service.hub.ScmProviderRepositoryFactory
import com.tencent.devops.scm.api.pojo.BranchListOptions
import com.tencent.devops.scm.api.pojo.repository.ScmProviderRepository
import com.tencent.devops.scm.exception.ScmException
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryCheckService @Autowired constructor(
    private val repositoryScmConfigService: RepositoryScmConfigService,
    private val scmProviderRepositoryFactory: ScmProviderRepositoryFactory,
    private val scmApiManager: ScmApiManager
) {

    fun checkGitCredential(projectId: String, authRepository: AuthRepository): ScmProviderRepository {
        val scmConfig = repositoryScmConfigService.get(scmCode = authRepository.scmCode)
        val providerRepository = scmProviderRepositoryFactory.create(
            properties = scmConfig.providerProps,
            authRepository = authRepository
        )
        checkApiCredential(
            providerProperties = scmConfig.providerProps,
            providerRepository = providerRepository
        )
        checkClientCredential(
            providerProperties = scmConfig.providerProps,
            providerRepository = providerRepository
        )
        return providerRepository
    }

    fun checkSvnCredential(projectId: String, authRepository: AuthRepository): ScmProviderRepository {
        val scmConfig = repositoryScmConfigService.get(scmCode = authRepository.scmCode)
        val providerRepository = scmProviderRepositoryFactory.create(
            properties = scmConfig.providerProps,
            authRepository = authRepository
        )
        checkApiCredential(
            providerProperties = scmConfig.providerProps,
            providerRepository = providerRepository
        )
        return providerRepository
    }

    /**
     * 校验api凭证有效性
     *
     * 调用api接口验证
     */
    private fun checkApiCredential(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository
    ) {
        scmApiManager.listBranches(
            providerProperties = providerProperties,
            providerRepository = providerRepository,
            opts = BranchListOptions(
                page = 1,
                pageSize = 1
            )
        )
    }

    /**
     * 校验客户端凭证有效性
     *
     * 调用客户端命令验证
     */
    private fun checkClientCredential(
        providerProperties: ScmProviderProperties,
        providerRepository: ScmProviderRepository
    ) {
        try {
            scmApiManager.lsRemote(
                providerProperties = providerProperties,
                providerRepository = providerRepository
            )
        } catch (ignored: Exception) {
            throw ScmException(
                GitUtils.matchExceptionCode(ignored.message ?: "")?.let {
                    I18nUtil.getCodeLanMessage(it)
                } ?: ignored.message ?: I18nUtil.getCodeLanMessage(CommonMessageCode.GIT_SERCRT_WRONG),
                ScmType.CODE_GIT.name
            )
        }
    }
}
