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

package com.tencent.devops.stream.pojo.enums

import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder

class StreamApiEnums

enum class StreamBranchesOrder(val value: String) {
    NAME("name"),
    UPDATE("update")
}

enum class StreamSortAscOrDesc(val value: String) {
    ASC("asc"),
    DESC("desc")
}

fun StreamSortAscOrDesc?.toGitCodeAscOrDesc(): GitCodeBranchesSort? {
    if (this == null) {
        return null
    }
    return when (this) {
        StreamSortAscOrDesc.ASC -> GitCodeBranchesSort.ASC
        StreamSortAscOrDesc.DESC -> GitCodeBranchesSort.DESC
    }
}

enum class StreamProjectsOrder(val value: String) {
    ID("id"),
    NAME("name"),
    PATH("path"),
    CREATED("created_at"),
    UPDATE("updated_at"),
    ACTIVITY("activity"),
    FULL_NAME("full_name"),
}

fun StreamProjectsOrder?.toGitCodeOrderBy(): GitCodeProjectsOrder? {
    if (this == null) {
        return null
    }
    return when (this) {
        StreamProjectsOrder.ID -> GitCodeProjectsOrder.ID
        StreamProjectsOrder.NAME -> GitCodeProjectsOrder.NAME
        StreamProjectsOrder.PATH -> GitCodeProjectsOrder.PATH
        StreamProjectsOrder.CREATED -> GitCodeProjectsOrder.CREATED
        StreamProjectsOrder.UPDATE -> GitCodeProjectsOrder.UPDATE
        StreamProjectsOrder.ACTIVITY -> GitCodeProjectsOrder.ACTIVITY
        StreamProjectsOrder.FULL_NAME -> GitCodeProjectsOrder.FULL_NAME
    }
}
