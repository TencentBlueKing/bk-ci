package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.service.ArchiveFileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ResourceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun serviceArtifactoryResource(@Autowired archiveFileService: ArchiveFileService) =
        ServiceArtifactoryResourceImpl(archiveFileService)
}
