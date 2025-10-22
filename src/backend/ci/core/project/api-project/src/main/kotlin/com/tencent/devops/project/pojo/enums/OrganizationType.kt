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

package com.tencent.devops.project.pojo.enums

enum class OrganizationType(val typeId: Int) {
    bg(6),
    businessLine(8),
    dept(1),
    center(7),
    group(2);

    companion object {
        fun getOrganizationTypeName(typeId: Int): String {
            values().forEach {
                if (typeId == it.typeId) return it.name
            }
            return typeId.toString()
        }

        // 是否为部门以下层级
        fun isBelowTheDept(typeId: Int): Boolean {
            return typeId == center.typeId || typeId == group.typeId
        }

        fun isBusinessLine(typeId: Int): Boolean {
            return typeId == businessLine.typeId
        }

        fun isDept(typeId: Int): Boolean {
            return typeId == dept.typeId
        }

        fun isCenter(typeId: Int): Boolean {
            return typeId == center.typeId
        }

        fun isGroup(typeId: Int): Boolean {
            return typeId == group.typeId
        }
    }
}
