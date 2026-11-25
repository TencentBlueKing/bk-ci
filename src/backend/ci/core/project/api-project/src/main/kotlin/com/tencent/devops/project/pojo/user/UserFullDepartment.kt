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

package com.tencent.devops.project.pojo.user

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 用户完整部门链路上的一个组织节点。
 *
 * 顺序从祖先到当前组织。同一类型可以出现多次，例如小组下面再挂小组。
 * 落库 JSON 示例：
 * [{"id":"1","name":"CDG","typeId":"6","type":"bg","parentId":"0"},
 * {"id":"9","name":"平台组","typeId":"2","type":"group","parentId":"1"},
 * {"id":"10","name":"开发小组","typeId":"2","type":"group","parentId":"9"}]
 */
@Schema(title = "用户完整部门节点")
data class UserFullDepartment(
    @get:Schema(title = "部门ID")
    val id: String,
    @get:Schema(title = "部门名称")
    val name: String,
    @get:Schema(title = "TOF组织类型ID")
    val typeId: String,
    @get:Schema(title = "组织类型")
    val type: String,
    @get:Schema(title = "父部门ID")
    val parentId: String
) {
    companion object {
        private val LIST_TYPE = object : TypeReference<List<UserFullDepartment>>() {}

        fun toJson(departments: List<UserFullDepartment>?): String? {
            if (departments.isNullOrEmpty()) {
                return null
            }
            return JsonUtil.toJson(departments, false)
        }

        fun fromJson(json: String?): List<UserFullDepartment>? {
            if (json.isNullOrBlank()) {
                return null
            }
            return JsonUtil.toOrNull(json, LIST_TYPE)
        }
    }
}
