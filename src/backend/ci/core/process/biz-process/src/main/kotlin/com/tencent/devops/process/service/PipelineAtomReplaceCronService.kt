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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.BusTypeEnum
import com.tencent.devops.common.api.enums.TaskStatusEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceBaseRecord
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceItemRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineAtomReplaceBaseDao
import com.tencent.devops.process.dao.PipelineAtomReplaceHistoryDao
import com.tencent.devops.process.dao.PipelineAtomReplaceItemDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.PipelineAtomReplaceHistory
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.service.template.TemplateFacadeService
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.AtomParamReplaceInfo
import com.tencent.devops.store.pojo.atom.AtomReplaceParamConvertRequest
import com.tencent.devops.store.pojo.atom.AtomVersionReplaceInfo
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.ATOM_INPUT
import com.tencent.devops.store.pojo.common.ATOM_NAMESPACE
import com.tencent.devops.store.pojo.common.ATOM_OUTPUT
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.Record
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineAtomReplaceCronService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineAtomReplaceBaseDao: PipelineAtomReplaceBaseDao,
    private val pipelineAtomReplaceItemDao: PipelineAtomReplaceItemDao,
    private val pipelineAtomReplaceHistoryDao: PipelineAtomReplaceHistoryDao,
    private val pipelineResDao: PipelineResDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val templateFacadeService: TemplateFacadeService,
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAtomReplaceCronService::class.java)
        private const val LOCK_KEY = "pipelineAtomReplace"
        private const val ITEM_PAGE_SIZE = 10
        private const val DEFAULT_PAGE_SIZE = 100
        private const val PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY = "pipeline:atom:replace:project:id"
        private const val PIPELINE_ATOM_REPLACE_PROJECT_MANAGER_KEY = "pipeline:atom:replace:project:manager"
        private const val PIPELINE_ATOM_REPLACE_FAIL_FLAG_KEY = "pipeline:atom:replace:fail:flag"
    }

    @Value("\${pipeline.atom.replaceSwitch:true}")
    private val switch: Boolean = true

    @Scheduled(cron = "0 0/1 * * * ?")
    fun pipelineAtomReplace() {
        if (!switch) {
            // 开关关闭，则不替换回滚
            return
        }
        val lock = RedisLock(redisOperation, LOCK_KEY, 3000)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            logger.info("begin pipelineAtomReplace!!")
            val atomReplaceBaseRecords = pipelineAtomReplaceBaseDao.getAtomReplaceBaseList(
                dslContext = dslContext,
                descFlag = false,
                statusList = listOf(TaskStatusEnum.INIT.name, TaskStatusEnum.HANDING.name),
                page = 1,
                pageSize = 1
            )
            atomReplaceBaseRecords?.forEach nextBase@{ atomReplaceBaseRecord ->
                val baseId = atomReplaceBaseRecord.id
                logger.info("pipelineAtomReplace baseId:$baseId replace start!!")
                try {
                    handleAtomReplaceBase(atomReplaceBaseRecord)
                } catch (ignored: Throwable) {
                    logger.warn("pipelineAtomReplace baseId:$baseId replace fail:", ignored)
                    val handleFailFlag = redisOperation.get("$PIPELINE_ATOM_REPLACE_FAIL_FLAG_KEY:$baseId")?.toBoolean()
                    // 判断是否需要处理异常情况
                    if (handleFailFlag != null && handleFailFlag) {
                        pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                            dslContext = dslContext,
                            baseId = baseId,
                            status = TaskStatusEnum.FAIL.name,
                            userId = atomReplaceBaseRecord.modifier
                        )
                        redisOperation.delete("$PIPELINE_ATOM_REPLACE_FAIL_FLAG_KEY:$baseId")
                        redisOperation.delete("$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId")
                    }
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("pipelineAtomReplace failed", ignored)
        } finally {
            lock.unlock()
        }
    }

    private fun handleAtomReplaceBase(atomReplaceBaseRecord: TPipelineAtomReplaceBaseRecord) {
        val baseId = atomReplaceBaseRecord.id
        val atomReplaceItemCount =
            pipelineAtomReplaceItemDao.getAtomReplaceItemCountByBaseId(dslContext, baseId!!)
        if (atomReplaceItemCount < 1) {
            return
        }
        val pipelineIdInfo = atomReplaceBaseRecord.pipelineIdInfo
        val projectId = atomReplaceBaseRecord.projectId
        val userId = atomReplaceBaseRecord.modifier
        // 流水线和项目信息都不传时则全量替换
        val replaceAllProjectFlag = pipelineIdInfo.isNullOrBlank() && projectId.isNullOrBlank()
        if (replaceAllProjectFlag) {
            var handleProjectPrimaryId =
                redisOperation.get("$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId")?.toLong()
            if (handleProjectPrimaryId == null) {
                handleProjectPrimaryId = client.get(ServiceProjectResource::class).getMinId().data ?: 0L
            }
            var maxHandleProjectPrimaryId = handleProjectPrimaryId + DEFAULT_PAGE_SIZE
            val projects = client.get(ServiceProjectResource::class).getProjectListById(
                minId = handleProjectPrimaryId + 1,
                maxId = maxHandleProjectPrimaryId
            ).data
            val maxProjectPrimaryId = client.get(ServiceProjectResource::class).getMaxId().data ?: 0L
            var projectCompleteFlag = false
            projects?.forEach { project ->
                val projectPrimaryId = project.id
                if (projectPrimaryId > maxHandleProjectPrimaryId) {
                    maxHandleProjectPrimaryId = projectPrimaryId
                }
                // 是否所有项目下的流水线的插件已完成替换标识
                projectCompleteFlag = maxHandleProjectPrimaryId >= maxProjectPrimaryId
                handleProjectPipelineAtom(
                    project = project,
                    projectCompleteFlag = projectCompleteFlag,
                    baseId = baseId,
                    userId = userId,
                    atomReplaceItemCount = atomReplaceItemCount
                )
            }
            if (projectCompleteFlag) {
                // 把插件替换基本信息记录状态置为”成功“
                pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                    dslContext = dslContext,
                    baseId = baseId,
                    status = TaskStatusEnum.SUCCESS.name,
                    userId = userId
                )
                // 已经替换完全部项目的流水线的插件，清除缓存
                redisOperation.delete("$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId")
                logger.info("pipelineAtomReplace baseId:$baseId replace success!!")
            } else {
                // 将下一次要处理的项目Id存入redis
                redisOperation.set(
                    key = "$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId",
                    value = maxHandleProjectPrimaryId.toString(),
                    expired = false
                )
            }
        } else {
            if (pipelineIdInfo.isNullOrBlank()) {
                if (!projectId.isNullOrBlank()) {
                    // 如果没有指定要替换插件的具体流水线信息而指定了项目，则把该项目下所有流水线下相关的插件都替换
                    val projectInfoRecord = client.get(ServiceProjectResource::class).get(projectId).data
                        ?: throw ErrorCodeException(
                            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                            params = arrayOf(projectId)
                        )
                    handleProjectPipelineAtom(
                        project = ProjectBaseInfo(projectInfoRecord.id, projectInfoRecord.englishName),
                        projectCompleteFlag = true,
                        baseId = baseId,
                        userId = userId,
                        atomReplaceItemCount = atomReplaceItemCount
                    )
                } else {
                    return
                }
            } else {
                // 如果指定了流水线信息，则只替换相应流水线下的插件
                val pipelineIdSet = JsonUtil.to(pipelineIdInfo, object : TypeReference<Set<String>>() {})
                val pipelineInfoRecords = pipelineInfoDao.listInfoByPipelineIds(
                    dslContext = dslContext,
                    pipelineIds = pipelineIdSet,
                    filterDelete = false
                )
                val pipelineInfoMap = mutableMapOf<String, TPipelineInfoRecord>()
                pipelineInfoRecords.forEach { pipelineInfoRecord ->
                    pipelineInfoMap[pipelineInfoRecord.pipelineId] = pipelineInfoRecord
                }
                handlePipelineAtomReplace(
                    projectId = null,
                    pipelineIdSet = pipelineIdSet,
                    pipelineInfoMap = pipelineInfoMap,
                    baseId = baseId,
                    userId = userId,
                    atomReplaceItemCount = atomReplaceItemCount,
                    completeFlag = true
                )
            }
            // 把插件替换基本信息记录状态置为”成功“
            pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                dslContext = dslContext,
                baseId = baseId,
                status = TaskStatusEnum.SUCCESS.name,
                userId = userId
            )
            logger.info("pipelineAtomReplace baseId:$baseId replace success!!")
        }
    }

    private fun handleProjectPipelineAtom(
        project: ProjectBaseInfo,
        projectCompleteFlag: Boolean,
        baseId: String,
        userId: String,
        atomReplaceItemCount: Long
    ) {
        var offset = 0
        do {
            val pipelineInfoRecords = pipelineInfoDao.listPipelineInfoByProject(
                dslContext = dslContext,
                projectId = project.englishName,
                offset = offset,
                limit = DEFAULT_PAGE_SIZE
            )
            val pipelineInfoMap = mutableMapOf<String, TPipelineInfoRecord>()
            val pipelineIdSet = mutableSetOf<String>()
            pipelineInfoRecords?.forEach { pipelineInfoRecord ->
                val pipelineId = pipelineInfoRecord.pipelineId
                pipelineIdSet.add(pipelineId)
                pipelineInfoMap[pipelineInfoRecord.pipelineId] = pipelineInfoRecord
            }
            // 当所有项目都执行完并且项目下的所有流水线也执行完，才认为任务完成
            val completeFlag = projectCompleteFlag && (pipelineInfoRecords?.size != DEFAULT_PAGE_SIZE)
            handlePipelineAtomReplace(
                projectId = project.englishName,
                pipelineIdSet = pipelineIdSet,
                pipelineInfoMap = pipelineInfoMap,
                baseId = baseId,
                userId = userId,
                atomReplaceItemCount = atomReplaceItemCount,
                completeFlag = completeFlag
            )
            offset += DEFAULT_PAGE_SIZE
        } while (pipelineInfoRecords?.size == DEFAULT_PAGE_SIZE)
    }

    @Suppress("UNCHECKED_CAST")
    private fun handlePipelineAtomReplace(
        projectId: String?,
        pipelineIdSet: Set<String>?,
        pipelineInfoMap: Map<String, TPipelineInfoRecord>,
        baseId: String,
        userId: String,
        atomReplaceItemCount: Long,
        completeFlag: Boolean
    ) {
        // 把插件替换基本信息记录状态置为”处理中“
        pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
            dslContext = dslContext,
            baseId = baseId,
            status = TaskStatusEnum.HANDING.name,
            userId = userId
        )
        // 当插件替换基本信息记录状态为”处理中“才需要处理异常失败的情况
        redisOperation.set(
            key = "$PIPELINE_ATOM_REPLACE_FAIL_FLAG_KEY:$baseId",
            value = "true"
        )
        val totalPages = PageUtil.calTotalPage(ITEM_PAGE_SIZE, atomReplaceItemCount)
        for (page in 1..totalPages) {
            val atomReplaceItemList = pipelineAtomReplaceItemDao.getAtomReplaceItemListByBaseId(
                dslContext = dslContext,
                baseId = baseId,
                statusList = listOf(TaskStatusEnum.INIT.name, TaskStatusEnum.HANDING.name),
                descFlag = false,
                page = page,
                pageSize = ITEM_PAGE_SIZE
            )
            atomReplaceItemList?.forEach nextItem@{ atomReplaceItem ->
                val itemId = atomReplaceItem.id
                val toAtomCode = atomReplaceItem.toAtomCode
                val toAtomVersion = atomReplaceItem.toAtomVersion
                try {
                    val toAtomInfo =
                        client.get(ServiceAtomResource::class).getAtomVersionInfo(toAtomCode, toAtomVersion).data
                            ?: return@nextItem
                    val paramReplaceInfoList = if (atomReplaceItem.paramReplaceInfo != null) JsonUtil.to(
                        atomReplaceItem.paramReplaceInfo,
                        object : TypeReference<List<AtomParamReplaceInfo>>() {}
                    ) else null
                    replacePipelineAtomByItem(
                        atomReplaceItem = atomReplaceItem,
                        toAtomInfo = toAtomInfo,
                        pipelineIdSet = pipelineIdSet,
                        pipelineInfoMap = pipelineInfoMap,
                        paramReplaceInfoList = paramReplaceInfoList,
                        baseId = baseId,
                        userId = userId
                    )
                    replaceTemplateAtomByItem(
                        atomReplaceItem = atomReplaceItem,
                        projectId = projectId,
                        toAtomInfo = toAtomInfo,
                        paramReplaceInfoList = paramReplaceInfoList,
                        baseId = baseId,
                        userId = userId
                    )
                    if (completeFlag) {
                        // 把插件替换版本信息记录状态置为”成功“
                        pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
                            dslContext = dslContext,
                            itemId = itemId,
                            status = TaskStatusEnum.SUCCESS.name,
                            userId = userId
                        )
                        logger.info("handlePipelineAtomReplace itemId:$itemId replace success!!")
                    }
                } catch (ignored: Throwable) {
                    logger.warn("replacePipelineAtomByItem failed", ignored)
                    pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
                        dslContext = dslContext,
                        itemId = itemId,
                        status = TaskStatusEnum.FAIL.name,
                        userId = userId
                    )
                    logger.info("handlePipelineAtomReplace itemId:$itemId replace fail!!")
                }
            }
        }
    }

    private fun replaceTemplateAtomByItem(
        atomReplaceItem: TPipelineAtomReplaceItemRecord,
        projectId: String?,
        userId: String,
        toAtomInfo: PipelineAtom,
        paramReplaceInfoList: List<AtomParamReplaceInfo>?,
        baseId: String
    ) {
        if (projectId != null) {
            var templatePage = 1
            do {
                val templateListModel = templateFacadeService.listTemplate(
                    projectId = projectId,
                    userId = userId,
                    templateType = TemplateType.CUSTOMIZE,
                    storeFlag = null,
                    page = templatePage,
                    pageSize = DEFAULT_PAGE_SIZE
                )
                templateListModel.models.forEach {
                    try {
                        replaceTemplateModelAtom(
                            templateModel = it,
                            projectId = projectId,
                            userId = userId,
                            atomReplaceItem = atomReplaceItem,
                            toAtomInfo = toAtomInfo,
                            paramReplaceInfoList = paramReplaceInfoList,
                            baseId = baseId
                        )
                    } catch (ignored: Throwable) {
                        logger.warn("replaceTemplateModelAtom failed", ignored)
                        pipelineAtomReplaceHistoryDao.createAtomReplaceHistory(
                            dslContext = dslContext,
                            pipelineAtomReplaceHistory = PipelineAtomReplaceHistory(
                                projectId = projectId,
                                busId = it.templateId,
                                busType = BusTypeEnum.TEMPLATE.name,
                                sourceVersion = it.version.toInt(),
                                status = TaskStatusEnum.FAIL.name,
                                baseId = baseId,
                                itemId = atomReplaceItem.id,
                                userId = userId,
                                log = getErrorMessage(ignored)
                            )
                        )
                    }
                }
                templatePage++
            } while (templateListModel.models.size == DEFAULT_PAGE_SIZE)
        }
    }

    private fun replaceTemplateModelAtom(
        templateModel: TemplateModel,
        projectId: String,
        userId: String,
        atomReplaceItem: TPipelineAtomReplaceItemRecord,
        toAtomInfo: PipelineAtom,
        paramReplaceInfoList: List<AtomParamReplaceInfo>?,
        baseId: String
    ) {
        val templateId = templateModel.templateId
        val template = templateFacadeService.getTemplate(
            projectId = projectId,
            userId = userId,
            templateId = templateId,
            version = templateModel.version
        )
        val model = template.template
        val replaceAtomFlag = generateReplacePipelineModel(
            pipelineModel = model,
            projectId = projectId,
            busId = templateId,
            fromAtomCode = atomReplaceItem.fromAtomCode,
            toAtomCode = atomReplaceItem.toAtomCode,
            toAtomInfo = toAtomInfo,
            atomVersionReplaceInfo = AtomVersionReplaceInfo(
                paramReplaceInfoList = paramReplaceInfoList,
                fromAtomVersion = atomReplaceItem.fromAtomVersion,
                toAtomVersion = atomReplaceItem.toAtomVersion
            )
        )
        if (replaceAtomFlag) {
            val targetVersion = templateFacadeService.updateTemplate(
                projectId = projectId,
                userId = template.creator,
                templateId = templateId,
                versionName = templateModel.versionName,
                template = model
            ).toInt()
            val templateVersion = templateModel.version.toInt()
            pipelineAtomReplaceHistoryDao.createAtomReplaceHistory(
                dslContext = dslContext,
                pipelineAtomReplaceHistory = PipelineAtomReplaceHistory(
                    projectId = projectId,
                    busId = templateId,
                    busType = BusTypeEnum.TEMPLATE.name,
                    sourceVersion = templateVersion,
                    targetVersion = targetVersion,
                    status = TaskStatusEnum.SUCCESS.name,
                    baseId = baseId,
                    itemId = atomReplaceItem.id,
                    userId = userId
                )
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun replacePipelineAtomByItem(
        atomReplaceItem: TPipelineAtomReplaceItemRecord,
        toAtomInfo: PipelineAtom,
        pipelineIdSet: Set<String>?,
        pipelineInfoMap: Map<String, TPipelineInfoRecord>,
        paramReplaceInfoList: List<AtomParamReplaceInfo>?,
        baseId: String,
        userId: String
    ) {
        if (pipelineIdSet == null || pipelineIdSet.isEmpty()) {
            logger.info("pipelineIdSet is empty, skip")
            return
        }
        val itemId = atomReplaceItem.id
        val toAtomVersion = atomReplaceItem.toAtomVersion
        val fromAtomVersion = atomReplaceItem.fromAtomVersion
        // 把插件替换版本信息记录状态置为”处理中“
        pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
            dslContext = dslContext,
            itemId = itemId,
            status = TaskStatusEnum.HANDING.name,
            userId = userId
        )
        // 查询需要替换插件的流水线集合
        val pipelineModelList = pipelineResDao.listLatestModelResource(dslContext, pipelineIdSet)
        pipelineModelList?.forEach nextPipelineModel@{ pipelineModelObj ->
            try {
                if (replacePipelineModelAtom(
                        pipelineModelObj = pipelineModelObj,
                        pipelineInfoMap = pipelineInfoMap,
                        fromAtomCode = atomReplaceItem.fromAtomCode,
                        toAtomCode = atomReplaceItem.toAtomCode,
                        toAtomInfo = toAtomInfo,
                        atomVersionReplaceInfo = AtomVersionReplaceInfo(
                            paramReplaceInfoList = paramReplaceInfoList,
                            fromAtomVersion = fromAtomVersion,
                            toAtomVersion = toAtomVersion
                        ),
                        baseId = baseId,
                        itemId = itemId,
                        userId = userId
                    )
                ) return@nextPipelineModel
            } catch (ignored: Throwable) {
                logger.warn("replacePipelineModelAtom failed", ignored)
                val pipelineId = pipelineModelObj["PIPELINE_ID"] as String
                val pipelineInfoRecord = pipelineInfoMap[pipelineId]
                if (pipelineInfoRecord != null) {
                    pipelineAtomReplaceHistoryDao.createAtomReplaceHistory(
                        dslContext = dslContext,
                        pipelineAtomReplaceHistory = PipelineAtomReplaceHistory(
                            projectId = pipelineInfoRecord.projectId,
                            busId = pipelineId,
                            busType = BusTypeEnum.PIPELINE.name,
                            sourceVersion = pipelineInfoRecord.version,
                            status = TaskStatusEnum.FAIL.name,
                            baseId = baseId,
                            itemId = itemId,
                            userId = userId,
                            log = getErrorMessage(ignored)
                        )
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun replacePipelineModelAtom(
        pipelineModelObj: Record,
        pipelineInfoMap: Map<String, TPipelineInfoRecord>,
        fromAtomCode: String,
        toAtomCode: String,
        toAtomInfo: PipelineAtom,
        atomVersionReplaceInfo: AtomVersionReplaceInfo,
        baseId: String,
        itemId: String,
        userId: String
    ): Boolean {
        val pipelineModel = JsonUtil.to(pipelineModelObj["MODEL"] as String, Model::class.java)
        pipelineModel.latestVersion = 0 // latestVersion置为0以便适配修改流水线的校验逻辑
        val pipelineId = pipelineModelObj["PIPELINE_ID"] as String
        val pipelineInfoRecord = pipelineInfoMap[pipelineId]
        if (pipelineInfoRecord == null) {
            logger.info("pipeline[$pipelineId] has no project, skip")
            return true
        }
        val pipelineProjectId = pipelineInfoRecord.projectId
        val channelCode = pipelineInfoRecord.channel.let { ChannelCode.valueOf(it) }
        val replaceAtomFlag = generateReplacePipelineModel(
            pipelineModel = pipelineModel,
            channelCode = channelCode,
            projectId = pipelineProjectId,
            busId = pipelineId,
            fromAtomCode = fromAtomCode,
            toAtomCode = toAtomCode,
            toAtomInfo = toAtomInfo,
            atomVersionReplaceInfo = atomVersionReplaceInfo
        )
        if (replaceAtomFlag) {
            // 更新流水线模型
            val creator = pipelineInfoRecord.lastModifyUser
            val targetVersion = pipelineRepositoryService.deployPipeline(
                model = pipelineModel,
                projectId = pipelineProjectId,
                signPipelineId = pipelineId,
                userId = creator,
                channelCode = channelCode,
                create = false
            ).version
            pipelineAtomReplaceHistoryDao.createAtomReplaceHistory(
                dslContext = dslContext,
                pipelineAtomReplaceHistory = PipelineAtomReplaceHistory(
                    projectId = pipelineProjectId,
                    busId = pipelineId,
                    busType = BusTypeEnum.PIPELINE.name,
                    sourceVersion = pipelineInfoRecord.version,
                    targetVersion = targetVersion,
                    status = TaskStatusEnum.SUCCESS.name,
                    baseId = baseId,
                    itemId = itemId,
                    userId = userId
                )
            )
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateReplacePipelineModel(
        pipelineModel: Model,
        channelCode: ChannelCode = ChannelCode.BS,
        projectId: String,
        busId: String,
        fromAtomCode: String,
        toAtomCode: String,
        toAtomInfo: PipelineAtom,
        atomVersionReplaceInfo: AtomVersionReplaceInfo
    ): Boolean {
        var replaceAtomFlag = false // 是否需要替换插件标识
        pipelineModel.stages.forEach { stage ->
            stage.containers.forEach { container ->
                val finalElements = mutableListOf<Element>()
                container.elements.forEach nextElement@{ element ->
                    val fromAtomVersion = atomVersionReplaceInfo.fromAtomVersion
                    if (element.getAtomCode() == fromAtomCode && element.version == fromAtomVersion) {
                        replaceAtomFlag = true
                        // 默认插件无需安装
                        if (toAtomInfo.defaultFlag != true) {
                            // 判断用户的项目是否安装了要替换的插件
                            val projectManager = getProjectManager(projectId) // 获取项目管理员
                            val installFlag = client.get(ServiceMarketAtomResource::class).installAtom(
                                userId = projectManager,
                                channelCode = channelCode,
                                installAtomReq = InstallAtomReq(arrayListOf(projectId), toAtomCode)
                            ).data
                            if (installFlag != true) {
                                throw ErrorCodeException(
                                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                                    errorCode = ProcessMessageCode.USER_INSTALL_ATOM_CODE_IS_INVALID
                                )
                            }
                        }
                        val toAtomPropMap = toAtomInfo.props
                        val toAtomInputParamNameList =
                            (toAtomPropMap?.get(ATOM_INPUT) as? Map<String, Any>)?.map { it.key }
                        val fromAtomInputParamMap = generateFromAtomInputParamMap(element)
                        // 生成目标替换插件的输入参数
                        val toAtomVersion = atomVersionReplaceInfo.toAtomVersion
                        val toAtomInputParamMap = generateToAtomInputParamMap(
                            toAtomCode = toAtomCode,
                            toAtomVersion = toAtomVersion,
                            toAtomInputParamNameList = toAtomInputParamNameList,
                            fromAtomInputParamMap = fromAtomInputParamMap,
                            paramReplaceInfoList = atomVersionReplaceInfo.paramReplaceInfoList
                        )
                        // 判断生成的目标插件参数集合是否符合要求
                        if (toAtomInputParamNameList?.size != toAtomInputParamMap.size) {
                            val message =
                                "bus[$busId] plugin: $fromAtomCode: ${atomVersionReplaceInfo.fromAtomVersion} " +
                                    "cannot be replaced by plugin: $toAtomCode: $toAtomVersion, parameter mapping error"
                            logger.warn(message)
                            throw ErrorCodeException(
                                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                                params = arrayOf(message),
                                defaultMessage = message
                            )
                        }
                        val toAtomJobType = toAtomInfo.jobType
                        val dataMap = generateAtomDataMap(toAtomInputParamMap, toAtomPropMap, element)
                        if (toAtomJobType == JobTypeEnum.AGENT.name) {
                            val marketBuildAtomElement = generateMarketBuildAtomElement(
                                toAtomInfo = toAtomInfo,
                                toAtomVersion = toAtomVersion,
                                element = element,
                                dataMap = dataMap
                            )
                            finalElements.add(marketBuildAtomElement)
                        } else {
                            val marketBuildLessAtomElement =
                                generateMarketBuildLessAtomElement(
                                    toAtomInfo = toAtomInfo,
                                    toAtomVersion = toAtomVersion,
                                    element = element,
                                    dataMap = dataMap
                                )
                            finalElements.add(marketBuildLessAtomElement)
                        }
                    } else {
                        finalElements.add(element)
                    }
                }
                // 替换流水线模型的element集合
                container.elements = finalElements
            }
        }
        return replaceAtomFlag
    }

    private fun getProjectManager(projectId: String): String {
        // 首先从redis中获取项目的管理员，redis中获取不到再从权限中心查
        var projectManager = redisOperation.get("$PIPELINE_ATOM_REPLACE_PROJECT_MANAGER_KEY:$projectId")
        if (projectManager == null) {
            val projectManagers =
                client.get(ServiceUserResource::class).getProjectUserRoles(projectId, BkAuthGroup.MANAGER).data
            if (projectManagers == null || projectManagers.isEmpty()) {
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = ProcessMessageCode.QUERY_USER_INFO_FAIL
                )
            }
            projectManager = projectManagers[0]
            redisOperation.set(
                key = "$PIPELINE_ATOM_REPLACE_PROJECT_MANAGER_KEY:$projectId",
                value = projectManager,
                expiredInSecond = TimeUnit.DAYS.toSeconds(3)
            )
        }
        return projectManager
    }

    private fun getErrorMessage(t: Throwable): String? {
        var message = t.message
        if (message != null && message.length > 128) {
            message = message.substring(0, 127)
        }
        return message
    }

    private fun generateAtomDataMap(
        toAtomInputParamMap: MutableMap<String, Any?>,
        toAtomPropMap: Map<String, Any>,
        element: Element
    ): MutableMap<String, Any> {
        val dataMap = mutableMapOf<String, Any>(
            ATOM_INPUT to toAtomInputParamMap
        )
        val outputParamMap = toAtomPropMap[ATOM_OUTPUT]
        if (outputParamMap != null) {
            dataMap[ATOM_OUTPUT] = outputParamMap
        }
        val fromAtomNameSpace = generateFromAtomNameSpace(element)
        if (fromAtomNameSpace != null) {
            // 生成目标替换插件的命名空间
            dataMap[ATOM_NAMESPACE] = fromAtomNameSpace
        }
        return dataMap
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateFromAtomInputParamMap(element: Element): Map<String, Any>? {
        return when (element) {
            is MarketBuildAtomElement -> {
                element.data[ATOM_INPUT] as? Map<String, Any>
            }
            is MarketBuildLessAtomElement -> {
                element.data[ATOM_INPUT] as? Map<String, Any>
            }
            else -> {
                JsonUtil.toMap(element)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateFromAtomNameSpace(element: Element): String? {
        return when (element) {
            is MarketBuildAtomElement -> {
                element.data[ATOM_NAMESPACE] as? String
            }
            is MarketBuildLessAtomElement -> {
                element.data[ATOM_NAMESPACE] as? String
            }
            else -> {
                JsonUtil.toMap(element)[ATOM_NAMESPACE] as? String
            }
        }
    }

    private fun generateToAtomInputParamMap(
        toAtomCode: String,
        toAtomVersion: String,
        toAtomInputParamNameList: List<String>?,
        fromAtomInputParamMap: Map<String, Any>?,
        paramReplaceInfoList: List<AtomParamReplaceInfo>?
    ): MutableMap<String, Any?> {
        val toAtomInputParamMap = mutableMapOf<String, Any?>()
        toAtomInputParamNameList?.forEach { toAtomInputParamName ->
            // 如果参数名一样则从被替换插件取值
            if (fromAtomInputParamMap != null) {
                val fromAtomInputParamValue = fromAtomInputParamMap[toAtomInputParamName]
                if (fromAtomInputParamValue != null) {
                    toAtomInputParamMap[toAtomInputParamName] = fromAtomInputParamValue
                }
            }
            // 如果有插件参数替换映射信息则根据映射关系替换
            run handleParamReplace@{
                paramReplaceInfoList?.forEach { paramReplaceInfo ->
                    if (generateToAtomInputParam(
                            paramReplaceInfo = paramReplaceInfo,
                            toAtomInputParamName = toAtomInputParamName,
                            toAtomCode = toAtomCode,
                            toAtomVersion = toAtomVersion,
                            fromAtomInputParamMap = fromAtomInputParamMap,
                            toAtomInputParamMap = toAtomInputParamMap
                        )
                    ) return@handleParamReplace
                }
            }
        }
        return toAtomInputParamMap
    }

    private fun generateToAtomInputParam(
        paramReplaceInfo: AtomParamReplaceInfo,
        toAtomInputParamName: String,
        toAtomCode: String,
        toAtomVersion: String,
        fromAtomInputParamMap: Map<String, Any>?,
        toAtomInputParamMap: MutableMap<String, Any?>
    ): Boolean {
        val fromParamName = paramReplaceInfo.fromParamName
        val toParamName = paramReplaceInfo.toParamName
        // 获取参数自定义转换接口路径
        val paramConvertUrl = paramReplaceInfo.paramConvertUrl
        if (!paramConvertUrl.isNullOrBlank() && toAtomInputParamName == toParamName) {
            // 参数自定义转换
            val atomReplaceParamConvertRequest = AtomReplaceParamConvertRequest(
                toAtomCode = toAtomCode,
                toAtomVersion = toAtomVersion,
                fromField = fromParamName,
                fromFieldValue = fromAtomInputParamMap?.get(fromParamName),
                toField = toParamName,
                toFieldDefaultValue = paramReplaceInfo.toParamValue
            )
            val response = OkhttpUtils.doPost(paramConvertUrl, JsonUtil.toJson(atomReplaceParamConvertRequest))
            val responseContent = response.body!!.string()
            val errorMessage = "$fromParamName convert $toParamName fail"
            if (!response.isSuccessful) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                    params = arrayOf(errorMessage),
                    defaultMessage = errorMessage
                )
            }
            val result = JsonUtil.to(responseContent, object : TypeReference<Result<Any?>>() {})
            if (result.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                    params = arrayOf(errorMessage),
                    defaultMessage = errorMessage
                )
            }
            toAtomInputParamMap[toAtomInputParamName] = result.data
            return true
        } else if (toAtomInputParamName == toParamName) {
            val inputParamValue = if (paramReplaceInfo.toParamValue != null) {
                paramReplaceInfo.toParamValue
            } else {
                fromAtomInputParamMap?.get(fromParamName)
            }
            if (inputParamValue != null) {
                toAtomInputParamMap[toAtomInputParamName] = inputParamValue
            }
            return true
        }
        return false
    }

    private fun generateMarketBuildLessAtomElement(
        toAtomInfo: PipelineAtom,
        toAtomVersion: String,
        element: Element,
        dataMap: MutableMap<String, Any>
    ): MarketBuildLessAtomElement {
        val marketBuildLessAtomElement = MarketBuildLessAtomElement(
            name = element.name,
            id = element.id,
            status = element.status,
            atomCode = toAtomInfo.atomCode,
            version = toAtomVersion,
            data = dataMap
        )
        setElementBaseInfo(marketBuildLessAtomElement, element)
        return marketBuildLessAtomElement
    }

    private fun setElementBaseInfo(
        element: Element,
        dataElement: Element
    ) {
        element.executeCount = dataElement.executeCount
        element.canRetry = dataElement.canRetry
        element.elapsed = dataElement.elapsed
        element.startEpoch = dataElement.startEpoch
        element.templateModify = dataElement.templateModify
        element.additionalOptions = dataElement.additionalOptions
        element.errorType = dataElement.errorType
        element.errorCode = dataElement.errorCode
        element.errorMsg = dataElement.errorMsg
    }

    private fun generateMarketBuildAtomElement(
        toAtomInfo: PipelineAtom,
        toAtomVersion: String,
        element: Element,
        dataMap: MutableMap<String, Any>
    ): MarketBuildAtomElement {
        val marketBuildAtomElement = MarketBuildAtomElement(
            name = element.name,
            id = element.id,
            status = element.status,
            atomCode = toAtomInfo.atomCode,
            version = toAtomVersion,
            data = dataMap
        )
        setElementBaseInfo(marketBuildAtomElement, element)
        return marketBuildAtomElement
    }
}
