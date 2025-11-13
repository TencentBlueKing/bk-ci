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

package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 单向网络状态枚举
 */
@Schema(description = "单向网络状态")
enum class CdsMeshStatus(
    val value: Int,
    @get:Schema(title = "状态描述")
    val description: String
) {
    /** 未启用单向网络或在黑名单中 */
    DISABLED(0, "未启用单向网络"),
    
    /** Mesh单向网络模式 */
    MESH(1, "Mesh单向网络"),
    
    /** SSL单向网络模式 */
    SSL(2, "SSL单向网络");

    companion object {
        /**
         * 根据数值获取对应的状态
         */
        fun fromValue(value: Int): CdsMeshStatus {
            return values().find { it.value == value } ?: DISABLED
        }
    }
    
    /**
     * 是否为单向网络（Mesh 或 SSL）
     */
    fun isSingleNetwork(): Boolean = this == MESH || this == SSL
}

