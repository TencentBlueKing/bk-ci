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

package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.pojo.enum.RunElementType

data class QualityIndicator(
    val hashId: String,
    val elementType: String,
    val elementDetail: String,
    val enName: String,
    val cnName: String,
    val stage: String,
    var operation: QualityOperation,
    val operationList: List<QualityOperation>,
    var threshold: String,
    var thresholdType: QualityDataType,
    val readOnly: Boolean,
    val type: String,
    val tag: String?,
    val metadataList: List<Metadata>,
    val desc: String?,
    val logPrompt: String,
    val enable: Boolean?,
    val range: String?,
    var taskName: String? = null
) {
    data class Metadata(
        val hashId: String,
        val name: String, // 中文名
        val enName: String // 英文名
    )

    companion object {
        val SCRIPT_ELEMENT = setOf(
            LinuxScriptElement.classType,
            WindowsScriptElement.classType,
            RunElementType.RUN.elementType
        )
    }

    fun isScriptElementIndicator(): Boolean {
        return elementType in SCRIPT_ELEMENT
    }

    fun clone(): QualityIndicator {
        return QualityIndicator(
            hashId = this.hashId,
            elementType = this.elementType,
            elementDetail = this.elementDetail,
            enName = this.enName,
            cnName = this.cnName,
            stage = this.stage,
            operation = this.operation,
            operationList = this.operationList,
            threshold = this.threshold,
            thresholdType = this.thresholdType,
            readOnly = this.readOnly,
            type = this.type,
            tag = this.tag,
            metadataList = this.metadataList,
            desc = this.desc,
            logPrompt = this.logPrompt,
            enable = this.enable,
            range = this.range
        )
    }
}
