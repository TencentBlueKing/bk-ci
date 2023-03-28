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

package com.tencent.devops.auth.service.permission.iam

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionProjectService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class TxPermissionProjectServiceImpl @Autowired constructor(
    override val permissionRoleService: PermissionRoleService,
    override val permissionRoleMemberService: PermissionRoleMemberService,
    override val authHelper: AuthHelper,
    override val policyService: PolicyService,
    override val client: Client,
    override val iamConfiguration: IamConfiguration,
    override val deptService: DeptService,
    override val groupService: AuthGroupService,
    override val iamCacheService: IamCacheService
) : AbsPermissionProjectService(
    permissionRoleService = permissionRoleService,
    permissionRoleMemberService = permissionRoleMemberService,
    authHelper = authHelper,
    policyService = policyService,
    client = client,
    iamConfiguration = iamConfiguration,
    deptService = deptService,
    groupService = groupService,
    iamCacheService = iamCacheService
) {
    private val projectIdCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, String>()

    override fun getUserByExt(group: BkAuthGroup, projectCode: String): List<String> {
        val groupInfo = groupService.getGroupByCode(projectCode, group.value) ?: return emptyList()
        val extProjectId = getExtProjectId(projectCode)
        val groupMemberInfos = permissionRoleMemberService.getRoleMember(
            projectId = extProjectId,
            roleId = groupInfo.id,
            page = 0,
            pageSize = 1000
        ).results
        val users = mutableListOf<String>()
        groupMemberInfos.forEach {
            if (it.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
                users.add(it.id)
            }
        }
        return users
    }

    private fun getExtProjectId(projectCode: String): Int {
        val iamProjectId = if (projectIdCache.getIfPresent(projectCode) != null) {
            projectIdCache.getIfPresent(projectCode)!!
        } else {
            val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data

            if (projectInfo != null && !projectInfo.relationId.isNullOrEmpty()) {
                projectIdCache.put(projectCode, projectInfo!!.relationId!!)
            }
            projectInfo?.relationId
        }
        if (iamProjectId.isNullOrEmpty()) {
            logger.warn("[IAM] $projectCode iamProject is empty")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                defaultMessage = MessageUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()))
            )
        }
        return iamProjectId.toInt()
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxPermissionProjectServiceImpl::class.java)
    }
}
