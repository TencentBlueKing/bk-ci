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

package com.tencent.devops.quality.service.permission.config

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.quality.dao.QualityNotifyGroupDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import com.tencent.devops.quality.service.QualityPermissionService
import com.tencent.devops.quality.service.permission.SampleQualityPermissionServiceImpl
import com.tencent.devops.quality.service.StreamQualityPermissionServiceImpl
import com.tencent.devops.quality.service.permission.RbacQualityPermissionServiceImpl
import com.tencent.devops.quality.service.permission.V3QualityPermissionServiceImpl
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("UNUSED")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class QualityConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "sample")
    fun sampleQualityPermissionService(
        authPermissionApi: AuthPermissionApi,
        authResourceApi: AuthResourceApi,
        qualityAuthServiceCode: QualityAuthServiceCode,
        qualityRuleDao: QualityRuleDao,
        groupDao: QualityNotifyGroupDao,
        dslContext: DSLContext
    ): QualityPermissionService = SampleQualityPermissionServiceImpl(
        authPermissionApi = authPermissionApi,
        authResourceApi = authResourceApi,
        qualityAuthServiceCode = qualityAuthServiceCode,
        qualityRuleDao = qualityRuleDao,
        groupDao = groupDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
    fun v3QualityPermissionServiceImpl(
        authPermissionApi: AuthPermissionApi,
        authResourceApi: AuthResourceApi,
        qualityAuthServiceCode: QualityAuthServiceCode,
        groupDao: QualityNotifyGroupDao,
        qualityRuleDao: QualityRuleDao,
        dslContext: DSLContext
    ): QualityPermissionService = V3QualityPermissionServiceImpl(
        authPermissionApi = authPermissionApi,
        authResourceApi = authResourceApi,
        qualityAuthServiceCode = qualityAuthServiceCode,
        dslContext = dslContext,
        groupDao = groupDao,
        qualityRuleDao = qualityRuleDao
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamQualityPermissionService(
        client: Client,
        tokenCheckService: ClientTokenService,
        ruleDao: QualityRuleDao,
        groupDao: QualityNotifyGroupDao,
        dslContext: DSLContext
    ): QualityPermissionService = StreamQualityPermissionServiceImpl(
        client = client,
        tokenCheckService = tokenCheckService,
        ruleDao = ruleDao,
        groupDao = groupDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamQualityPermissionService(
        client: Client,
        tokenCheckService: ClientTokenService,
        ruleDao: QualityRuleDao,
        groupDao: QualityNotifyGroupDao,
        dslContext: DSLContext
    ): QualityPermissionService = StreamQualityPermissionServiceImpl(
        client = client,
        tokenCheckService = tokenCheckService,
        ruleDao = ruleDao,
        groupDao = groupDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacQualityPermissionService(
        client: Client,
        dslContext: DSLContext,
        tokenService: ClientTokenService
    ): QualityPermissionService = RbacQualityPermissionServiceImpl(
        client = client,
        dslContext = dslContext,
        tokenService = tokenService
    )
}
