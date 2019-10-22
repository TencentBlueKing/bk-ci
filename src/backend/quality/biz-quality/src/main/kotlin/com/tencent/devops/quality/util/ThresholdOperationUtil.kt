package com.tencent.devops.quality.util

import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import java.math.BigDecimal

object ThresholdOperationUtil {
    val DEFAULT = "null"

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
}