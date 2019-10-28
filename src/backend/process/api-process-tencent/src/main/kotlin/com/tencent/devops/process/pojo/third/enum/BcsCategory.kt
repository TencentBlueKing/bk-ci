package com.tencent.devops.process.pojo.third.enum

import com.fasterxml.jackson.annotation.JsonValue
import javax.ws.rs.NotFoundException

enum class BcsCategory(private val category: String) {
    DAEMONSET("DaemonSet"),
    JOB("Job"),
    DEPLOYMENT("Deployment"),
    STATEFULSET("StatefulSet"),
    APPLICATION("Application");

    @JsonValue
    fun getValue(): String {
        return category
    }

    companion object {
        fun parse(value: String?): BcsCategory {
            values().forEach { category ->
                if (category.getValue() .equals(value, false)) {
                    return category
                }
            }
            throw NotFoundException("Unknown BcsCategory - $value")
        }
    }
}