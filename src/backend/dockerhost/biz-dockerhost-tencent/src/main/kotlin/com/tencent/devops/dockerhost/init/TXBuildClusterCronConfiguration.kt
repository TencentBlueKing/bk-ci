package com.tencent.devops.dockerhost.init

import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.config.TXDockerHostConfig
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

    @Value("\${downloadAgentCron}")
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
    fun updateAgentRunner(dockerHostConfig: TXDockerHostConfig): UpdateAgentRunner {
        return UpdateAgentRunner(dockerHostConfig)
    }
}
