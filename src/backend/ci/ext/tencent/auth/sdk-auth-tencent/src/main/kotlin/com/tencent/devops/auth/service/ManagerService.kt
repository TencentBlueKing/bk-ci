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

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.api.manager.ServiceManagerUserResource
import com.tencent.devops.auth.pojo.ProjectOrgInfo
import com.tencent.devops.auth.pojo.UserPermissionInfo
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

class ManagerService @Autowired constructor(
    val client: Client
) {
    private val userPermissionMap = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String/*userId*/, Map<String/*organizationId*/, UserPermissionInfo>>()

    private val projectInfoMap = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .build<String/*userId*/, ProjectOrgInfo?>()

    fun isManagerPermission(userId: String, projectId: String, resourceType: AuthResourceType, authPermission: AuthPermission): Boolean {

        logger.info("isManagerPermission $userId| $projectId| ${resourceType.value} | ${authPermission.value}")
        // 从缓存内获取用户管理员信息，若缓存击穿，调用auth服务获取源数据，并刷入内存
        val manageInfo = if (userPermissionMap.getIfPresent(userId) == null) {
            val remoteManagerInfo = client.get(ServiceManagerUserResource::class).getManagerInfo(userId)
            if (remoteManagerInfo.data != null && remoteManagerInfo.data!!.isNotEmpty()) {
                userPermissionMap.put(userId, remoteManagerInfo.data!!)
                remoteManagerInfo.data
            } else {
                null
            }
        } else {
            userPermissionMap.getIfPresent(userId)
        }
        logger.info("user managerInfo $userId| $manageInfo")
        if (manageInfo == null) {
            // 用户没有管理员相关信息
            return false
        }

        // 从缓存内获取项目组织信息，若缓存击穿，调用project服务获取源数据，并刷入内存
        val projectCacheOrgInfo = projectInfoMap.getIfPresent(projectId)

        val projectOrgInfo = if (projectCacheOrgInfo == null) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId)
            if (projectVo.data == null) {
                logger.info("get projectInfo is empty, $projectId")
                null
            } else {
                val remoteProjectOrgInfo = ProjectOrgInfo(
                    bgId = projectVo!!.data?.bgId ?: "0",
                    deptId = projectVo!!.data?.deptId,
                    centerId = projectVo!!.data?.centerId
                )
                projectInfoMap.put(projectId, remoteProjectOrgInfo)
                remoteProjectOrgInfo
            }
        } else {
            projectInfoMap.getIfPresent(projectId)
        }

        logger.info("project org Info: $projectId, $projectOrgInfo")
        if (projectOrgInfo == null) {
            logger.info("project OrgInfo is empty $projectId")
            return false
        }

        var isManagerPermission = false

        run managerPermissionFor@{
            // 匹配管理员组织信息与项目组织信息
            manageInfo.keys.forEach orgForEach@{ orgId ->
                val managerPermission = manageInfo[orgId] ?: return@orgForEach
                val isOrgEqual =
                when (managerPermission.organizationLevel) {
                    1 -> projectOrgInfo!!.bgId == managerPermission.organizationId.toString()
                    2 -> projectOrgInfo!!.deptId == managerPermission.organizationId.toString()
                    3 -> projectOrgInfo!!.centerId == managerPermission.organizationId.toString()
                    else -> false
                }
                if (!isOrgEqual) {
                    // 组织信息未匹配
                    return@orgForEach
                }
                logger.info("managerUser project org check success $userId $projectId $projectOrgInfo")
                // 匹配管理员内的资源类型与用户操作的资源类型
                val orgManagerPermissionMap = managerPermission.permissionMap
                orgManagerPermissionMap.keys.forEach resourceForEach@{ resourceKey ->
                    if (resourceKey == resourceType) {
                        // 资源类型一致的情况下，匹配action是否一致
                        val orgManagerPermissionList = orgManagerPermissionMap[resourceKey]
                        if (orgManagerPermissionList == null || orgManagerPermissionList.isEmpty()) {
                            return@resourceForEach
                        }

                        if (orgManagerPermissionList.contains(authPermission)) {
                            logger.info("$userId has $projectId ${resourceType.value} ${authPermission.value} $projectOrgInfo manager permission")
                            isManagerPermission = true
                            return@managerPermissionFor
                        }
                    }
                }
            }
        }
        return isManagerPermission
    }

    companion object {
        val logger = LoggerFactory.getLogger(ManagerService::class.java)
    }
}
