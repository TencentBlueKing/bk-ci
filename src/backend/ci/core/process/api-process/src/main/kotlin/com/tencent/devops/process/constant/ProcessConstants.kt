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

package com.tencent.devops.process.constant

/**
 * 流水线常量
 */
object ProcessConstants {

    /**
     * 动态版本号（-1）：引用信息中表示"引用最新版本"
     */
    const val DYNAMIC_VERSION = -1

    /**
     * 公共变量组名称校验规则：以英文字母开头，由字母、数字、下划线组成，长度 3-32 字符。
     * 对外文案 [ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_NAME_FORMAT] 必须与本规则保持一致。
     */
    const val PUBLIC_VAR_GROUP_NAME_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$"

    /**
     * 公共变量名校验规则：以字母或下划线开头，由字母、数字、下划线组成，长度不超过 64 字符。
     */
    const val PUBLIC_VAR_NAME_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_]{0,63}$"

    // 公共变量组锁相关常量
    const val PUBLIC_VAR_GROUP_ADD_LOCK_KEY = "PUBLIC_VAR_GROUP_ADD_LOCK"
    const val PUBLIC_VAR_GROUP_DELETE_LOCK_KEY = "PUBLIC_VAR_GROUP_DELETE_LOCK"
    // 引用计数锁：以项目ID和变量组名为粒度，格式为 PUBLIC_VAR_GROUP_REFER_LOCK:${projectId}:${groupName}
    const val PUBLIC_VAR_GROUP_REFER_LOCK_KEY_PREFIX = "PUBLIC_VAR_GROUP_REFER_LOCK"
    const val PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS = 10L
}
