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
import com.tencent.bk.sdk.iam.constants.AuthTypeEnum
import com.tencent.bk.sdk.iam.dto.ProviderConfigDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.action.ActionUpdateDTO
import com.tencent.bk.sdk.iam.dto.resource.RelatedResourceTypeDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceActionDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceCreateConfigAction
import com.tencent.bk.sdk.iam.dto.resource.ResourceCreateConfigDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceCreatorActionsDTO
import com.tencent.bk.sdk.iam.dto.resource.ResourceTypeChainDTO
import com.tencent.bk.sdk.iam.dto.system.SystemDTO
import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.bk.sdk.iam.service.IamActionService
import com.tencent.bk.sdk.iam.service.IamResourceService
import com.tencent.bk.sdk.iam.service.SystemService
import com.tencent.devops.auth.dao.ActionDao
import com.tencent.devops.auth.pojo.action.CreateActionDTO
import com.tencent.devops.auth.pojo.action.UpdateActionDTO
import com.tencent.devops.auth.service.action.BkResourceService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import javax.annotation.PostConstruct

class IamBkActionServiceImpl @Autowired constructor(
    override val dslContext: DSLContext,
    override val actionDao: ActionDao,
    override val resourceService: BkResourceService,
    val iamConfiguration: IamConfiguration,
    val systemService: SystemService,
    val iamActionService: IamActionService,
    val iamResourceService: IamResourceService
): BKActionServiceImpl(dslContext, actionDao, resourceService) {

    @Value("\${iam.system.callback:#{null}}")
    val iamSystemCallBack = "http://ci-auth.service.consul:21936"

    @Value("\${iam.system.clients:#{null}}")
    val iamClients = "bkci,bk_ci"

    @PostConstruct
    fun initAction() {
        try {
            // 校验系统是否存在
            try {
                systemService.systemCheck(systemId)
            } catch (iamE: IamException) {
                // 报iam异常 说明系统不存在,需要优先创建系统
                val systemInfo = SystemDTO()
                systemInfo.id = systemId
                systemInfo.name = SYSTEMNAME
                systemInfo.client = iamClients
                systemInfo.englishName = ENGLISHNAME
                val providerConfigDTO = ProviderConfigDTO()
                providerConfigDTO.path = iamSystemCallBack
                providerConfigDTO.authType = AuthTypeEnum.BASIC
                systemInfo.providerConfig = providerConfigDTO
                systemService.createSystem(systemInfo)
            }

            // 获取本地所有action
            val actionInfos = actionDao.getAllAction(dslContext, "*") ?: return
            // 匹配iam内注册的action
            val iamActions = systemService.getSystemFieldsInfo(systemId).actions.map {
                it.id
            }
            val createActions = mutableListOf<CreateActionDTO>()
            actionInfos.forEach {
                val iamAction = it.actionId
                logger.info("ci action ${it.actionId}|${it.resourceId}")
                if (!iamActions.contains(iamAction)) {
                    logger.info("ci action $iamAction need syn iam")
                    val iamCreateAction = CreateActionDTO(
                        actionType = it.actionType,
                        actionId = iamAction,
                        resourceId = it.resourceId,
                        desc = it.actionName,
                        actionEnglishName = it.actionEnglishName,
                        actionName = it.actionName
                    )
                    createActions.add(iamCreateAction)
                }
            }
            if (createActions.isEmpty()) {
                logger.info("all ci action(${actionInfos.size}) in iam")
                return
            }
            // 以本地action为源，补充iam内的action
            createActions.forEach {
                logger.info("action ${it.actionId} start syn iam")
                extSystemCreate("system", it)
                logger.info("action ${it.actionId} syn success")
            }
        } catch (e: Exception) {
            logger.error("action init fail. $e")
            throw e
        }
    }

    override fun extSystemCreate(userId: String, action: CreateActionDTO) {
        logger.info("extSystemCreate $userId $action")
        // TODO: 系统id换回ci
//        val systemInfo = systemService.getSystemFieldsInfo(iamConfiguration.systemId)
        val actionInfo = iamActionService.getAction(action.actionId)

        // 1. 优先判断action是否存在, 存在修改，不存在添加
        if (actionInfo == null) {
            // action基本数据
            val iamCreateAction = buildAction(action)
            val iamActions = mutableListOf<ActionDTO>()
            iamActions.add(iamCreateAction)
            logger.info("extSystemCreate create ${action.actionId} $iamCreateAction")
            // 新增action需要把新的action添加到对应actionGroup
            iamActionService.createAction(iamActions)
        } else {
            val iamUpdateAction = ActionUpdateDTO()
            iamUpdateAction.name = action.actionName
            iamUpdateAction.englishName = action.actionEnglishName
            iamUpdateAction.description = action.desc
            iamUpdateAction.relatedAction = buildRelationAction(action.resourceId, action.actionType)
            logger.info("extSystemCreate update ${action.actionId} $iamUpdateAction")
            iamActionService.updateAction(action.actionId, iamUpdateAction)
        }
        // 3. 维护系统新建关联yml（不存在添加，存在继续追击。 create类挂project级别，其他action挂对应资源子集）
//        createRelation(action)
    }

    override fun extSystemUpdate(userId: String, actionId: String, action: UpdateActionDTO) {
        val iamUpdateAction = ActionUpdateDTO()
        iamUpdateAction.name = action.actionName
        iamUpdateAction.englishName = action.actionEnglishName
        iamUpdateAction.description = action.desc
        iamActionService.updateAction(actionId, iamUpdateAction)
    }

    private fun createRelation(action: CreateActionDTO) {
        // TODO
//        val systemId = iamConfiguration.systemId
        val systemCreateRelationInfo = systemService.getSystemFieldsInfo(systemId).resourceCreatorActions

        // 如果资源是项目。或者其他资源但是操作类型是create。都需要加到项目的新建关联。
        if (systemCreateRelationInfo == null) {
            if (action.resourceId != AuthResourceType.PROJECT.value) {
                logger.warn("first action must project,please create project resource before ${action.actionId}")
            }
            val resourceCreatorActions = buildCreateRelation(action, systemCreateRelationInfo)
            logger.info("createRelation create ${action.actionId} $resourceCreatorActions")
            iamActionService.createResourceCreatorAction(resourceCreatorActions)
        } else {
            val resourceCreatorActions = buildCreateRelation(action, systemCreateRelationInfo)
            logger.info("createRelation update ${action.actionId} $resourceCreatorActions")
            iamActionService.updateResourceCreatorAction(resourceCreatorActions)
        }
    }

    /**
     * 示例：
        [
            {
                "id":"pipeline_execute",
                "name":"执行流水线",
                "description":"执行流水线",
                "name_en":"执行流水线",
                "related_resource_types":[
                    {
                        "id":"pipeline",
                        "system_id":"XXX",
                        "related_instance_selections":[
                            {
                                "id":"pipeline_instance",
                                "system_id":"XXX"
                            }
                        ]
                    }
                ],
                "related_actions":["project_view","pipeline_view"]
            }
        ]
     */
    private fun buildAction(action: CreateActionDTO): ActionDTO {

        // action基础数据
        val iamCreateAction = ActionDTO()
        iamCreateAction.id = action.actionId
        iamCreateAction.name = action.actionName
        iamCreateAction.englishName = action.actionEnglishName
        iamCreateAction.description = action.desc
        iamCreateAction.relatedAction = buildRelationAction(action.actionType, action.resourceId)
        iamCreateAction.type = ActionTypeEnum.parseType(action.actionType)

        // action关联资源数据
        val relationResources = mutableListOf<RelatedResourceTypeDTO>()
        // 定义action关联资源
        val relationResource = RelatedResourceTypeDTO()
        relationResource.systemId = systemId

        // TODO: systemID换回ci
        val systemId = systemId
//        relationResource.systemId = iamConfiguration.systemId

        // 定义action关联资源视图列表
        val relatedInstanceSelections = mutableListOf<ResourceTypeChainDTO>()
        // 定义action关联资源视图
        val relatedInstanceSelection = ResourceTypeChainDTO()
        relatedInstanceSelection.systemId = systemId

        // 项目相关的action。action关联资源都为project。
        if (action.resourceId == AuthResourceType.PROJECT.value) {
            // 视图绑定到project_instance
            relatedInstanceSelection.id = PROJECT_SELECT_INSTANCE
            relationResource.id = AuthResourceType.PROJECT.value

            relatedInstanceSelections.add(relatedInstanceSelection)
            relationResource.relatedInstanceSelections = relatedInstanceSelections
        } else {
            // 如果是添加操作绑定项目失败，非create操作非项目资源则绑定资源本身的视图
            if (action.actionType.contains("create")) {
                relatedInstanceSelection.id = PROJECT_SELECT_INSTANCE
                relationResource.id = AuthResourceType.PROJECT.value
            } else {
                relatedInstanceSelection.id = action.resourceId + RESOURCE_SELECT_INSTANCE
                relationResource.id = action.resourceId
            }
            relatedInstanceSelections.add(relatedInstanceSelection)
            relationResource.relatedInstanceSelections = relatedInstanceSelections
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

    private fun buildIamAction(resourceType: String, ciAction: String): String {
        return resourceType + "_" + ciAction
    }

    /**
     * project_view为最基础的action，其他的action都需关联关联此action
     * 其他资源需要额外关联改资源的view操作
     */
    private fun buildRelationAction(actionType: String, resourceType: String): List<String> {
        if (resourceType == AuthResourceType.PROJECT.value && actionType == ActionTypeEnum.VIEW.type) {
            return emptyList()
        }
        val relationActions = mutableListOf<String>()
        // 除了project资源下，view类型的操作。都会关联project_view
        val projectView = buildIamAction(AuthResourceType.PROJECT.value, AuthPermission.VIEW.value)
        relationActions.add(projectView)

        // 非project资源，且操作类型不为create。需关联改资源的view权限
        if (resourceType != AuthResourceType.PROJECT.value && actionType != AuthPermission.VIEW.value) {
            val resourceView = buildIamAction(resourceType, AuthPermission.VIEW.value)
            relationActions.add(resourceView)
        }
        return relationActions
    }

    companion object {
        private const val systemId = "fitz_test"
        private const val SYSTEMNAME = "持续集成平台"
        private const val ENGLISHNAME = "bkci"
        private const val PROJECT_SELECT_INSTANCE = "project_instance"
        private const val RESOURCE_SELECT_INSTANCE = "_instance"
        private val logger = LoggerFactory.getLogger(IamBkActionServiceImpl::class.java)
    }
}
