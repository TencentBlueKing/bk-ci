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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.enums

import com.tencent.devops.common.api.exception.InvalidParamException

enum class AgentStatus(val status: Int) {
    UN_IMPORT(0), // 未导入，用户刚刚在界面上面生成链接
    UN_IMPORT_OK(1), // 未导入但是agent状态正常（这个时候还是不能用来当构建机）
    IMPORT_OK(2), // 用户已经在界面导入并且agent工作正常（构建机只有在这个状态才能正常工作）
    IMPORT_EXCEPTION(3), // agent异常
    DELETE(4);

    override fun toString() = status.toString()

    companion object {
        fun fromStatus(status: Int): AgentStatus {
            values().forEach {
                if (status == it.status) {
                    return it
                }
            }
            throw InvalidParamException("Unknown agent status($status)")
        }

        fun isDelete(status: AgentStatus) =
            status == DELETE

        fun isUnImport(status: AgentStatus) = status == UN_IMPORT

        fun isImportException(status: AgentStatus) = status == IMPORT_EXCEPTION
    }
}