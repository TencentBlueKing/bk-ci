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

package com.tencent.devops.common.pipeline.enums

enum class VersionStatus(val statusName: String) {
    RELEASED("已发布版本"),
    COMMITTING("草稿版本"),
    BRANCH("分支版本"),
    BRANCH_RELEASE("通过分支版本发布（中间态）"),
    DRAFT_RELEASE("通过草稿版本发布（中间态）"),
    DELETE("被删除（无法查询）");

    fun fix(): VersionStatus {
        return if (this == BRANCH_RELEASE) {
            BRANCH
        } else if (this == DRAFT_RELEASE) {
            RELEASED
        } else {
            this
        }
    }

    fun isReleasing(): Boolean = this == RELEASED || this == BRANCH_RELEASE || this == DRAFT_RELEASE

    fun isNotReleased(): Boolean = this == COMMITTING || this == BRANCH
}

enum class BranchVersionAction(val statusName: String) {
    ACTIVE("活跃分支（可以被代码推送直接更新）"),
    INACTIVE("不活跃分支（已被发布或已被删除）"),
    CONFLICT("有冲突分支（落后于主干无法直接合入）");
}
