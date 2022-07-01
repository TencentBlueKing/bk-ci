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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.auth.pojo.PermissionUrlDTO
import com.tencent.devops.auth.service.iam.PermissionUrlService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.Action
import com.tencent.devops.common.auth.api.pojo.EsbPermissionUrlReq
import com.tencent.devops.common.auth.api.pojo.RelatedResourceTypes
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.auth.utils.ActionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class BkPermissionUrlService @Autowired constructor(
    val iamEsbService: IamEsbService,
    @Autowired(required = false) // v3 才会有
    val iamConfiguration: IamConfiguration?,
    val bkPermissionProjectService: BkPermissionProjectService
) : PermissionUrlService {

    @Value("\${auth.webHost:#{null}}")
    val permissionCenterHost: String? = null

    override fun getPermissionUrl(permissionUrlDTO: List<PermissionUrlDTO>): Result<String?> {
        logger.info("get permissionUrl permissionUrlDTO: $permissionUrlDTO")
        val actions = mutableListOf<Action>()
        permissionUrlDTO.map {
            val resourceType = ActionUtils.buildAction(it.resourceId, it.actionId)
            val instanceList = it.instanceId
            val relatedResourceTypes = mutableListOf<RelatedResourceTypes>()
            var relatedResourceType = it.resourceId.value
            if (it.actionId == AuthPermission.CREATE) {
                relatedResourceType = AuthResourceType.PROJECT.value
            }

            if (instanceList == null || instanceList.isEmpty()) {
                relatedResourceTypes.add(RelatedResourceTypes(
                    system = iamConfiguration!!.systemId,
                    type = relatedResourceType,
                    instances = emptyList()
                ))
            } else {
                relatedResourceTypes.add(RelatedResourceTypes(
                    system = iamConfiguration!!.systemId,
                    type = relatedResourceType,
                    instances = listOf(instanceList))
                )
            }

            // 创建项目需特殊处理，无需关联任务资源
            if (it.resourceId == AuthResourceType.PROJECT && it.actionId == AuthPermission.CREATE) {
                logger.info("projectCreate ${it.actionId} |${it.instanceId}| ${it.resourceId}")
                actions.add(
                    Action(
                        id = resourceType,
                        related_resource_types = emptyList()
                    )
                )
            } else {
                actions.add(
                    Action(
                        id = resourceType,
                        related_resource_types = relatedResourceTypes
                    )
                )
            }
        }
        val iamEsbReq = EsbPermissionUrlReq(
            system = iamConfiguration!!.systemId,
            actions = actions,
            bk_app_code = "",
            bk_app_secret = "",
            bk_username = "admin"
        )
        logger.info("get permissionUrl iamEsbReq: $iamEsbReq")
        return Result(iamEsbService.getPermissionUrl(iamEsbReq))
    }

    override fun getRolePermissionUrl(projectId: String, groupId: String?): String? {
        val projectRelationId = bkPermissionProjectService.getProjectId(projectId)
        val rolePermissionUrl = if (!groupId.isNullOrEmpty()) {
            "user-group-detail/$groupId?current_role_id=$projectRelationId&tab=group_perm"
        } else {
            "/user-group?current_role_id=$projectRelationId"
        }
        return if (permissionCenterHost.isNullOrEmpty()) {
            null
        } else if (permissionCenterHost!!.endsWith("/")) {
            permissionCenterHost + rolePermissionUrl
        } else {
            "$permissionCenterHost/$rolePermissionUrl"
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(BkPermissionUrlService::class.java)
    }
}
