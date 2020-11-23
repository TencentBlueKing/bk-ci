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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线element相关的服务
 * @version 1.0
 */
@Service
class PipelineElementService @Autowired constructor(
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineElementService::class.java)
    }

    fun handlePostElements(
        projectId: String,
        atomItems: MutableList<AtomPostReqItem>,
        atomIndexMap: MutableMap<String, Int>,
        originalElementList: List<Element>,
        finalElementList: MutableList<Element>
    ): MutableList<Element> {
        logger.info("handlePostElements projectId:$projectId,atomItems:$atomItems,atomIndexMap:$atomIndexMap")
        val getPostAtomsResult =
            client.get(ServiceMarketAtomResource::class).getPostAtoms(projectId, atomItems)
        if (getPostAtomsResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = getPostAtomsResult.status.toString(),
                defaultMessage = getPostAtomsResult.message
            )
        }
        val atomPostResp = getPostAtomsResult.data
        if (atomPostResp != null) {
            val postAtoms = atomPostResp.postAtoms
            postAtoms?.forEach { postAtom ->
                val postAtomCode = postAtom.atomCode
                val postAtomIndex = atomIndexMap[postAtomCode]!!
                val originAtomElement = originalElementList[postAtomIndex]
                var originElementId = originAtomElement.id
                if (originElementId == null) {
                    originElementId = modelTaskIdGenerator.getNextId()
                    originAtomElement.id = originElementId
                }
                val elementName =
                    if (originAtomElement.name.length > 122) originAtomElement.name.substring(0, 122)
                    else originAtomElement.name
                val postCondition = postAtom.postCondition
                var postAtomRunCondition = RunCondition.PRE_TASK_SUCCESS
                if (postCondition == "failed()") {
                    postAtomRunCondition = RunCondition.PRE_TASK_FAILED_ONLY
                } else if (postCondition == "always()") {
                    postAtomRunCondition = RunCondition.ALWAYS
                }
                val additionalOptions = ElementAdditionalOptions(
                    enable = true,
                    continueWhenFailed = true,
                    retryWhenFailed = false,
                    runCondition = postAtomRunCondition,
                    customVariables = originAtomElement.additionalOptions?.customVariables,
                    retryCount = 0,
                    timeout = 100,
                    otherTask = null,
                    customCondition = null,
                    elementPostInfo = ElementPostInfo(postAtom.postEntryParam, originElementId)
                )
                if (originAtomElement is MarketBuildAtomElement) {
                    val marketBuildAtomElement = MarketBuildAtomElement(
                        name = "【POST】$elementName",
                        id = modelTaskIdGenerator.getNextId(),
                        atomCode = originAtomElement.getAtomCode(),
                        version = originAtomElement.version,
                        data = originAtomElement.data
                    )
                    marketBuildAtomElement.additionalOptions = additionalOptions
                    finalElementList.add(marketBuildAtomElement)
                } else if (originAtomElement is MarketBuildLessAtomElement) {
                    val marketBuildLessAtomElement = MarketBuildLessAtomElement(
                        name = "【POST】$elementName",
                        id = modelTaskIdGenerator.getNextId(),
                        atomCode = originAtomElement.getAtomCode(),
                        version = originAtomElement.version,
                        data = originAtomElement.data
                    )
                    marketBuildLessAtomElement.additionalOptions = additionalOptions
                    finalElementList.add(marketBuildLessAtomElement)
                }
            }
        }
        return finalElementList
    }
}
