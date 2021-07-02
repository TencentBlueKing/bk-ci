package com.tencent.devops.auth.configuration

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.gitci.GitCIPermissionProjectServiceImpl
import com.tencent.devops.auth.service.gitci.GitCIPermissionServiceImpl
import com.tencent.devops.auth.service.gitci.GitCiProjectInfoService
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitCI")
class GitCIConfiguration {
    @Bean
    fun gitCIPermissionServiceImpl(
        client: Client,
        managerService: ManagerService,
        projectInfoService: GitCiProjectInfoService
    ) = GitCIPermissionServiceImpl(client, managerService, projectInfoService)

    @Bean
    fun gitCIPermissionProjectServiceImpl(
        client: Client,
        projectInfoService: GitCiProjectInfoService
    ) = GitCIPermissionProjectServiceImpl(client, projectInfoService)

    @Bean
    fun gitProjectInfoService(
        client: Client
    ) = GitCiProjectInfoService(client)
}
