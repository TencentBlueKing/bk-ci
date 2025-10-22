/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.log.lucene

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.WebAutoConfiguration
import com.tencent.devops.log.jmx.LogStorageBean
import com.tencent.devops.log.service.IndexService
import com.tencent.devops.log.service.LogService
import com.tencent.devops.log.service.LogStatusService
import com.tencent.devops.log.service.LogTagService
import com.tencent.devops.log.service.impl.LogServiceLuceneImpl
import com.tencent.devops.log.service.BuildLogPrintService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Configuration
@ConditionalOnProperty(prefix = "log.storage", name = ["type"], havingValue = "lucene")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(WebAutoConfiguration::class)
@EnableConfigurationProperties(LuceneProperties::class)
class LuceneAutoConfiguration {

    @Value("\${log.lucene.dataDirectory}")
    private val dataDirectory: String? = null
    @Value("\${log.lucene.indexMaxSize}")
    private val indexMaxSize: Int? = Int.MAX_VALUE

    @Bean
    @Primary
    fun luceneClient(indexService: IndexService, redisOperation: RedisOperation): LuceneClient {
        if (dataDirectory.isNullOrBlank()) {
            throw IllegalArgumentException("Lucene storage path not config: log.lucene.dataDirectory")
        }
        return LuceneClient(dataDirectory!!, indexService, redisOperation)
    }

    @Bean
    fun luceneLogService(
        @Autowired luceneClient: LuceneClient,
        @Autowired indexService: IndexService,
        @Autowired logStatusService: LogStatusService,
        @Autowired logTagService: LogTagService,
        @Autowired defaultKeywords: List<String>,
        @Autowired logStorageBean: LogStorageBean,
        @Autowired buildLogPrintService: BuildLogPrintService
    ): LogService {
        if (indexMaxSize == null || indexMaxSize!! <= 0) {
            throw IllegalArgumentException("Lucene index max size of build invaild: log.lucene.indexMaxSize")
        }
        return LogServiceLuceneImpl(
            indexMaxSize = indexMaxSize!!,
            luceneClient = luceneClient,
            indexService = indexService,
            logStatusService = logStatusService,
            logTagService = logTagService,
            logStorageBean = logStorageBean,
            buildLogPrintService = buildLogPrintService
        )
    }
}
