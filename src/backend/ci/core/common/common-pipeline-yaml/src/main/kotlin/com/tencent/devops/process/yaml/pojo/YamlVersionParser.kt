package com.tencent.devops.process.yaml.pojo

interface YamlVersionParser {

    fun yamlVersion(): YamlVersion
}

enum class YamlVersion(val tag: String) {
    V2_0("v2.0"),
    V3_0("v3.0");

    companion object {
        const val V2 = "v2.0"
        const val V3 = "v3.0"

        fun parse(yamlTag: String): YamlVersion? {
            return when (yamlTag) {
                V2 -> V2_0
                V3 -> V3_0
                else -> null
            }
        }
    }
}
