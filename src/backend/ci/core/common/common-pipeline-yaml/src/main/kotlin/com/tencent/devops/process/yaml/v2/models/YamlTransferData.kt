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

package com.tencent.devops.process.yaml.v2.models

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException

/**
 * 从preYaml转向Yaml产生的内部使用而不暴露的中间数据
 * 如：Yaml对象的模板信息,用来分享凭证
 */
data class YamlTransferData(
    val templateData: TemplateData = TemplateData()
)

data class TemplateData(
    val templateId: String = "t-${UUIDUtil.generate()}",
    // Map<objectId,object>
    val transferDataMap: MutableMap<String, TransferTemplateData> = mutableMapOf()
)

data class TransferTemplateData(
    val objectId: String,
    val templateType: TemplateType,
    val remoteProjectId: String
)

fun YamlTransferData.add(objectId: String, templateType: TemplateType, remoteProjectId: String?) {
    if (remoteProjectId == null) {
        return
    }

    if (templateData.transferDataMap[objectId] != null) {
        throw YamlFormatException("step or job id $objectId is not unique")
    }

    templateData.transferDataMap[objectId] = TransferTemplateData(
        objectId = objectId,
        templateType = templateType,
        remoteProjectId = remoteProjectId
    )
}
