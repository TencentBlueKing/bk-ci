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

package com.tencent.devops.common.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.auth.api.external.BkAuthExPermissionApi
import com.tencent.devops.common.auth.api.external.BkAuthExProperties
import com.tencent.devops.common.auth.api.external.BkAuthExRegisterApi
import com.tencent.devops.common.auth.api.pojo.external.AUTH_PRINCIPAL_TYPE
import com.tencent.devops.common.auth.api.pojo.external.AUTH_SCOPE_TYPE
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.*
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AuthExAutoConfiguration {

    @Bean
    @Primary
    fun authProperties() = AuthExProperties()

    @Bean
    fun bkAuthPropertiesDev(authExProperties: AuthExProperties) = BkAuthExProperties(
            envName = authExProperties.envName,
            systemId = authExProperties.systemId,
            principalType = AUTH_PRINCIPAL_TYPE,
            scopeType = AUTH_SCOPE_TYPE,
            resourceType = authExProperties.resourceType,
            url = authExProperties.url,
            codeccCode = authExProperties.codeccCode,
            codeccSecret = authExProperties.codeccSecret
    )

    @Bean
    @Primary
    fun bkAuthExPermissionApi(bkAuthProperties: BkAuthExProperties, objectMapper: ObjectMapper) =
            BkAuthExPermissionApi(bkAuthProperties, objectMapper)


    @Bean
    @Primary
    fun bkAuthExRegisterApi(bkAuthProperties: BkAuthExProperties, objectMapper: ObjectMapper) =
            BkAuthExRegisterApi(bkAuthProperties, objectMapper)


}