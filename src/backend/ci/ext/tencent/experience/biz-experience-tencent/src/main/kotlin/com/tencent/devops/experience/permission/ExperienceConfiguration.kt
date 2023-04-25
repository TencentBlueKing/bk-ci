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

package com.tencent.devops.experience.permission

import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.GroupDao
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ExperienceConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun txExperiencePermissionServiceImpl(
        bsAuthPermissionApi: BSAuthPermissionApi,
        bsAuthResourceApi: BSAuthResourceApi,
        experienceServiceCode: BSExperienceAuthServiceCode
    ) = TxExperiencePermissionServiceImpl(
        bsAuthPermissionApi, bsAuthResourceApi, experienceServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txV3ExperiencePermissionServiceImpl(
        client: Client,
        dslContext: DSLContext,
        experienceDao: ExperienceDao,
        groupDao: GroupDao,
        tokenService: ClientTokenService,
        authResourceApiStr: AuthResourceApiStr
    ) = TxV3ExperiencePermissionServiceImpl(
        client, dslContext, experienceDao, groupDao, tokenService, authResourceApiStr
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun streamExperiencePermissionServiceImpl(
        client: Client,
        tokenService: ClientTokenService,
        experienceDao: ExperienceDao,
        groupDao: GroupDao,
        dslContext: DSLContext
    ) = StreamExperiencePermissionServiceImpl(
        client, tokenService, experienceDao, groupDao, dslContext
    )
}
