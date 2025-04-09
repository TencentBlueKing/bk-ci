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

package com.tencent.devops.store.pojo.common

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvRequest
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureRequest
import com.tencent.devops.store.pojo.common.version.VersionModel
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(title = "组件发布部署基础模型")
data class StoreReleaseBaseInfo(
    @get:Schema(title = "store组件类型", required = true)
    val storeType: StoreTypeEnum,
    @get:Schema(title = "组件名称", required = false)
    @field:BkField(patternStyle = BkStyleEnum.NAME_STYLE)
    var name: String? = null,
    @get:Schema(title = "组件logo地址", required = false)
    @field:BkField(maxLength = 1024)
    var logoUrl: String? = null,
    @get:Schema(title = "所属组件分类代码", required = false)
    val classifyCode: String? = null,
    @get:Schema(title = "组件简介", required = false)
    @field:BkField(maxLength = 256)
    val summary: String? = null,
    @get:Schema(title = "组件描述", required = false)
    @field:BkField(maxLength = 65535)
    var description: String? = null,
    @get:Schema(title = "版本信息", required = true)
    val versionInfo: VersionModel,
    @get:Schema(title = "标签标识列表", required = false)
    val labelCodeList: List<String>? = null,
    @get:Schema(title = "组件所属范畴", required = false)
    val categoryCodeList: List<String>? = null,
    @get:Schema(title = "基础扩展信息", required = false)
    val extBaseInfo: MutableMap<String, Any>? = null,
    @get:Schema(title = "特性信息", required = false)
    @Valid
    val baseFeatureInfo: StoreBaseFeatureRequest? = null,
    @get:Schema(title = "环境信息列表", required = false)
    @Valid
    val baseEnvInfos: MutableList<StoreBaseEnvRequest>? = null
)
