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

package com.tencent.devops.store.pojo.template

import com.tencent.devops.store.pojo.common.category.Category
import com.tencent.devops.store.pojo.common.label.Label
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板详情")
data class OpTemplateItem(
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板代码", required = true)
    val templateCode: String,
    @get:Schema(title = "模板名称", required = true)
    val templateName: String,
    @get:Schema(title = "模板logo", required = false)
    val logoUrl: String?,
    @get:Schema(title = "所属模板分类Id", required = false)
    val classifyId: String?,
    @get:Schema(title = "所属模板分类Code", required = false)
    val classifyCode: String?,
    @get:Schema(title = "所属模板分类名称", required = false)
    val classifyName: String?,
    @get:Schema(title = "简介", required = false)
    val summary: String?,
    @get:Schema(title =
        "模板状态，INIT：初始化|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGED：已下架",
        required = true
    )
    val templateStatus: String,
    @get:Schema(title = "模板描述", required = false)
    val description: String?,
    @get:Schema(title = "版本号", required = false)
    val version: String?,
    @get:Schema(title = "模板类型，FREEDOM：自由模式 CONSTRAINT：约束模式", required = true)
    val templateType: String,
    @get:Schema(title = "范畴列表", required = false)
    val categoryList: List<Category>?,
    @get:Schema(title = "标签列表", required = false)
    val labelList: List<Label>?,
    @get:Schema(title = "是否为最新版本模板 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,
    @get:Schema(title = "发布者", required = false)
    val publisher: String?,
    @get:Schema(title = "发布者描述", required = false)
    val pubDescription: String?,
    @get:Schema(title = "创建人")
    val creator: String,
    @get:Schema(title = "修改人")
    val modifier: String,
    @get:Schema(title = "创建时间")
    val createTime: String,
    @get:Schema(title = "修改时间")
    val updateTime: String
)
