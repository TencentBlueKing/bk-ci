package com.tencent.devops.common.auth.api

/**
 * Created by Aaron Sheng on 2018/1/23.
 */
enum class BkAuthServiceCode(val value: String) {
    BCS("bcs"),
    CODE("code"),
    PIPELINE("pipeline"),
    ARTIFACTORY("artifactory"),
    TICKET("ticket"),
    ENVIRONMENT("environment"),
    EXPERIENCE("experience"),
    VS("vs"),
    QUALITY("quality_gate"),
    WETEST("wetest");

    companion object {
        fun get(value: String): BkAuthServiceCode {
            BkAuthServiceCode.values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }
    }
}