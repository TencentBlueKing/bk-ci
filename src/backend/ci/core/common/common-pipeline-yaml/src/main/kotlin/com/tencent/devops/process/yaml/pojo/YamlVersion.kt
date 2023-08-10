package com.tencent.devops.process.yaml.pojo

interface YamlVersion {
    enum class Version {
        V2_0,
        V3_0
    }

    fun yamlVersion(): Version
}
