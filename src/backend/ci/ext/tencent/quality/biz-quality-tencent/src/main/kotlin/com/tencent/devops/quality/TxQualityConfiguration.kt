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

package com.tencent.devops.quality

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.quality.bean.GitCIQualityPipelineUrlBeanImpl
import com.tencent.devops.quality.dao.QualityNotifyGroupDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import com.tencent.devops.quality.service.QualityPermissionService
import com.tencent.devops.quality.service.StreamQualityPermissionServiceImpl
import com.tencent.devops.quality.service.TxQualityPermissionService
import com.tencent.devops.quality.service.TxV3QualityPermissionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TxQualityConfiguration {
    @Bean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun txQualityPermissionService(
        bkAuthPermissionApi: AuthPermissionApi,
        bkAuthResourceApi: AuthResourceApi,
        serviceCode: QualityAuthServiceCode,
        managerService: ManagerService,
        qualityRuleDao: QualityRuleDao,
        qualityGroupDao: QualityRuleDao,
        dslContext: DSLContext
    ) = TxQualityPermissionService(
        bkAuthPermissionApi, bkAuthResourceApi, serviceCode, managerService, qualityRuleDao, qualityGroupDao, dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txV3QualityPermissionService(
        client: Client,
        dslContext: DSLContext,
        ruleDao: QualityRuleDao,
        groupDao: QualityNotifyGroupDao,
        tokenService: ClientTokenService,
        authResourceApiStr: AuthResourceApiStr
    ) = TxV3QualityPermissionService(
        client, dslContext, ruleDao, groupDao, tokenService, authResourceApiStr
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitStreamQualityPermissionService(
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
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "stream")
    fun qualityUrlBean(
        @Autowired client: Client
    ) = GitCIQualityPipelineUrlBeanImpl(client)
}
