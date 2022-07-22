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
 *
 */

package com.tencent.devops.process.config

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.measure.MeasureServiceImpl
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import com.tencent.devops.process.template.service.TemplateService
import org.jooq.DSLContext
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Suppress("ALL")
@Configuration
class TencentOldMeasureConfig {

    @Value("\${build.atomMonitorData.report.switch:false}")
    private val atomMonitorSwitch: String = "false"

    @Value("\${build.atomMonitorData.report.maxMonitorDataSize:1677216}")
    private val maxMonitorDataSize: String = "1677216"

    @Bean
    @ConditionalOnMissingBean(name = ["measureEventDispatcher"])
    fun measureEventDispatcher(rabbitTemplate: RabbitTemplate) = MeasureEventDispatcher(rabbitTemplate)

    @Bean
    @ConditionalOnMissingBean(name = ["measureService"])
    fun measureService(
        @Autowired projectCacheService: ProjectCacheService,
        @Autowired pipelineTaskService: PipelineTaskService,
        @Autowired buildVariableService: BuildVariableService,
        @Autowired dslContext: DSLContext,
        @Autowired templateService: TemplateService,
        @Autowired pipelineInfoService: PipelineInfoService,
        @Autowired redisOperation: RedisOperation,
        @Autowired pipelineEventDispatcher: PipelineEventDispatcher,
        @Autowired measureEventDispatcher: MeasureEventDispatcher
    ) = MeasureServiceImpl(
        projectCacheService = projectCacheService,
        pipelineTaskService = pipelineTaskService,
        buildVariableService = buildVariableService,
        templateService = templateService,
        pipelineInfoService = pipelineInfoService,
        redisOperation = redisOperation,
        pipelineEventDispatcher = pipelineEventDispatcher,
        atomMonitorSwitch = atomMonitorSwitch,
        maxMonitorDataSize = maxMonitorDataSize,
        measureEventDispatcher = measureEventDispatcher
    )
}
