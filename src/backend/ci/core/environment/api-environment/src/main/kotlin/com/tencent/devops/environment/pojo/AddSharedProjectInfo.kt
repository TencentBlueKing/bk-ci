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

package com.tencent.devops.environment.pojo

import com.tencent.devops.environment.pojo.enums.SharedEnvType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "VM虚拟机配额")
data class AddSharedProjectInfo(
    @Deprecated("普通项目也支持 , 请使用projectId")
    @get:Schema(title = "工蜂项目ID", required = false)
    val gitProjectId: String? = null,
    @get:Schema(title = "项目名称，工蜂项目则为groupName/projectName", required = true)
    val name: String,
    @get:Schema(title = "类型，预留", required = true)
    val type: SharedEnvType,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String? = null
) {
    @SuppressWarnings("TooGenericExceptionThrown")
    fun getFinalProjectId(): String {
        return projectId ?: gitProjectId ?: throw RuntimeException("Project id must not null.")
    }
}
