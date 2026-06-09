/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.common.api.enums

/**
 * [PlatformEnum] 文件后缀白名单的运行时覆盖注册表。
 * 由各业务服务在启动 / 配置刷新时通过 [register] 注入；空时回落到 [PlatformEnum.defaultTails]。
 */
object PlatformTailsRegistry {

    @Volatile
    private var overrides: Map<String, List<String>> = emptyMap()

    fun register(tails: Map<String, List<String>>) {
        overrides = tails.mapKeys { it.key.uppercase() }
    }

    fun get(platform: PlatformEnum): List<String>? = overrides[platform.name]
}
