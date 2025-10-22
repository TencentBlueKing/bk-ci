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

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@Schema(title = "镜像详情")
data class ImageRepoInfo(

    @get:Schema(title = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val sourceType: ImageType,

    @get:Schema(title = "镜像仓库地址", required = true)
    val repoUrl: String,

    @get:Schema(title = "镜像在仓库中的路径", required = true)
    val repoName: String,

    @get:Schema(title = "镜像Tag", required = true)
    val repoTag: String,

    @get:Schema(title = "凭证Id", required = true)
    val ticketId: String,

    @get:Schema(title = "初始化凭证的项目", required = true)
    val ticketProject: String,

    @get:Schema(title = "是否为公共镜像 true：是 false：否", required = true)
    val publicFlag: Boolean,

    @get:Schema(title = "研发来源")
    val rdType: ImageRDTypeEnum
)
