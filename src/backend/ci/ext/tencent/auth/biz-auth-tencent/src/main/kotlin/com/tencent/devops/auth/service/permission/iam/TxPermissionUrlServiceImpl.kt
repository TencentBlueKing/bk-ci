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

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.RelatedResourceTypes
import com.tencent.bk.sdk.iam.dto.RelationResourceInstance
import com.tencent.bk.sdk.iam.dto.action.UrlAction
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.pojo.PermissionUrlDTO
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.iam.PermissionUrlService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class TxPermissionUrlServiceImpl @Autowired constructor(
    private val iamConfiguration: IamConfiguration,
    private val managerService: ManagerService,
    private val permissionProjectService: TxPermissionProjectServiceImpl,
    private val client: Client,
    private val authGroupService: AuthGroupService
) : PermissionUrlService {

    @Value("\${auth.webHost:#{null}}")
    val permissionCenterHost: String? = null

    /**
    {
    "system": "bk_job",  # 权限的系统
    "actions": [
    {
    "id": "execute_job",  # 操作id
    "related_resource_types": [  # 关联的资源类型, 无关联资源类型的操作, 必须为空, 资源类型的顺序必须操作注册时的顺序一致
    {
    "system": "bk_job",  # 资源类型所属的系统id
    "type": "job",  # 资源类型
    "instances": [  # 申请权限的资源实例
    [  # 带层级的实例表示
    {
    "type": "job",  # 层级节点的资源类型
    "id": "job1",  # 层级节点的资源实例id
    }
    ]
    ]
    },
    {
    "system": "bk_cmdb",  # 资源类型所属的系统id
    "type": "host",  # 操作依赖的另外一个资源类型
    "instances": [
    [
    {
    "type": "biz",
    "id": "biz1",
    }, {
    "type": "set",
    "id": "set1",
    }, {
    "type": "module",
    "id": "module1",
    }, {
    "type": "host",
    "id": "host1",
    }
    ]
    ],
    "attributes": [  # 支持配置实例的属性值
    {
    "id": "os",  # 属性的key
    "name": "操作系统",
    "values": [
    {
    "id": "linux",  # 属性的value, 可以有多个
    "name": "linux"
    }
    ]
    }
    ]
    }
    ]
    }
    ]
    }
     */
    override fun getPermissionUrl(permissionUrlDTO: List<PermissionUrlDTO>): Result<String?> {
        logger.info("get permissionUrl permissionUrlDTO: $permissionUrlDTO")
        val actions = mutableListOf<UrlAction>()
        permissionUrlDTO.forEach {
            val instanceList = it.instanceId
            val relatedResourceTypes = mutableListOf<RelatedResourceTypes>()
            var relatedResourceType = TActionUtils.extResourceType(it.resourceId)
            if (it.actionId == AuthPermission.CREATE) {
                relatedResourceType = AuthResourceType.PROJECT.value
            }

            if (instanceList == null || instanceList.isEmpty()) {
                relatedResourceTypes.add(RelatedResourceTypes(
                    iamConfiguration.systemId,
                    relatedResourceType,
                    emptyList(),
                    emptyList()
                ))
            } else {
                val relatedInstanceInfos = mutableListOf<RelationResourceInstance>()

                it.instanceId?.forEach { instance ->
                    var instanceId = instance.id
                    // 如果是流水线, 需要将pipelineId转为自增Id
                    if (instance.type == AuthResourceType.PIPELINE_DEFAULT.value) {
                        instanceId = getPipelineAutoId(instance.id)
                    }

                    // 如果instance.type为质量红线、版本体验。因为历史问题，需要特殊处理。取最上层的资源类型作为type
                    val instanceType = if (TActionUtils.extResourceTypeCheck(instance.type)) {
                        TActionUtils.extResourceType(it.resourceId)
                    } else instance.type

                    val relatedInstance = RelationResourceInstance(
                        iamConfiguration.systemId,
                        instanceType,
                        instanceId,
                        "")
                    relatedInstanceInfos.add(relatedInstance)
                }

                relatedResourceTypes.add(RelatedResourceTypes(
                    iamConfiguration.systemId,
                    relatedResourceType,
                    arrayListOf(relatedInstanceInfos),
                    emptyList()
                ))
            }
            // 创建项目需特殊处理，无需关联任务资源
            if (it.resourceId == AuthResourceType.PROJECT && it.actionId == AuthPermission.CREATE) {
                logger.info("projectCreate ${it.actionId} |${it.instanceId}| ${it.resourceId}")
                actions.add(
                    UrlAction(
                        TActionUtils.buildAction(it.actionId, it.resourceId),
                        emptyList()
                    )
                )
            } else {
                actions.add(
                    UrlAction(
                        TActionUtils.buildAction(it.actionId, it.resourceId),
                        relatedResourceTypes
                    )
                )
            }
        }
        val permissionUrlDTO = com.tencent.bk.sdk.iam.dto.PermissionUrlDTO(
            iamConfiguration.systemId,
            actions
        )
        logger.info("get permissionUrl permissionUrlDTO: $permissionUrlDTO")
        return Result(managerService.getPermissionUrl(permissionUrlDTO))
    }

    override fun getRolePermissionUrl(projectId: String, groupId: String?): String? {
        val projectRelationId = permissionProjectService.getProjectId(projectId)
        val rolePermissionUrl = if (!groupId.isNullOrEmpty()) {
            val iamGroupId = authGroupService.getRelationId(groupId.toInt())
            "user-group-detail/$iamGroupId?current_role_id=$projectRelationId&tab=group_perm"
        } else {
            "user-group?current_role_id=$projectRelationId"
        }
        return if (permissionCenterHost.isNullOrEmpty()) {
            null
        } else if (permissionCenterHost!!.endsWith("/")) {
            permissionCenterHost + rolePermissionUrl
        } else {
            "$permissionCenterHost/$rolePermissionUrl"
        }
    }

    private fun getPipelineAutoId(pipelineId: String): String {
        val pipelineInfo = client.get(ServicePipelineResource::class)
            .getPipelineInfoByPipelineId(pipelineId)?.data
            ?: return pipelineId
        return pipelineInfo.id.toString()
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxPermissionUrlServiceImpl::class.java)
    }
}
