package com.tencent.devops.prebuild.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PreBuildConfig {

    @Value("\${codeCC.softwarePath}")
    val codeCCSofwarePath: String? = null
}