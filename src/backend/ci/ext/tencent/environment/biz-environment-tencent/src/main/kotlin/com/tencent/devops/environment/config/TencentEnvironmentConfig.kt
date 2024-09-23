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

package com.tencent.devops.environment.config

import com.tencent.devops.environment.service.cmdb.EsbCmdbClient
import com.tencent.devops.environment.service.cmdb.NewCmdbClient
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import com.tencent.devops.environment.service.cmdb.impl.TencentCmdbServiceImpl
import com.tencent.devops.environment.service.cmdb.impl.TencentNewCmdbServiceImpl
import com.tencent.devops.environment.service.job.NodeManApi
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class TencentEnvironmentConfig {

    @Bean
    @ConditionalOnMissingBean(NodeManApi::class)
    fun nodeManApi(environmentProperties: EnvironmentProperties): NodeManApi {
        return NodeManApi(
            environmentProperties.nodeman.nodemanApiBaseUrl,
            environmentProperties.apigw.bkAppCode,
            environmentProperties.apigw.bkAppSecret,
            environmentProperties.nodeman.defaultUser
        )
    }

    @Bean
    fun esbCmdbClient(environmentProperties: EnvironmentProperties, esbProperties: EsbProperties): EsbCmdbClient {
        return EsbCmdbClient(
            environmentProperties.cmdb.baseUrl,
            esbProperties.appCode,
            esbProperties.appSecret
        )
    }

    @Bean
    fun newCmdbClient(environmentProperties: EnvironmentProperties): NewCmdbClient {
        return NewCmdbClient(
            environmentProperties.newCmdb.newCmdbBaseUrl,
            environmentProperties.newCmdb.appId,
            environmentProperties.newCmdb.appKey
        )
    }

    @Bean
    fun oldCmdbService(esbCmdbClient: EsbCmdbClient): TencentCmdbService {
        return TencentCmdbServiceImpl(esbCmdbClient)
    }

    @Primary
    @Bean("newCmdbService")
    @ConditionalOnProperty(
        prefix = "environment.newCmdb",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun newCmdbService(newCmdbClient: NewCmdbClient): TencentNewCmdbServiceImpl {
        return TencentNewCmdbServiceImpl(newCmdbClient)
    }
}
