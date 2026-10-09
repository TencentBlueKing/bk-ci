package com.tencent.devops.environment.pojo.enums

enum class AgentType {
    BUILD,
    CREATE;

    companion object {
        fun fromValue(value: String?) = when (value) {
            "BUILD" -> BUILD
            "CREATE" -> CREATE
            else -> null
        }
    }
}