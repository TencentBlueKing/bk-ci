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

package com.tencent.devops.auth.configuration

import com.tencent.devops.auth.service.v0.V0AuthPermissionProjectServiceImpl
import com.tencent.devops.auth.service.v0.V0AuthPermissionServiceImpl
import com.tencent.devops.auth.service.v0.ServiceCodeService
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class V0AuthConfiguration {
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun defaultPermissionServiceImpl(
        authPermissionApi: AuthPermissionApi,
        serviceCodeService: ServiceCodeService
    ) = V0AuthPermissionServiceImpl(authPermissionApi, serviceCodeService)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun defaultPermissionProjectServiceImpl(
        authProjectApi: AuthProjectApi,
        commonServiceCode: BSCommonAuthServiceCode
    ) = V0AuthPermissionProjectServiceImpl(authProjectApi, commonServiceCode)
}
