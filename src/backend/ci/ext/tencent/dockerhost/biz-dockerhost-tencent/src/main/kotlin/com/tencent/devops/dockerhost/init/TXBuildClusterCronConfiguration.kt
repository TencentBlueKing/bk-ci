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

package com.tencent.devops.dockerhost.init

import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.cron.DebugRunner
import com.tencent.devops.dockerhost.cron.UpdateAgentRunner
import com.tencent.devops.dockerhost.service.DockerHostDebugService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.IntervalTask
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.util.concurrent.Executors

/**
 * 有构建环境的docker集群下才会生效
 * @version 1.0
 */

@Configuration
@ConditionalOnProperty(prefix = "run", name = ["mode"], havingValue = "docker_build")
@EnableScheduling
class TXBuildClusterCronConfiguration : SchedulingConfigurer {

    @Value("\${dockerCli.downloadAgentCron:0 0/30 * * * ?}")
    var downloadAgentCron: String? = null

    override fun configureTasks(scheduledTaskRegistrar: ScheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(Executors.newScheduledThreadPool(100))

        scheduledTaskRegistrar.addFixedRateTask(
            IntervalTask(
                Runnable { debugRunner.startBuild() }, 5 * 1000, 30 * 1000
            )
        )
        scheduledTaskRegistrar.addFixedRateTask(
            IntervalTask(
                Runnable { debugRunner.endBuild() }, 10 * 1000, 60 * 1000
            )
        )
        scheduledTaskRegistrar.addCronTask(
//                { updateAgentRunner.update() }, "0 0 3 * * ?"
            { updateAgentRunner.update() }, downloadAgentCron!!
        )
    }

    @Autowired
    private lateinit var debugRunner: DebugRunner

    @Autowired
    private lateinit var updateAgentRunner: UpdateAgentRunner

    @Bean
    fun debugRunner(dockerHostDebugService: DockerHostDebugService): DebugRunner {
        return DebugRunner(dockerHostDebugService)
    }

    @Bean
    fun updateAgentRunner(dockerHostConfig: DockerHostConfig): UpdateAgentRunner {
        return UpdateAgentRunner(dockerHostConfig)
    }
}
