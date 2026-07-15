/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.experience.config

import com.tencent.devops.common.api.enums.PlatformTailsRegistry
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Component

/**
 * 平台文件后缀白名单配置（支持 /actuator/refresh 热更新）。
 * 不配置时使用 [com.tencent.devops.common.api.enums.PlatformEnum.defaultTails]。
 */
@Component
@RefreshScope
class PlatformTailsConfig {

    @Value("\${experience.platform.tails.android:#{null}}")
    private var androidTails: List<String>? = null

    @Value("\${experience.platform.tails.ios:#{null}}")
    private var iosTails: List<String>? = null

    @Value("\${experience.platform.tails.hap:#{null}}")
    private var hapTails: List<String>? = null

    @Value("\${experience.platform.tails.win:#{null}}")
    private var winTails: List<String>? = null

    @PostConstruct
    fun register() {
        val overrides = buildMap<String, List<String>> {
            androidTails?.let { put("ANDROID", it) }
            iosTails?.let { put("IOS", it) }
            hapTails?.let { put("HAP", it) }
            winTails?.let { put("WIN", it) }
        }
        PlatformTailsRegistry.register(overrides)
    }
}
