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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.JsonUtil.deepCopy
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.ElementBaseInfo
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.AtomPostInfo
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import com.tencent.devops.store.pojo.common.ATOM_POST_CONDITION
import com.tencent.devops.store.pojo.common.ATOM_POST_ENTRY_PARAM
import com.tencent.devops.store.pojo.common.ATOM_POST_FLAG
import com.tencent.devops.store.pojo.common.ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 流水线添加post-action回调插件的专用服务
 * @version 1.0
 */
@Suppress("ALL")
@Service
class PipelinePostElementService @Autowired constructor(
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelinePostElementService::class.java)
    }

    @Value("\${pipeline.atom.postPrompt:POST：}")
    private val postPrompt: String = "POST："

    fun handlePostElements(
        projectId: String,
        elementItemList: MutableList<ElementBaseInfo>,
        originalElementList: List<Element>,
        finalElementList: MutableList<Element>,
        startValues: Map<String, String>? = null,
        finallyStage: Boolean
    ): MutableList<Element> {
        logger.info("handlePostElements projectId:$projectId,elementItemList:$elementItemList")
        val allPostElements = mutableListOf<ElementPostInfo>()
        val noCacheAtomItems = mutableSetOf<AtomPostReqItem>()
        val noCacheElementMap = mutableMapOf<String, ElementBaseInfo>()
        elementItemList.forEach { elementItem ->
            val elementId = elementItem.elementId
            val atomCode = elementItem.atomCode
            val version = elementItem.version
            val atomVersionTestFlag = redisOperation.hget("$ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX:$atomCode", version)
            val atomPostInfo = redisOperation.hget("$ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX:$atomCode", version)
            val flag = version.contains("*") && (atomVersionTestFlag == null || atomVersionTestFlag.toBoolean())
            if (flag || atomPostInfo == null) {
                // 如果插件在redis中没有其版本对应普通项目的post标识或者当前插件大版本内有测试版本，则通过接口获取post标识
                val atomPostReqItem = AtomPostReqItem(atomCode, version)
                noCacheAtomItems.add(atomPostReqItem)
                noCacheElementMap[elementId] = elementItem
            } else {
                val atomPostInfoMap = JsonUtil.toMap(atomPostInfo)
                val postFlag = atomPostInfoMap[ATOM_POST_FLAG] as? Boolean
                if (postFlag == true) {
                    allPostElements.add(ElementPostInfo(
                        postEntryParam = atomPostInfoMap[ATOM_POST_ENTRY_PARAM] as String,
                        postCondition = atomPostInfoMap[ATOM_POST_CONDITION] as String,
                        parentElementId = elementId,
                        parentElementName = elementItem.elementName,
                        parentElementJobIndex = elementItem.elementJobIndex
                    ))
                }
            }
        }
        if (noCacheAtomItems.isNotEmpty()) {
            handleNoCachePostElement(
                projectId = projectId,
                noCacheAtomItems = noCacheAtomItems,
                noCacheElementMap = noCacheElementMap,
                allPostElements = allPostElements
            )
        }
        // 将post操作的element倒序排序以满足业务需要
        allPostElements.sortByDescending { it.parentElementJobIndex }
        allPostElements.forEach { elementPostInfo ->
            addPostElement(
                elementPostInfo = elementPostInfo,
                originalElementList = originalElementList,
                startValues = startValues,
                finalElementList = finalElementList,
                finallyStage = finallyStage
            )
        }
        return finalElementList
    }

    private fun handleNoCachePostElement(
        projectId: String,
        noCacheAtomItems: MutableSet<AtomPostReqItem>,
        noCacheElementMap: MutableMap<String, ElementBaseInfo>,
        allPostElements: MutableList<ElementPostInfo>
    ) {
        val getPostAtomsResult =
            client.get(ServiceMarketAtomResource::class).getPostAtoms(projectId, noCacheAtomItems)
        if (getPostAtomsResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = getPostAtomsResult.status.toString(),
                defaultMessage = getPostAtomsResult.message
            )
        }
        val atomPostResp = getPostAtomsResult.data
        val atomPostAtoms = atomPostResp?.postAtoms
        if (atomPostAtoms != null && atomPostAtoms.isNotEmpty()) {
            addNoCachePostElement(noCacheElementMap, atomPostAtoms, allPostElements)
        }
    }

    private fun addNoCachePostElement(
        noCacheElementMap: MutableMap<String, ElementBaseInfo>,
        atomPostAtoms: List<AtomPostInfo>,
        allPostElements: MutableList<ElementPostInfo>
    ) {
        noCacheElementMap.forEach { (elementId, elementItem) ->
            atomPostAtoms.forEach { atomPostInfo ->
                if (elementItem.atomCode == atomPostInfo.atomCode && elementItem.version == atomPostInfo.version) {
                    // 把redis中未缓存的带post操作的element加入集合
                    allPostElements.add(
                        ElementPostInfo(
                            postEntryParam = atomPostInfo.postEntryParam,
                            postCondition = atomPostInfo.postCondition,
                            parentElementId = elementId,
                            parentElementName = elementItem.elementName,
                            parentElementJobIndex = elementItem.elementJobIndex
                        )
                    )
                }
            }
        }
    }

    private fun addPostElement(
        elementPostInfo: ElementPostInfo,
        originalElementList: List<Element>,
        startValues: Map<String, String>?,
        finalElementList: MutableList<Element>,
        finallyStage: Boolean
    ) {
        val originAtomElement = originalElementList[elementPostInfo.parentElementJobIndex]
        var originElementId = originAtomElement.id
        var elementStatus: String? = null
        if (originElementId == null) {
            originElementId = modelTaskIdGenerator.getNextId()
            originAtomElement.id = originElementId
        } else {
            // 若post插件的父类插件id不为空，可能会通过变量的方式来进行对父插件的跳过，需要对父插件启用状态设置为false
            if (startValues != null) {
                originAtomElement.disableBySkipVar(variables = startValues)
            }
        }
        val status = originAtomElement.initStatus(rerun = finallyStage)
        // 如果原插件执行时选择跳过，那么插件的post操作也要跳過
        if (status == BuildStatus.SKIP) {
            elementStatus = BuildStatus.SKIP.name
        }
        val elementName =
            if (originAtomElement.name.length > 128) originAtomElement.name.substring(0, 128)
            else originAtomElement.name
        val postCondition = elementPostInfo.postCondition
        val postAtomRunCondition = getPostAtomRunCondition(postCondition)
        val additionalOptions = originAtomElement.additionalOptions?.deepCopy<ElementAdditionalOptions>()
        additionalOptions?.let {
            additionalOptions.enable = true
            additionalOptions.continueWhenFailed = true
            additionalOptions.retryWhenFailed = false
            additionalOptions.runCondition = postAtomRunCondition
            additionalOptions.pauseBeforeExec = null
            additionalOptions.subscriptionPauseUser = null
            additionalOptions.retryCount = 0
            additionalOptions.otherTask = null
            additionalOptions.customCondition = null
            additionalOptions.elementPostInfo = elementPostInfo
        }
        // 生成post操作的element
        val postElementName = getPostElementName(elementName)
        if (originAtomElement is MarketBuildAtomElement) {
            val marketBuildAtomElement = MarketBuildAtomElement(
                name = postElementName,
                id = modelTaskIdGenerator.getNextId(),
                status = elementStatus,
                atomCode = originAtomElement.getAtomCode(),
                version = originAtomElement.version,
                data = originAtomElement.data
            )
            marketBuildAtomElement.additionalOptions = additionalOptions
            finalElementList.add(marketBuildAtomElement)
        } else if (originAtomElement is MarketBuildLessAtomElement) {
            val marketBuildLessAtomElement = MarketBuildLessAtomElement(
                name = postElementName,
                id = modelTaskIdGenerator.getNextId(),
                status = elementStatus,
                atomCode = originAtomElement.getAtomCode(),
                version = originAtomElement.version,
                data = originAtomElement.data
            )
            marketBuildLessAtomElement.additionalOptions = additionalOptions
            finalElementList.add(marketBuildLessAtomElement)
        }
    }

    fun getPostElementName(elementName: String): String {
        return "$postPrompt$elementName"
    }

    fun getPostAtomRunCondition(postCondition: String): RunCondition {
        var postAtomRunCondition = RunCondition.PRE_TASK_SUCCESS
        when (postCondition) {
            "failed()" -> {
                postAtomRunCondition = RunCondition.PRE_TASK_FAILED_ONLY
            }

            "always()" -> {
                postAtomRunCondition = RunCondition.PRE_TASK_FAILED_BUT_CANCEL
            }

            "canceledOrTimeOut()" -> {
                postAtomRunCondition = RunCondition.PARENT_TASK_CANCELED_OR_TIMEOUT
            }
        }
        return postAtomRunCondition
    }
}
