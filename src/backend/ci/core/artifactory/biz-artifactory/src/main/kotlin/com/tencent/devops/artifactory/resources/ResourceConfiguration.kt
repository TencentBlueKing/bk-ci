package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.artifactory.service.SamplePipelineBuildArtifactoryService
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.client.Client
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ResourceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun pipelineBuildArtifactoryService(
        @Autowired dslContext: DSLContext,
        @Autowired fileDao: FileDao,
        @Autowired bkRepoClient: BkRepoClient
    ) =
        SamplePipelineBuildArtifactoryService(dslContext, fileDao, bkRepoClient)

    @Bean
    @ConditionalOnMissingBean
    fun userArtifactoryResource(@Autowired archiveFileService: ArchiveFileService) =
        UserArtifactoryResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean
    fun userFileResource(@Autowired archiveFileService: ArchiveFileService) =
        UserFileResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean
    fun serviceArtifactoryResource(@Autowired archiveFileService: ArchiveFileService) =
        ServiceArtifactoryResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean
    fun userReportStorageResource(@Autowired archiveFileService: ArchiveFileService) =
        UserReportStorageResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean
    fun userLogFileResource() = UserLogFileResourceImpl()

    @Bean
    @ConditionalOnMissingBean
    fun serviceLogFileResource() = ServiceLogFileResourceImpl()

    @Bean
    @ConditionalOnMissingBean
    fun buildArtifactoryResource(
        @Autowired archiveFileService: ArchiveFileService,
        @Autowired client: Client
    ) = BuildArtifactoryResourceImpl(archiveFileService, client)
}
