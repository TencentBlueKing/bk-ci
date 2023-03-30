/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.service

import com.tencent.devops.common.service.env.Env
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 *
 * Powered By Tencent
 */
const val PROFILE_DEFAULT = "default"

const val PROFILE_DEVELOPMENT = "dev"
const val PROFILE_PRODUCTION = "prod"
const val PROFILE_TEST = "test"
const val PROFILE_STREAM = "stream"
const val PROFILE_AUTO = "auto"

@Component
class Profile(private val environment: Environment) {

    private val activeProfiles = environment.activeProfiles

    fun isDebug(): Boolean {
        return activeProfiles.isEmpty() ||
            activeProfiles.contains(PROFILE_DEFAULT) ||
            activeProfiles.contains(PROFILE_DEVELOPMENT) ||
            activeProfiles.contains(PROFILE_TEST)
    }

    fun isDev(): Boolean {
        return activeProfiles.contains(PROFILE_DEVELOPMENT)
    }

    fun isTest(): Boolean {
        return activeProfiles.contains(PROFILE_TEST)
    }

    fun isProd(): Boolean {
        return activeProfiles.contains(PROFILE_PRODUCTION)
    }

    fun isLocal() =
        activeProfiles.contains(PROFILE_DEFAULT)

    fun isStream(): Boolean {
        activeProfiles.forEach { activeProfile ->
            if (activeProfile.contains(PROFILE_STREAM)) {
                return true
            }
        }
        return false
    }

    fun isAuto(): Boolean {
        activeProfiles.forEach { activeProfile ->
            if (activeProfile.contains(PROFILE_AUTO)) {
                return true
            }
        }
        return false
    }

    fun getEnv(): Env {
        return when {
            isProd() -> Env.PROD
            isTest() -> Env.TEST
            isDev() -> Env.DEV
            isLocal() -> Env.DEFAULT
            else -> Env.PROD
        }
    }

    fun getActiveProfiles(): Array<String> {
        return activeProfiles
    }

    fun getApplicationName() = environment.getProperty("spring.application.name")
}
