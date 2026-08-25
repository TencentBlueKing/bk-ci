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

import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工作台模板列表项")
data class MyTemplateItemResponse(
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板代码", required = true)
    val templateCode: String,
    @get:Schema(title = "模板名称", required = true)
    val templateName: String,
    @get:Schema(title = "模板logo", required = false)
    val logoUrl: String?,
    @get:Schema(title = "描述", required = false)
    val description: String?,
    @get:Schema(title = "是否开启PAC", required = false)
    val enablePac: Boolean? = false,
    @get:Schema(title = "模板最新上架版本名称", required = true)
    val latestPublishedVersionName: String? = null,
    @get:Schema(title = "模板最新上架版本", required = true)
    val latestPublishedVersion: Long? = null,
    @get:Schema(title = "模板最新发布版本", required = true)
    val latestReleasedVersion: Long? = null,
    @get:Schema(
        title =
        "模板状态，INIT：初始化|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGED：已下架",
        required = true
    )
    val templateStatus: TemplateStatusEnum,
    @get:Schema(title = "是否需要升级", required = true)
    val upgradeFlag: Boolean,
    @get:Schema(title = "模板所属项目代码", required = true)
    val projectCode: String,
    @get:Schema(title = "模板所属项目名称", required = true)
    val projectName: String,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "修改人", required = true)
    val modifier: String,
    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,
    @get:Schema(title = "修改时间", required = true)
    val updateTime: Long
)
