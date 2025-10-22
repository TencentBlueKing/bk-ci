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
package com.tencent.devops.store.pojo.image.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@Schema(title = "镜像范畴")
data class Category(

    @get:Schema(title = "范畴ID", required = true)
    val id: String,

    @get:Schema(title = "范畴代码", required = true)
    val categoryCode: String,

    @get:Schema(title = "范畴名称", required = true)
    val categoryName: String,

    @get:Schema(title = "类别 ATOM:插件,TEMPLATE:模板,IMAGE:镜像", required = true)
    val categoryType: String,

    @get:Schema(title = "范畴图标链接", required = true)
    val iconUrl: String,

    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,

    @get:Schema(title = "修改时间", required = true)
    val updateTime: Long

)
