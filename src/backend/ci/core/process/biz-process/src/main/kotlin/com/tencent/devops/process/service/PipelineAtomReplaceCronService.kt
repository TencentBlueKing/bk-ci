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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.TaskStatusEnum
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceItemRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.dao.PipelineAtomReplaceBaseDao
import com.tencent.devops.process.dao.PipelineAtomReplaceHistoryDao
import com.tencent.devops.process.dao.PipelineAtomReplaceItemDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.pojo.atom.AtomParamReplaceInfo
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.ATOM_INPUT
import com.tencent.devops.store.pojo.common.ATOM_NAMESPACE
import com.tencent.devops.store.pojo.common.ATOM_OUTPUT
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PipelineAtomReplaceCronService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineAtomReplaceBaseDao: PipelineAtomReplaceBaseDao,
    private val pipelineAtomReplaceItemDao: PipelineAtomReplaceItemDao,
    private val pipelineAtomReplaceHistoryDao: PipelineAtomReplaceHistoryDao,
    private val pipelineResDao: PipelineResDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAtomReplaceCronService::class.java)
        private const val LOCK_KEY = "pipelineAtomReplace"
        private const val PAGE_SIZE = 100
        private const val PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY = "pipeline:atom:replace:project:id"
        private const val PIPELINE_ATOM_REPLACE_FAIL_FLAG_KEY = "pipeline:atom:replace:fail:flag"
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    fun pipelineAtomReplace() {
        val lock = RedisLock(redisOperation, LOCK_KEY, 3000)
        var baseId: String? = null
        var userId: String? = null
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
                baseId = atomReplaceBaseRecord.id
                val atomReplaceItemCount =
                    pipelineAtomReplaceItemDao.getAtomReplaceItemCountByBaseId(dslContext, baseId!!)
                if (atomReplaceItemCount < 1) {
                    return@nextBase
                }
                val pipelineIdInfo = atomReplaceBaseRecord.pipelineIdInfo
                val projectId = atomReplaceBaseRecord.projectId
                val fromAtomCode = atomReplaceBaseRecord.fromAtomCode
                val toAtomCode = atomReplaceBaseRecord.toAtomCode
                userId = atomReplaceBaseRecord.creator
                val replaceAllProjectFlag = pipelineIdInfo.isNullOrBlank() && projectId.isNullOrBlank()
                if (replaceAllProjectFlag) {
                    var handleProjectPrimaryId =
                        redisOperation.get("$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId")?.toLong()
                    if (handleProjectPrimaryId == null) {
                        handleProjectPrimaryId = client.get(ServiceProjectResource::class).getMinId().data ?: 0L
                    }
                    var maxHandleProjectPrimaryId = handleProjectPrimaryId + PAGE_SIZE
                    val projects = client.get(ServiceProjectResource::class).getProjectListById(
                        minId = handleProjectPrimaryId + 1,
                        maxId = maxHandleProjectPrimaryId
                    ).data
                    val maxProjectPrimaryId = client.get(ServiceProjectResource::class).getMaxId().data ?: 0L
                    var completeFlag = false
                    projects?.forEach { project ->
                        val projectPrimaryId = project.id
                        if (projectPrimaryId > maxHandleProjectPrimaryId) {
                            maxHandleProjectPrimaryId = projectPrimaryId
                        }
                        val pipelineIdSet =
                            pipelineInfoDao.listPipelineIdByProject(dslContext, project.englishName).toSet()
                        // 是否所有项目下的流水线的插件已完成替换标识
                        completeFlag = maxHandleProjectPrimaryId >= maxProjectPrimaryId
                        handlePipelineAtomReplace(
                            pipelineIdSet = pipelineIdSet,
                            baseId = baseId!!,
                            userId = userId!!,
                            atomReplaceItemCount = atomReplaceItemCount,
                            toAtomCode = toAtomCode,
                            fromAtomCode = fromAtomCode,
                            completeFlag = completeFlag
                        )
                    }
                    if (completeFlag) {
                        // 把插件替换基本信息记录状态置为”成功“
                        pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                            dslContext = dslContext,
                            baseId = baseId!!,
                            status = TaskStatusEnum.SUCCESS.name,
                            userId = userId!!
                        )
                        // 已经替换完全部项目的流水线的插件，清除缓存
                        redisOperation.delete("$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId")
                        logger.info("pipelineAtomReplace baseId:$baseId replace success")
                        return@nextBase
                    } else {
                        // 将下一次要处理的项目Id存入redis
                        redisOperation.set(
                            key = "$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId",
                            value = maxHandleProjectPrimaryId.toString(),
                            expired = false
                        )
                    }
                } else {
                    val pipelineIdSet = if (pipelineIdInfo.isNullOrBlank()) {
                        if (!projectId.isNullOrBlank()) {
                            // 如果没有指定要替换插件的具体流水线信息而指定了项目，则把该项目下所有流水线下相关的插件都替换
                            pipelineInfoDao.listPipelineIdByProject(dslContext, projectId).toSet()
                        } else {
                            null
                        }
                    } else {
                        JsonUtil.to(pipelineIdInfo, object : TypeReference<Set<String>>() {})
                    }
                    if (handlePipelineAtomReplace(
                            pipelineIdSet = pipelineIdSet,
                            baseId = baseId!!,
                            userId = userId!!,
                            atomReplaceItemCount = atomReplaceItemCount,
                            toAtomCode = toAtomCode,
                            fromAtomCode = fromAtomCode,
                            completeFlag = true
                        )
                    ) {
                        // 把插件替换基本信息记录状态置为”成功“
                        pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                            dslContext = dslContext,
                            baseId = baseId!!,
                            status = TaskStatusEnum.SUCCESS.name,
                            userId = userId!!
                        )
                        return@nextBase
                    }
                }
            }
        } catch (t: Throwable) {
            logger.warn("pipelineAtomReplace failed", t)
            val handleFailFlag = redisOperation.get("$PIPELINE_ATOM_REPLACE_FAIL_FLAG_KEY:$baseId")?.toBoolean()
            if (handleFailFlag != null && handleFailFlag) {
                pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                    dslContext = dslContext,
                    baseId = baseId!!,
                    status = TaskStatusEnum.FAIL.name,
                    userId = userId!!
                )
                redisOperation.delete("$PIPELINE_ATOM_REPLACE_FAIL_FLAG_KEY:$baseId")
                redisOperation.delete("$PIPELINE_ATOM_REPLACE_PROJECT_ID_KEY:$baseId")
            }
        } finally {
            lock.unlock()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handlePipelineAtomReplace(
        pipelineIdSet: Set<String>?,
        baseId: String,
        userId: String,
        atomReplaceItemCount: Long,
        toAtomCode: String,
        fromAtomCode: String,
        completeFlag: Boolean
    ): Boolean {
        // 要替换的流水线ID集合为空，则把任务表的状态置为成功
        if (pipelineIdSet == null || pipelineIdSet.isEmpty()) {
            logger.info("pipelineIdSet is empty, skip")
            if (completeFlag) {
                dslContext.transaction { t ->
                    val context = DSL.using(t)
                    pipelineAtomReplaceItemDao.updateAtomReplaceItemByBaseId(
                        dslContext = context,
                        baseId = baseId,
                        status = TaskStatusEnum.SUCCESS.name,
                        userId = userId
                    )
                }
            }
            return true
        }
        val pipelineInfoRecords = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            pipelineIds = pipelineIdSet,
            filterDelete = false
        )
        val pipelineInfoMap = mutableMapOf<String, TPipelineInfoRecord>()
        pipelineInfoRecords.forEach { pipelineInfoRecord ->
            pipelineInfoMap[pipelineInfoRecord.pipelineId] = pipelineInfoRecord
        }
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
        val totalPages = PageUtil.calTotalPage(PAGE_SIZE, atomReplaceItemCount)
        for (page in 1..totalPages) {
            val atomReplaceItemList = pipelineAtomReplaceItemDao.getAtomReplaceItemListByBaseId(
                dslContext = dslContext,
                baseId = baseId,
                statusList = listOf(TaskStatusEnum.INIT.name, TaskStatusEnum.HANDING.name),
                descFlag = false,
                page = page,
                pageSize = PAGE_SIZE
            )
            atomReplaceItemList?.forEach nextItem@{ atomReplaceItem ->
                try {
                    if (replacePipelineAtomByItem(
                            atomReplaceItem = atomReplaceItem,
                            fromAtomCode = fromAtomCode,
                            toAtomCode = toAtomCode,
                            pipelineIdSet = pipelineIdSet,
                            pipelineInfoMap = pipelineInfoMap,
                            completeFlag = completeFlag,
                            baseId = baseId,
                            userId = userId
                        )
                    ) return@nextItem
                } catch (t: Throwable) {
                    logger.warn("replacePipelineAtomByItem failed", t)
                    val itemId = atomReplaceItem.id
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
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun replacePipelineAtomByItem(
        atomReplaceItem: TPipelineAtomReplaceItemRecord,
        fromAtomCode: String,
        toAtomCode: String,
        pipelineIdSet: Set<String>,
        pipelineInfoMap: Map<String, TPipelineInfoRecord>,
        completeFlag: Boolean,
        baseId: String,
        userId: String
    ): Boolean {
        val itemId = atomReplaceItem.id
        val toAtomVersion = atomReplaceItem.toAtomVersion
        val toAtomInfo =
            client.get(ServiceAtomResource::class).getAtomVersionInfo(toAtomCode, toAtomVersion).data
                ?: return true
        val fromAtomVersion = atomReplaceItem.fromAtomVersion
        val paramReplaceInfoList = if (atomReplaceItem.paramReplaceInfo != null) JsonUtil.to(
            atomReplaceItem.paramReplaceInfo,
            object : TypeReference<List<AtomParamReplaceInfo>>() {}
        ) else null
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
                        fromAtomCode = fromAtomCode,
                        toAtomInfo = toAtomInfo,
                        fromAtomVersion = fromAtomVersion,
                        toAtomCode = toAtomCode,
                        toAtomVersion = toAtomVersion,
                        paramReplaceInfoList = paramReplaceInfoList,
                        baseId = baseId,
                        userId = userId
                    )
                ) return@nextPipelineModel
            } catch (t: Throwable) {
                logger.warn("replacePipelineModelAtom failed", t)
            }
        }
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
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun replacePipelineModelAtom(
        pipelineModelObj: Record,
        pipelineInfoMap: Map<String, TPipelineInfoRecord>,
        fromAtomCode: String,
        fromAtomVersion: String,
        toAtomCode: String,
        toAtomVersion: String,
        toAtomInfo: PipelineAtom,
        paramReplaceInfoList: List<AtomParamReplaceInfo>?,
        baseId: String,
        userId: String
    ): Boolean {
        val pipelineModel = JsonUtil.to(pipelineModelObj["MODEL"] as String, Model::class.java)
        val pipelineId = pipelineModelObj["PIPELINE_ID"] as String
        val pipelineInfoRecord = pipelineInfoMap[pipelineId]
        if (pipelineInfoRecord == null) {
            logger.info("pipeline[$pipelineId] has no project, skip")
            return true
        }
        val pipelineProjectId = pipelineInfoRecord.pipelineId
        var replaceAtomFlag = false // 是否需要替换插件标识
        pipelineModel.stages.forEach { stage ->
            stage.containers.forEach { container ->
                val finalElements = mutableListOf<Element>()
                container.elements.forEach nextElement@{ element ->
                    if (element.getAtomCode() == fromAtomCode) {
                        replaceAtomFlag = true
                        val toAtomPropMap = toAtomInfo.props
                        val toAtomInputParamNameList =
                            (toAtomPropMap?.get(ATOM_INPUT) as? Map<String, Any>)?.map { it.key }
                        val fromAtomInputParamMap = generateFromAtomInputParamMap(element)
                        // 生成目标替换插件的输入参数
                        val toAtomInputParamMap = generateToAtomInputParamMap(
                            toAtomInputParamNameList = toAtomInputParamNameList,
                            fromAtomInputParamMap = fromAtomInputParamMap,
                            paramReplaceInfoList = paramReplaceInfoList
                        )
                        // 判断生成的目标插件参数集合是否符合要求
                        if (toAtomInputParamNameList?.size != toAtomInputParamMap.size) {
                            logger.warn("plugin: $fromAtomCode: $fromAtomVersion cannot be replaced by plugin: $toAtomCode: $toAtomVersion, parameter mapping error")
                            return@nextElement
                        }
                        val toAtomJobType = toAtomInfo.jobType
                        val dataMap = generateAtomDataMap(toAtomInputParamMap, toAtomPropMap, element)
                        if (toAtomJobType == JobTypeEnum.AGENT.name) {
                            val marketBuildAtomElement = generateMarketBuildAtomElement(
                                toAtomInfo = toAtomInfo,
                                element = element,
                                dataMap = dataMap
                            )
                            finalElements.add(marketBuildAtomElement)
                        } else {
                            val marketBuildLessAtomElement =
                                generateMarketBuildLessAtomElement(
                                    toAtomInfo = toAtomInfo,
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
        if (replaceAtomFlag) {
            // 更新流水线模型
            val creator = pipelineInfoRecord.lastModifyUser
            val channelCode = pipelineInfoRecord.channel.let { ChannelCode.valueOf(it) }
            try {
                pipelineRepositoryService.deployPipeline(
                    model = pipelineModel,
                    projectId = pipelineProjectId,
                    signPipelineId = pipelineId,
                    userId = creator,
                    channelCode = channelCode,
                    create = false
                )
                pipelineAtomReplaceHistoryDao.createAtomReplaceHistory(
                    dslContext = dslContext,
                    projectId = pipelineProjectId,
                    busId = pipelineId,
                    busType = "PIPELINE",
                    sourceVersion = pipelineInfoRecord.version,
                    targetVersion = pipelineInfoRecord.version + 1,
                    status = TaskStatusEnum.SUCCESS.name,
                    baseId = baseId,
                    userId = userId
                )
            } catch (t: Throwable) {
                logger.warn("refresh pipeline model failed", t)
                pipelineAtomReplaceHistoryDao.createAtomReplaceHistory(
                    dslContext = dslContext,
                    projectId = pipelineProjectId,
                    busId = pipelineId,
                    busType = "PIPELINE",
                    sourceVersion = pipelineInfoRecord.version,
                    targetVersion = pipelineInfoRecord.version + 1,
                    status = TaskStatusEnum.FAIL.name,
                    baseId = baseId,
                    userId = userId,
                    log = t.message?.substring(0, 127)
                )
            }
        }
        return false
    }

    private fun generateAtomDataMap(
        toAtomInputParamMap: MutableMap<String, Any>,
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
        toAtomInputParamNameList: List<String>?,
        fromAtomInputParamMap: Map<String, Any>?,
        paramReplaceInfoList: List<AtomParamReplaceInfo>?
    ): MutableMap<String, Any> {
        val toAtomInputParamMap = mutableMapOf<String, Any>()
        toAtomInputParamNameList?.forEach { inputParamName ->
            // 如果参数名一样则从被替换插件取值
            if (fromAtomInputParamMap != null) {
                val fromAtomInputParamValue = fromAtomInputParamMap[inputParamName]
                if (fromAtomInputParamValue != null) {
                    toAtomInputParamMap[inputParamName] = fromAtomInputParamValue
                }
            }
            // 如果有插件参数替换映射信息则根据映射关系替换
            run handleParamReplace@{
                paramReplaceInfoList?.forEach { paramReplaceInfo ->
                    val toParamName = paramReplaceInfo.toParamName
                    if (inputParamName == toParamName) {
                        val inputParamValue = if (paramReplaceInfo.toParamValue != null) {
                            paramReplaceInfo.toParamValue
                        } else {
                            fromAtomInputParamMap?.get(paramReplaceInfo.fromParamName)
                        }
                        if (inputParamValue != null) {
                            toAtomInputParamMap[inputParamName] = inputParamValue
                        }
                        return@handleParamReplace
                    }
                }
            }
        }
        return toAtomInputParamMap
    }

    private fun generateMarketBuildLessAtomElement(
        toAtomInfo: PipelineAtom,
        element: Element,
        dataMap: MutableMap<String, Any>
    ): MarketBuildLessAtomElement {
        val marketBuildLessAtomElement = MarketBuildLessAtomElement(
            name = toAtomInfo.name,
            id = element.id,
            status = element.status,
            atomCode = toAtomInfo.atomCode,
            version = toAtomInfo.version,
            data = dataMap
        )
        marketBuildLessAtomElement.executeCount = element.executeCount
        marketBuildLessAtomElement.canRetry = element.canRetry
        marketBuildLessAtomElement.elapsed = element.elapsed
        marketBuildLessAtomElement.startEpoch = element.startEpoch
        marketBuildLessAtomElement.templateModify = element.templateModify
        marketBuildLessAtomElement.additionalOptions = element.additionalOptions
        marketBuildLessAtomElement.errorType = element.errorType
        marketBuildLessAtomElement.errorCode = element.errorCode
        marketBuildLessAtomElement.errorMsg = element.errorMsg
        return marketBuildLessAtomElement
    }

    private fun generateMarketBuildAtomElement(
        toAtomInfo: PipelineAtom,
        element: Element,
        dataMap: MutableMap<String, Any>
    ): MarketBuildAtomElement {
        val marketBuildAtomElement = MarketBuildAtomElement(
            name = toAtomInfo.name,
            id = element.id,
            status = element.status,
            atomCode = toAtomInfo.atomCode,
            version = toAtomInfo.version,
            data = dataMap
        )
        marketBuildAtomElement.executeCount = element.executeCount
        marketBuildAtomElement.canRetry = element.canRetry
        marketBuildAtomElement.elapsed = element.elapsed
        marketBuildAtomElement.startEpoch = element.startEpoch
        marketBuildAtomElement.templateModify = element.templateModify
        marketBuildAtomElement.additionalOptions = element.additionalOptions
        marketBuildAtomElement.errorType = element.errorType
        marketBuildAtomElement.errorCode = element.errorCode
        marketBuildAtomElement.errorMsg = element.errorMsg
        return marketBuildAtomElement
    }
}
