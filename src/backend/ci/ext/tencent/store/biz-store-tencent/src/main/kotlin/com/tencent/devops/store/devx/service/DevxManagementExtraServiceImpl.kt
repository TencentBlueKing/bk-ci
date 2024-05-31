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

package com.tencent.devops.store.devx.service

import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.service.StoreManagementExtraService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service("DEVX_MANAGEMENT_EXTRA_SERVICE")
class DevxManagementExtraServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) : StoreManagementExtraService {

    override fun doComponentDeleteCheck(storeCode: String): Result<Boolean> {
        return Result(true)
    }

    override fun deleteComponentRepoFile(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        return client.get(ServiceArchiveComponentPkgResource::class).deleteStorePkg(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
    }

    override fun uninstallComponentCheck(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String
    ): Result<Boolean> {
        // 用户是否有权限卸载
        val isInstaller = storeProjectRelDao.isInstaller(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType).type.toByte()
        )
        if (!(hasManagerPermission(projectCode, userId) || isInstaller)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.PROJECT_NO_PERMISSION,
                params = arrayOf(projectCode, storeCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        return Result(true)
    }

    private fun hasManagerPermission(projectCode: String, userId: String): Boolean {
        return authProjectApi.getProjectUsers(pipelineAuthServiceCode, projectCode, BkAuthGroup.MANAGER)
            .contains(userId)
    }
}
