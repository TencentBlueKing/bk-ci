/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.pojo.common.publication

import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "组件基本数据PO")
data class StoreBaseDataPO(
    @get:Schema(title = "主键ID")
    val id: String,
    @get:Schema(title = "组件标识")
    val storeCode: String,
    @get:Schema(title = "组件类型")
    val storeType: StoreTypeEnum,
    @get:Schema(title = "组件名称")
    val name: String,
    @get:Schema(title = "版本号")
    val version: String,
    @get:Schema(title = "状态")
    val status: StoreStatusEnum,
    @get:Schema(title = "状态描述")
    val statusMsg: String? = null,
    @get:Schema(title = "logo地址")
    val logoUrl: String? = null,
    @get:Schema(title = "简介", required = true)
    val summary: String = "",
    @get:Schema(title = "描述", required = false)
    val description: String? = null,
    @get:Schema(title = "是否为最新版本")
    val latestFlag: Boolean = false,
    @get:Schema(title = "发布者")
    val publisher: String = "",
    @get:Schema(title = "发布时间")
    val pubTime: LocalDateTime? = null,
    @get:Schema(title = "分类ID")
    val classifyId: String = "",
    @get:Schema(title = "业务序号")
    val busNum: Long? = null,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime = LocalDateTime.now(),
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime = LocalDateTime.now()
)
