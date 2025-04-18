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

package com.tencent.devops.store.pojo.common.publication

import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

data class StoreApproveReleaseRequest(
    @get:Schema(title = "是否为公共组件", required = true)
    val publicFlag: Boolean = false,
    @get:Schema(title = "是否推荐", required = true)
    val recommendFlag: Boolean = true,
    @get:Schema(title = "是否为官方认证", required = true)
    val certificationFlag: Boolean = false,
    @get:Schema(title = "是否在首页展示", required = true)
    val showFlag: Boolean = true,
    @get:Schema(title = "研发来源 SELF_DEVELOPED：自研 THIRD_PARTY：第三方", required = false)
    val rdType: RdTypeEnum? = null,
    @get:Schema(title = "权重（数值越大代表权重越高）", required = false)
    val weight: Int? = null,
    @get:Schema(title = "审核结果：PASS：通过|REJECT：驳回", required = true)
    val result: String = PASS,
    @get:Schema(title = "审核结果说明", required = true)
    val message: String = ""
)
