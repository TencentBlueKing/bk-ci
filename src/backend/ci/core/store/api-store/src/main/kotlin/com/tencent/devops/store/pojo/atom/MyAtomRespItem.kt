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

package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "流水线-插件信息")
data class MyAtomRespItem(
    @Schema(name = "插件ID", required = true)
    val atomId: String,
    @Schema(name = "插件名称", required = true)
    @BkFieldI18n(source = I18nSourceEnum.DB)
    val name: String,
    @Schema(name = "插件代码", required = true)
    val atomCode: String,
    @Schema(name = "开发语言", required = true)
    val language: String?,
    @Schema(name = "插件所属范畴，TRIGGER：触发器类插件 TASK：任务类插件", required = true)
    val category: String,
    @Schema(name = "logo链接")
    val logoUrl: String?,
    @Schema(name = "版本号", required = true)
    val version: String,
    @Schema(name =
        "插件状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|" +
            "AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|" +
            "UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val atomStatus: String,
    @Schema(name = "项目", required = true)
    val projectName: String,
    @Schema(name = "是否有处于上架状态的插件插件版本", required = true)
    val releaseFlag: Boolean,
    @Schema(name = "创建人", required = true)
    val creator: String,
    @Schema(name = "修改人", required = true)
    val modifier: String,
    @Schema(name = "创建时间", required = true)
    val createTime: String,
    @Schema(name = "创建时间", required = true)
    val updateTime: String,
    @Schema(name = "处于流程中的插件版本信息", required = false)
    val processingVersionInfos: List<AtomBaseInfo>? = null
)
