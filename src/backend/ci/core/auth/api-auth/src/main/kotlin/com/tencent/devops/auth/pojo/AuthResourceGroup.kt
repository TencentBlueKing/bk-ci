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

package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "资源用户组信息")
data class AuthResourceGroup(
    val id: Long? = null,
    @get:Schema(title = "项目ID", required = true)
    val projectCode: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "资源ID", required = true)
    val resourceCode: String,
    @get:Schema(title = "资源名", required = true)
    val resourceName: String,
    @get:Schema(title = "IAM资源ID", required = true)
    val iamResourceCode: String,
    @get:Schema(title = "组编码, @See DefaultGroupType", required = true)
    val groupCode: String,
    @get:Schema(title = "用户组名, @See DefaultGroupType", required = true)
    val groupName: String,
    @get:Schema(title = "是否是默认组", required = true)
    val defaultGroup: Boolean,
    @get:Schema(title = "IAM 用户组ID, @See DefaultGroupType", required = true)
    val relationId: Int,
    @get:Schema(title = "创建时间", required = false)
    val createTime: LocalDateTime? = null,
    @get:Schema(title = "更新时间", required = false)
    val updateTime: LocalDateTime? = null,
    @get:Schema(title = "用户组描述", required = false)
    val description: String? = null,
    @get:Schema(title = "IAM人员模板ID", required = false)
    val iamTemplateId: Int? = null
)
