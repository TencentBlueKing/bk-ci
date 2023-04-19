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

package com.tencent.devops.monitoring.constant

enum class SlaPluginError(
    private val code: String,
    private val mean: String
) {
    DEFAULT_ERROR("2199001", "插件默认异常"),
    CONFIG_ERROR("2199002", "用户配置有误"),
    DEPEND_ERROR("2199003", "插件依赖异常"),
    EXEC_FAILED("2199004", "用户任务执行失败"),
    TIMEOUT("2199005", "用户任务执行超时失败（自行限制）"),
    GITCI_ERROR("2199006", "工蜂服务异常"),
    LOW_QUALITY("2199007", "触碰质量红线"),
    ;

    companion object {
        fun getMean(code: String?): String {
            values().forEach {
                if (code == it.code) {
                    return it.mean
                }
            }
            return DEFAULT_ERROR.mean
        }
    }
}
