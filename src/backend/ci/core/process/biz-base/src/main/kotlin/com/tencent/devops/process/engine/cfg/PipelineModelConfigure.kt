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

package com.tencent.devops.process.engine.cfg

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.process.engine.atom.plugin.IElementBizPluginService
import com.tencent.devops.process.engine.extend.DefaultModelCheckPlugin
import com.tencent.devops.process.pojo.config.JobCommonSettingConfig
import com.tencent.devops.process.pojo.config.PipelineCommonSettingConfig
import com.tencent.devops.process.pojo.config.StageCommonSettingConfig
import com.tencent.devops.process.pojo.config.TaskCommonSettingConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class PipelineModelConfigure {

    @Bean
    @ConditionalOnMissingBean(ModelCheckPlugin::class)
    fun modelCheckPlugin(
        @Autowired client: Client,
        @Autowired pipelineCommonSettingConfig: PipelineCommonSettingConfig,
        @Autowired stageCommonSettingConfig: StageCommonSettingConfig,
        @Autowired jobCommonSettingConfig: JobCommonSettingConfig,
        @Autowired taskCommonSettingConfig: TaskCommonSettingConfig,
        @Autowired elementBizPluginServices: List<IElementBizPluginService>
    ) = DefaultModelCheckPlugin(
        client = client,
        pipelineCommonSettingConfig = pipelineCommonSettingConfig,
        stageCommonSettingConfig = stageCommonSettingConfig,
        jobCommonSettingConfig = jobCommonSettingConfig,
        taskCommonSettingConfig = taskCommonSettingConfig,
        elementBizPluginServices = elementBizPluginServices
    )

    @Bean
    fun pipelineIdGenerator() = PipelineIdGenerator()

    @Bean
    fun buildIdGenerator() = BuildIdGenerator()

    @Bean
    fun modelContainerIdGenerator() = ModelContainerIdGenerator()

    @Bean
    fun modelStageIdGenerator() = ModelStageIdGenerator()

    @Bean
    fun modelTaskIdGenerator() = ModelTaskIdGenerator()
}
