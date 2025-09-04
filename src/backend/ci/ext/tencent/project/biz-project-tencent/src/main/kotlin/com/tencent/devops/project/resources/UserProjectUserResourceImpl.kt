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

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.user.UserProjectUserResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.UserRole
import com.tencent.devops.project.pojo.user.ProjectUser
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectLocalService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.tof.TOFService
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class UserProjectUserResourceImpl @Autowired constructor(
    private val tofService: TOFService,
    private val projectLocalService: ProjectLocalService,
    private val serviceCode: PipelineAuthServiceCode,
    private val projectService: ProjectService
) : UserProjectUserResource {

    @Value("\${avatar.url:#{null}}")
    private val avatarUrl: String? = null

    @Value("\${devx.loginFqdn:#{null}}")
    private val devxLoginFqdn: String? = null

    override fun get(userId: String, bkToken: String?): Result<ProjectUser> {
        val name = if (UserUtil.isTaiUser(userId)) {
            getTaiUser(bkToken)
        } else {
            bkToken?.let { tofService.getStaffInfo(userId, it).chineseName } ?: userId
        }
        return Result(
            ProjectUser(
                chineseName = name,
                avatarUrl = avatarUrl?.replace("##UserId##", userId) ?: "",
                username = userId
            )
        )
    }

    override fun getDetail(userId: String, bkToken: String): Result<UserDeptDetail> {
        return Result(tofService.getUserDeptDetail(userId, bkToken))
    }

    override fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        return projectLocalService.getProjectUsers(accessToken, userId, projectCode)
    }

    override fun getProjectUserRoles(accessToken: String, userId: String, projectCode: String): Result<List<UserRole>> {
        return Result(projectLocalService.getProjectUserRoles(accessToken, userId, projectCode, serviceCode))
    }

    override fun mangerRoleCheck(userId: String, projectCode: String): Result<Boolean> {
        return Result(
            projectService.verifyUserProjectPermission(
                userId = userId,
                projectId = projectCode,
                accessToken = null,
                permission = AuthPermission.MANAGE
            )
        )
    }

    /**
     * 获取太湖账号信息
     */
    private fun getTaiUser(bkToken: String?): String {
        val url = "$devxLoginFqdn/login/accounts/get_user/?bk_token=$bkToken"
        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use {
            val resp = it.body!!.string()
            if (!it.isSuccessful) {
                logger.error("get tai user error , resp: $resp")
                throw RemoteServiceException("Get tai user fail")
            }
            logger.info("Get tai user resp : $resp")
            return resp
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserProjectUserResourceImpl::class.java)
    }
}
