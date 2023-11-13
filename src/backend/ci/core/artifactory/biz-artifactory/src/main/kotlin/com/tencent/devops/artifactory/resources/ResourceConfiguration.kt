package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServiceLogFileResource
import com.tencent.devops.artifactory.api.user.UserArtifactoryResource
import com.tencent.devops.artifactory.api.user.UserFileResource
import com.tencent.devops.artifactory.api.user.UserLogFileResource
import com.tencent.devops.artifactory.api.user.UserReportStorageResource
import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.service.ArchiveFileService
import com.tencent.devops.artifactory.service.PipelineBuildArtifactoryService
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
    @ConditionalOnMissingBean(PipelineBuildArtifactoryService::class)
    fun pipelineBuildArtifactoryService(
        @Autowired dslContext: DSLContext,
        @Autowired fileDao: FileDao,
        @Autowired bkRepoClient: BkRepoClient
    ): PipelineBuildArtifactoryService =
        SamplePipelineBuildArtifactoryService(dslContext, fileDao, bkRepoClient)

    @Bean
    @ConditionalOnMissingBean(UserArtifactoryResource::class)
    fun userArtifactoryResource(@Autowired archiveFileService: ArchiveFileService): UserArtifactoryResource =
        UserArtifactoryResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean(UserFileResource::class)
    fun userFileResource(@Autowired archiveFileService: ArchiveFileService): UserFileResource =
        UserFileResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean(ServiceArtifactoryResource::class)
    fun serviceArtifactoryResource(@Autowired archiveFileService: ArchiveFileService): ServiceArtifactoryResource =
        ServiceArtifactoryResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean(UserReportStorageResource::class)
    fun userReportStorageResource(@Autowired archiveFileService: ArchiveFileService): UserReportStorageResource =
        UserReportStorageResourceImpl(archiveFileService)

    @Bean
    @ConditionalOnMissingBean(UserLogFileResource::class)
    fun userLogFileResource(): UserLogFileResource = UserLogFileResourceImpl()

    @Bean
    @ConditionalOnMissingBean(ServiceLogFileResource::class)
    fun serviceLogFileResource(): ServiceLogFileResource = ServiceLogFileResourceImpl()

    @Bean
    @ConditionalOnMissingBean(BuildArtifactoryResource::class)
    fun buildArtifactoryResource(
        @Autowired archiveFileService: ArchiveFileService,
        @Autowired client: Client
    ): BuildArtifactoryResource = BuildArtifactoryResourceImpl(archiveFileService, client)
}
