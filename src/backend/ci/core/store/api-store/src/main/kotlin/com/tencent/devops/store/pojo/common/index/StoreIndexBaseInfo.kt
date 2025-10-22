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

package com.tencent.devops.store.pojo.common.index

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.IndexOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "研发商店指标基本信息")
data class StoreIndexBaseInfo(
    @get:Schema(title = "ID", required = true)
    val id: String,
    @get:Schema(title = "指标代码", required = true)
    @BkField(maxLength = 10, patternStyle = BkStyleEnum.CODE_STYLE)
    val indexCode: String,
    @get:Schema(title = "指标名称", required = true)
    @BkField(maxLength = 64)
    val indexName: String,
    @get:Schema(title = "指标描述", required = true)
    @BkField(maxLength = 256)
    val description: String,
    @get:Schema(title = "运算类型", required = true)
    val operationType: IndexOperationTypeEnum,
    @get:Schema(title = "指标对应的插件代码")
    val atomCode: String? = null,
    @get:Schema(title = "插件执行版本号")
    val atomVersion: String? = null,
    @get:Schema(title = "完成执行任务数量", required = false)
    val finishTaskNum: Int? = null,
    @get:Schema(title = "执行任务总数", required = false)
    val totalTaskNum: Int? = null,
    @get:Schema(title = "指标执行时间类型", required = true)
    val executeTimeType: IndexExecuteTimeTypeEnum,
    @get:Schema(title = "组件类型", required = true)
    val storeType: StoreTypeEnum,
    @get:Schema(title = "指标展示权重", required = true)
    val weight: Int,
    @get:Schema(title = "创建者", required = true)
    val creator: String,
    @get:Schema(title = "修改者", required = true)
    val modifier: String,
    @get:Schema(title = "更新时间", required = true)
    val updateTime: LocalDateTime,
    @get:Schema(title = "创建时间", required = true)
    val createTime: LocalDateTime
)
