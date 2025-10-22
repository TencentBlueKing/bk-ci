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

package com.tencent.devops.quality.util

import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import java.math.BigDecimal

object ThresholdOperationUtil {
    private const val DEFAULT = "null"

    fun getOperationName(operation: QualityOperation): String {
        return when (operation) {
            QualityOperation.LT -> "<"
            QualityOperation.LE -> "<="
            QualityOperation.EQ -> "="
            QualityOperation.GE -> ">="
            QualityOperation.GT -> ">"
        }
    }

    fun getOperationOppositeName(operation: QualityOperation): String {
        return when (operation) {
            QualityOperation.LT -> ">="
            QualityOperation.LE -> ">"
            QualityOperation.EQ -> "!="
            QualityOperation.GE -> "<"
            QualityOperation.GT -> "<="
        }
    }

    fun valid(actualValue: String?, boundaryValue: String, operation: QualityOperation): Boolean {
        if (actualValue == null) return false
        return when (operation) {
            QualityOperation.LT -> {
                actualValue != DEFAULT && actualValue.toInt() < boundaryValue.toInt()
            }
            QualityOperation.LE -> {
                actualValue != DEFAULT && actualValue.toInt() <= boundaryValue.toInt()
            }
            QualityOperation.EQ -> {
                actualValue != DEFAULT && actualValue == boundaryValue
            }
            QualityOperation.GE -> {
                actualValue != DEFAULT && actualValue.toInt() >= boundaryValue.toInt()
            }
            QualityOperation.GT -> {
                actualValue != DEFAULT && actualValue.toInt() > boundaryValue.toInt()
            }
        }
    }

    fun validDecimal(actualValue: BigDecimal?, boundaryValue: BigDecimal, operation: QualityOperation): Boolean {
        if (actualValue == null) return false
        return when (operation) {
            QualityOperation.LT -> {
                actualValue.compareTo(boundaryValue) == -1
            }
            QualityOperation.LE -> {
                actualValue.compareTo(boundaryValue) == -1 || actualValue.compareTo(boundaryValue) == 0
            }
            QualityOperation.EQ -> {
                actualValue.compareTo(boundaryValue) == 0
            }
            QualityOperation.GE -> {
                actualValue.compareTo(boundaryValue) == 1 || actualValue.compareTo(boundaryValue) == 0
            }
            QualityOperation.GT -> {
                actualValue.compareTo(boundaryValue) == 1
            }
        }
    }

    fun validBoolean(actualValue: String?, boundaryValue: String, operation: QualityOperation): Boolean {
        if (actualValue == null) return false
        return actualValue == boundaryValue
    }
}
