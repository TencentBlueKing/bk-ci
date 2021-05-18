/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.service

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class Profile(private val environment: Environment) {

    companion object {
        const val PROFILE_DEFAULT = "local"
        const val PROFILE_DEVELOPMENT = "dev"
        const val PROFILE_PRODUCTION = "prod"
        const val PROFILE_TEST = "test"
        const val PROFILE_EXP = "exp"
    }

    private val activeProfiles = environment.activeProfiles

    fun isDebug(): Boolean {
        /*return activeProfiles.isEmpty() || activeProfiles.contains(PROFILE_DEFAULT) || activeProfiles.contains(
                PROFILE_DEVELOPMENT
        ) || activeProfiles.contains(PROFILE_TEST)*/
        return false
    }

    fun isDev() = activeProfiles.contains(PROFILE_DEVELOPMENT)

    fun isExp() = activeProfiles.contains(PROFILE_EXP)

    fun isTest() = activeProfiles.contains(PROFILE_TEST)

    fun isProd() = activeProfiles.contains(PROFILE_PRODUCTION)

    fun isLocal() = activeProfiles.contains(PROFILE_DEFAULT) || activeProfiles.isEmpty()

    fun isInEnv(profileNames: Set<String>): Boolean {
        if (activeProfiles.isEmpty() && profileNames.contains(PROFILE_DEFAULT)) {
            return true
        }
        return profileNames.any { activeProfiles.contains(it) }
    }

    fun getApplicationName() = environment.getProperty("spring.application.name")
}
