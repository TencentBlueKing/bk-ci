package com.tencent.devops.process.yaml.pojo

interface YamlVersion {
    enum class Version {
        V2_0,
        V3_0;

        companion object {
            const val V2 = "v2.0"
            const val V3 = "v3.0"
        }
    }

    fun yamlVersion(): Version
}
