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
@Schema(title = "我的镜像")
data class MyImage(

    @get:Schema(title = "镜像Id", required = true)
    val imageId: String,

    @get:Schema(title = "镜像代码", required = true)
    val imageCode: String,

    @get:Schema(title = "镜像名称", required = true)
    val imageName: String,

    @get:Schema(title = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: String,

    @get:Schema(title = "镜像仓库URL", required = false)
    val imageRepoUrl: String,

    @get:Schema(title = "镜像在仓库中的名称", required = false)
    val imageRepoName: String,

    @get:Schema(title = "版本号", required = true)
    val version: String,

    @get:Schema(title = "镜像tag", required = true)
    val imageTag: String,

    @get:Schema(title = "镜像大小（MB字符串）", required = true)
    val imageSize: String,

    @get:Schema(title = "镜像大小数值（字节）", required = true)
    val imageSizeNum: Long,

    @get:Schema(title =
        "镜像状态，INIT：初始化|COMMITTING：提交中|CHECKING：验证中|CHECK_FAIL：验证失败|TESTING：测试中|" +
            "AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|" +
            "UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val imageStatus: String,

    @get:Schema(title = "所属项目代码", required = true)
    val projectCode: String,

    @get:Schema(title = "所属项目名称", required = true)
    val projectName: String,

    @get:Schema(title = "项目是否被禁用", required = true)
    val projectEnabled: Boolean,

    @get:Schema(title = "创建人", required = true)
    val creator: String,

    @get:Schema(title = "修改人", required = true)
    val modifier: String,

    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,

    @get:Schema(title = "修改时间", required = true)
    val updateTime: Long,

    @get:Schema(title = "是否为公共镜像", required = true)
    val publicFlag: Boolean
)
