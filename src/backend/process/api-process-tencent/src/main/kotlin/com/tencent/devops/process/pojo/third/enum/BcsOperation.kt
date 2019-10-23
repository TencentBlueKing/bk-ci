package com.tencent.devops.process.pojo.third.enum

import com.fasterxml.jackson.annotation.JsonValue
import javax.ws.rs.NotFoundException

enum class BcsOperation(private val operation: String) {
    CREATE("create"),
    RECREATE("recreate"),
    SCALE("scale"),
    ROLLINGUPDATE("rollingupdate"),
    DELETE("delete"),
    SIGNAL("signal"),
    COMMAND("command");

    @JsonValue
    fun getValue(): String {
        return operation
    }

    companion object {
        fun parse(value: String?): BcsOperation {
            values().forEach { operation ->
                if (operation.getValue() .equals(value, false)) {
                    return operation
                }
            }
            throw NotFoundException("Unknown BcsOperation - $value")
        }
    }
}