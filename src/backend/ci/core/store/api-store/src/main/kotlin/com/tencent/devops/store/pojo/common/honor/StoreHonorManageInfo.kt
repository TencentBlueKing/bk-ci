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

package com.tencent.devops.store.pojo.common.honor

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "组件荣誉管理信息")
data class StoreHonorManageInfo(
    @get:Schema(title = "storeName", required = true)
    val storeName: String,
    @get:Schema(title = "storeCode", required = true)
    val storeCode: String,
    @get:Schema(title = "ID", required = true)
    val honorId: String,
    @get:Schema(title = "荣誉头衔", required = true)
    @BkField(maxLength = 4)
    val honorTitle: String,
    @get:Schema(title = "荣誉名称", required = true)
    @BkField(maxLength = 40)
    val honorName: String,
    @get:Schema(title = "组件范畴", required = true)
    val storeType: StoreTypeEnum,
    @get:Schema(title = "创建者", required = true)
    val creator: String,
    @get:Schema(title = "修改者", required = true)
    val modifier: String,
    @get:Schema(title = "更新时间", required = true)
    val updateTime: LocalDateTime,
    @get:Schema(title = "创建时间", required = true)
    val createTime: LocalDateTime
)
