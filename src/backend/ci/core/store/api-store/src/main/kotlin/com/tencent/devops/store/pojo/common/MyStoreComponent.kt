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

package com.tencent.devops.store.pojo.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "研发商店-我的组件信息")
data class MyStoreComponent(
    @get:Schema(title = "store组件ID", required = true)
    val storeId: String,
    @get:Schema(title = "store组件代码", required = true)
    val storeCode: String,
    @get:Schema(title = "store组件类型", required = true)
    val storeType: String,
    @get:Schema(title = "store组件名称", required = true)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @get:Schema(title = "开发语言", required = false)
    val language: String? = null,
    @get:Schema(title = "logo链接", required = false)
    val logoUrl: String? = null,
    @get:Schema(title = "版本号", required = true)
    val version: String,
    @get:Schema(title = "状态", required = true)
    val status: String,
    @get:Schema(title = "项目名称", required = true)
    val projectName: String,
    @get:Schema(title = "是否有处于上架状态的组件版本", required = true)
    val releaseFlag: Boolean,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "修改人", required = true)
    val modifier: String,
    @get:Schema(title = "创建时间", required = true)
    val createTime: String,
    @get:Schema(title = "创建时间", required = true)
    val updateTime: String,
    @get:Schema(title = "处于流程中的组件版本信息", required = false)
    val processingVersionInfos: List<StoreBaseInfo>? = null
)
