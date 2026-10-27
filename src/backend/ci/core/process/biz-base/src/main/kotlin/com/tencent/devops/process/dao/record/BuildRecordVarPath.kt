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

package com.tencent.devops.process.dao.record

/**
 * 执行记录里 `*_VAR` JSON 列的路径拼装，供 task / container / stage 三张记录表共用。
 */
object BuildRecordVarPath {

    /**
     * 执行记录 JSON 里大变量引用串的键。插件、Job、Stage、流水线级共用这一个名字。
     *
     * 带 `bk` 前缀是因为这些 JSON 列同时承载 `Model` / `Element` 的字段，
     * 不加前缀的 `largeVariables` 将来可能和某个新增字段撞名。
     */
    const val BK_LARGE_VARIABLES = "bkLargeVariables"

    /**
     * 取某个大变量引用串的 MySQL JSON 路径，变量名无法表达成合法路径时返回 null。
     *
     * 变量名加引号，`.`、空格才不会被当成路径分隔；`"` 和 `\` 先转义。
     * 变量名来自接口入参，控制字符在带引号的 JSON 成员名里非法，MySQL 会直接报错而不是返回空，
     * 所以这里先挡掉——真实变量名不会含控制字符。
     */
    fun largeVariable(jsonKey: String, varName: String): String? {
        if (varName.isEmpty() || varName.any { it.isISOControl() }) return null
        val escaped = buildString(varName.length) {
            varName.forEach { ch ->
                if (ch == '\\' || ch == '"') append('\\')
                append(ch)
            }
        }
        return "\$.$jsonKey.\"$escaped\""
    }
}
