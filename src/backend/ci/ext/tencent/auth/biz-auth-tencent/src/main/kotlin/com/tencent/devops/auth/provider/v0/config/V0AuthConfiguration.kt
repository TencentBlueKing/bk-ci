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

package com.tencent.devops.auth.provider.v0.config

import com.tencent.devops.auth.provider.v0.service.ServiceCodeService
import com.tencent.devops.auth.provider.v0.service.V0AuthPermissionProjectServiceImpl
import com.tencent.devops.auth.provider.v0.service.V0AuthPermissionServiceImpl
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
class V0AuthConfiguration {

    @Bean
    @Primary
    fun defaultPermissionServiceImpl(
        authPermissionApi: AuthPermissionApi,
        serviceCodeService: ServiceCodeService
    ) = V0AuthPermissionServiceImpl(authPermissionApi, serviceCodeService)

    @Bean
    @Primary
    fun defaultPermissionProjectServiceImpl(
        authProjectApi: AuthProjectApi,
        bsProjectAuthServiceCode: BSProjectServiceCodec,
        client: Client
    ) = V0AuthPermissionProjectServiceImpl(authProjectApi, bsProjectAuthServiceCode, client)
}
