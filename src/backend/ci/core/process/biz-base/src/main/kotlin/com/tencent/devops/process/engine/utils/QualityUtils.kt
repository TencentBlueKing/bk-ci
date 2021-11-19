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

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import org.slf4j.LoggerFactory

@Suppress("ALL")
object QualityUtils {

    private val logger = LoggerFactory.getLogger(QualityUtils::class.java)

    fun getInsertElement(
        element: Element,
        elementRuleMap: Map<String, List<Map<String, Any>>>,
        isBefore: Boolean
    ): Element? {
        val position = if (isBefore) "BEFORE" else "AFTER"

        // 取出所有规则的gatewayIds
        val gatewayIds = mutableSetOf<String>()
        val elementList = elementRuleMap[element.getAtomCode()]?.filter { it["position"] as String == position }
        elementList?.forEach {
            // 处理包含某些rule没填gateway id的情况
            val itemGatewayIds = it.getValue("gatewayIds") as List<String>
            if (itemGatewayIds.isEmpty()) gatewayIds.add(("")) else gatewayIds.addAll(itemGatewayIds)
        }
        logger.info("elementName: ${element.name}, gatewayIds: $gatewayIds")
        return if (gatewayIds.isEmpty() || gatewayIds.any { element.name.toLowerCase().contains(it.toLowerCase()) }) {
            val id = "T-${UUIDUtil.generate()}"
            if (isBefore) {
                QualityGateInElement("质量红线(准入)", id, null, element.getAtomCode(), element.name)
            } else {
                QualityGateOutElement("质量红线(准出)", id, null, element.getAtomCode(), element.name)
            }
        } else {
            null
        }
    }
}
