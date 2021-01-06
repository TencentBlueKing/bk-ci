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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineAtomReplaceBaseDao
import com.tencent.devops.process.dao.PipelineAtomReplaceItemDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.pojo.atom.AtomParamReplaceInfo
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PipelineAtomReplaceCronService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineAtomReplaceBaseDao: PipelineAtomReplaceBaseDao,
    private val pipelineAtomReplaceItemDao: PipelineAtomReplaceItemDao,
    private val pipelineResDao: PipelineResDao,
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAtomReplaceCronService::class.java)
        private const val LOCK_KEY = "pipelineAtomReplace"
        private const val PAGE_SIZE = 10
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    fun pipelineAtomReplace() {
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
                page = 1,
                pageSize = 1
            )
            atomReplaceBaseRecords?.forEach { atomReplaceBaseRecord ->
                val baseId = atomReplaceBaseRecord.id
                val atomReplaceItemCount = pipelineAtomReplaceItemDao.getAtomReplaceItemCountByBaseId(dslContext, baseId)
                if (atomReplaceItemCount < 1) {
                    return@forEach
                }
                val pipelineIdInfo = atomReplaceBaseRecord.pipelineIdInfo
                val projectId = atomReplaceBaseRecord.projectId
                val fromAtomCode = atomReplaceBaseRecord.fromAtomCode
                val toAtomCode = atomReplaceBaseRecord.toAtomCode
                val pipelineIdSet = if (pipelineIdInfo.isNullOrBlank()) null else JsonUtil.to(pipelineIdInfo, object : TypeReference<Set<String>>() {})
                val totalPages = PageUtil.calTotalPage(PAGE_SIZE, atomReplaceItemCount)
                for (page in 1..totalPages) {
                    val atomReplaceItemList = pipelineAtomReplaceItemDao.getAtomReplaceItemListByBaseId(
                        dslContext = dslContext,
                        baseId = baseId,
                        descFlag = false,
                        page = page,
                        pageSize = PAGE_SIZE
                    )
                    atomReplaceItemList?.forEach  nextItem@{ atomReplaceItem ->
                        val toAtomVersion = atomReplaceItem.toAtomVersion
                        val toAtomInfo =
                            client.get(ServiceAtomResource::class).getAtomVersionInfo(toAtomCode, toAtomVersion).data
                                ?: return@nextItem
                        val fromAtomVersion = atomReplaceItem.fromAtomVersion
                        val paramReplaceInfoList = if (atomReplaceItem.paramReplaceInfo != null) JsonUtil.to(
                            atomReplaceItem.paramReplaceInfo,
                            object : TypeReference<List<AtomParamReplaceInfo>>() {}
                        ) else null
                        if (pipelineIdSet != null && pipelineIdSet.isNotEmpty()) {
                           // 查询需要替换插件的流水线集合
                           val pipelineModelList = pipelineResDao.listModelResource(dslContext, pipelineIdSet)
                           pipelineModelList.forEach { pipelineModelObj ->
                               val pipelineModel = JsonUtil.to(pipelineModelObj.model, Model::class.java)
                               pipelineModel.stages.forEach { stage ->
                                   stage.containers.forEach { container ->
                                       val finalElements = mutableListOf<Element>()
                                       container.elements.forEach nextElement@{ element ->
                                           if (element.getAtomCode() == fromAtomCode) {
                                               val toAtomPropMap = toAtomInfo.props
                                               val toAtomInputParamNameList = (toAtomPropMap?.get("input") as? Map<String, Any>)?.map { it.key }
                                               val toAtomInputParamMap = mutableMapOf<String, Any>()
                                               val fromAtomInputParamMap = when (element) {
                                                   is MarketBuildAtomElement -> {
                                                       element.data["input"] as? Map<String, Any>
                                                   }
                                                   is MarketBuildLessAtomElement -> {
                                                       element.data["input"] as? Map<String, Any>
                                                   }
                                                   else -> {
                                                       JsonUtil.toMap(element)
                                                   }
                                               }
                                               // 生成目标替换插件的输入参数
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
                                               // 判断生成的目标插件参数集合是否符合要求
                                               if (toAtomInputParamNameList?.size != toAtomInputParamMap.size) {
                                                   logger.warn("plugin: $fromAtomCode: $fromAtomVersion cannot be replaced by plugin: $toAtomCode: $toAtomVersion, parameter mapping error")
                                                   return@nextElement
                                               }
                                               val toAtomClassType = toAtomInfo.classType
                                               val dataMap = mutableMapOf<String, Any>(
                                                   "input" to toAtomInputParamMap
                                               )
                                               val outputParamMap = toAtomPropMap["output"]
                                               if (outputParamMap != null) {
                                                   dataMap["output"] = outputParamMap
                                               }
                                               if (toAtomClassType == JobTypeEnum.AGENT.name) {
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
                               // 更新流水线模型

                           }
                       }
                    }
                }
            }
        } catch (t: Throwable) {
            logger.warn("pipelineAtomReplace failed", t)
        } finally {
            lock.unlock()
        }
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
