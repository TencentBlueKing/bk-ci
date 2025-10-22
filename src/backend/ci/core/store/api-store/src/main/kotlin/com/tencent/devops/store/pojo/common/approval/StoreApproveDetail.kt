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

package com.tencent.devops.store.pojo.common.approval

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "审核信息详情")
data class StoreApproveDetail(
    @get:Schema(title = "ID", required = true)
    val approveId: String,
    @get:Schema(title = "审批内容", required = true)
    val content: String,
    @get:Schema(title = "申请人", required = true)
    val applicant: String,
    @get:Schema(title = "审批类型 ATOM_COLLABORATOR_APPLY:申请成为插件协作者", required = true)
    val approveType: String,
    @get:Schema(title = "审批状态 WAIT:待审批，PASS:通过，REFUSE:拒绝", required = true)
    val approveStatus: String,
    @get:Schema(title = "审批信息", required = false)
    val approveMsg: String?,
    @get:Schema(title = "store组件代码", required = true)
    val storeCode: String,
    @get:Schema(title = "store组件类别 ATOM:插件 TEMPLATE:模板 IMAGE:镜像 IDE_ATOM:IDE插件", required = true)
    val storeType: String,
    @get:Schema(title = "审批业务的额外参数", required = false)
    var additionalParams: Map<String, String>? = null,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "修改人", required = true)
    val modifier: String,
    @get:Schema(title = "创建日期", required = true)
    val createTime: Long = 0,
    @get:Schema(title = "更新日期", required = true)
    val updateTime: Long = 0
)
