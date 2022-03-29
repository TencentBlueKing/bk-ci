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

package com.tencent.devops.auth.service.action.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ActionTypeEnum
import com.tencent.bk.sdk.iam.dto.SelectionDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.action.ActionUpdateDTO
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceActionDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceCreateConfigAction
import com.tencent.bk.sdk.iam.dto.resource.ResourceCreateConfigDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceCreatorActionsDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceTypeChainDTO
import com.tencent.bk.sdk.iam.service.ActionService
import com.tencent.bk.sdk.iam.service.ResourceService
import com.tencent.bk.sdk.iam.service.SystemService
import com.tencent.devops.auth.dao.ActionDao
import com.tencent.devops.auth.pojo.action.CreateActionDTO
import com.tencent.devops.auth.pojo.action.UpdateActionDTO
import com.tencent.devops.auth.service.action.BkResourceService
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class IamBkActionServiceImpl @Autowired constructor(
    override val dslContext: DSLContext,
    override val actionDao: ActionDao,
    override val resourceService: BkResourceService,
    val iamConfiguration: IamConfiguration,
    val systemService: SystemService,
    val iamActionService: ActionService,
    val iamResourceService: ResourceService
): BKActionServiceImpl(dslContext, actionDao, resourceService) {

    override fun extSystemCreate(userId: String, action: CreateActionDTO) {
        logger.info("extSystemCreate $userId $action")
        val systemInfo = systemService.getSystemFieldsInfo(iamConfiguration.systemId)
        // 1. 优先判断action是否存在, 存在修改，不存在添加
        val iamActionInfo = systemInfo.actions
        if (iamActionInfo == null) {
            // action基本数据
            val iamCreateAction = buildAction(action)
            // 新增action需要把新的action添加到对应actionGroup
            iamActionService.createAction(iamCreateAction)
        } else {
            val iamUpdateAction = ActionUpdateDTO()
            iamUpdateAction.name = action.actionName
            iamUpdateAction.englishName = action.actionEnglishName
            iamUpdateAction.description = action.desc
            iamUpdateAction.relatedAction = action.relationAction
            iamActionService.updateAction(action.actionId, iamUpdateAction)
        }
        // 3. 维护系统新建关联yml（不存在添加，存在继续追击。 create类挂project级别，其他action挂对应资源子集）
        createRelation(action)
    }

    override fun extSystemUpdate(userId: String,actionId: String, action: UpdateActionDTO) {
        val iamUpdateAction = ActionUpdateDTO()
        iamUpdateAction.name = action.actionName
        iamUpdateAction.englishName = action.actionEnglishName
        iamUpdateAction.description = action.desc
        iamUpdateAction.relatedAction = action.relationAction
        iamActionService.updateAction(actionId, iamUpdateAction)
    }

    private fun createRelation(action: CreateActionDTO) {
        val systemId = iamConfiguration.systemId
        val systemCreateRelationInfo = systemService.getSystemFieldsInfo(systemId).resourceCreatorActions

        // 如果资源是项目。或者其他资源但是操作类型是create。都需要加到项目的新建关联。
        if (systemCreateRelationInfo == null) {
            if (action.resourceId != AuthResourceType.PROJECT.value) {
                logger.warn("first action must project,please create project resource before ${action.actionId}")
            }
            val resourceCreatorActions = buildCreateRelation(action, systemCreateRelationInfo)
            iamActionService.createResourceCreatorAction(resourceCreatorActions)
        } else {
            val resourceCreatorActions = buildCreateRelation(action, systemCreateRelationInfo)
            iamActionService.updateResourceCreatorAction(resourceCreatorActions)
        }
    }

    private fun buildAction(action: CreateActionDTO): ActionDTO {

        // action基础数据
        val iamCreateAction = ActionDTO()
        iamCreateAction.id = action.actionId
        iamCreateAction.name = action.actionName
        iamCreateAction.englishName = action.actionEnglishName
        iamCreateAction.description = action.desc
        iamCreateAction.relatedAction = action.relationAction
        iamCreateAction.type = ActionTypeEnum.parseType(action.actionType)

        // action关联资源数据
        val relationResources = mutableListOf<RelatedResourceTypeDTO>()
        val relationResource = RelatedResourceTypeDTO()
        relationResource.systemId = iamConfiguration.systemId
        if (action.resourceId == AuthResourceType.PROJECT.value) {
            val relatedInstanceSelections = ResourceTypeChainDTO()
            relatedInstanceSelections.systemId = iamConfiguration.systemId
            // TODO: 视图逻辑需优化
            relatedInstanceSelections.id = "project_instance"
            relationResource.id = AuthResourceType.PROJECT.value
        } else {
            relationResource.id = AuthResourceType.get(action.resourceId).value
            val relatedInstanceSelections = ResourceTypeChainDTO()
            relatedInstanceSelections.systemId = iamConfiguration.systemId
            if (action.actionType.contains("create")) {
                // TODO: 视图逻辑需优化 1. create相关的关联项目呢视图 2.其他action关联对应action视图,需从对应的resourceType里面拿视图
                relatedInstanceSelections.id = "project_instance"
            } else {
                // TODO: 视图逻辑需优化 1. create相关的关联项目呢视图 2.其他action关联对应action视图,需从对应的resourceType里面拿视图
                relatedInstanceSelections.id = "project_instance"
            }
        }
        relationResources.add(relationResource)
        iamCreateAction.relatedResourceTypes = relationResources
        return iamCreateAction
    }

    fun buildCreateRelation(
        action: CreateActionDTO,
        systemCreateRelationInfo: ResourceCreatorActionsDTO?
    ): ResourceCreatorActionsDTO {
        var resourceCreatorActions= ResourceCreatorActionsDTO()
        if (systemCreateRelationInfo == null) {
            // 默认最先创建project相关的新建关联
            resourceCreatorActions.mode = "system"
            val resourceCreateConfig = ResourceCreateConfigDTO()
            val resourceAction= ResourceActionDTO()
            resourceAction.id = action.actionId
            resourceAction.required = false
            resourceCreateConfig.id = action.resourceId
            resourceCreateConfig.actions = arrayListOf(resourceAction)

            resourceCreatorActions.config = arrayOf(resourceCreateConfig).toMutableList()
        } else {
            // 蓝盾默认只有两级。 第一级必然是project
            val projectConfig = systemCreateRelationInfo.config[0]
            if (projectConfig.id != AuthResourceType.PROJECT.value) {
                // 第一层不是project直接报错
            }

            // 判断新action操作类型， 如果是create或者资源是project，直接追加到project（第一层）下的action
            if (action.actionType == ActionTypeEnum.CREATE.type
                || action.resourceId == AuthResourceType.PROJECT.value) {
                val projectActions = projectConfig.actions
                val newAction = ResourceActionDTO()
                newAction.id = action.actionId
                newAction.required = false
                projectActions.add(newAction)
                projectConfig.actions = projectActions
                systemCreateRelationInfo.config[0] = projectConfig
            } else {
                // 如果是其他资源的非create操作,都是操作project的子集
                // 1.如果资源没有需新创建资源 2. 资源存在则为追加资源下action
                val chainActions = projectConfig.subResourceType
                var newResourceType = true
                chainActions.forEach {
                    // 资源存在则为追加资源下的action
                    if (it.id == action.resourceId) {
                        val newAction = ResourceActionDTO()
                        newAction.id = action.actionId
                        newAction.required = false
                        it.actions.add(newAction)
                        newResourceType = false
                    }
                }

                // 资源不存在 则创建新资源子集，并将action归入新子集
                if (newResourceType) {
                    val newResourceCreateConfigAction = ResourceCreateConfigAction()
                    val newAction = ResourceActionDTO()
                    newAction.id = action.actionId
                    newAction.required = false
                    newResourceCreateConfigAction.actions = arrayListOf(newAction)
                    newResourceCreateConfigAction.id = action.resourceId
                    chainActions.add(newResourceCreateConfigAction)
                }

                // 更新project子集的action里面
                projectConfig.subResourceType = chainActions
                systemCreateRelationInfo.config[0] = projectConfig
                resourceCreatorActions = systemCreateRelationInfo
            }
        }
        return resourceCreatorActions
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IamBkActionServiceImpl::class.java)
    }
}