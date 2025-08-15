package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryConfigResource
import com.tencent.devops.artifactory.config.ReportArchiveTaskConfig
import com.tencent.devops.artifactory.pojo.ReportPluginConfig
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Value

@RestResource
class BuildArtifactoryConfigResourceImpl(
    private val reportArchiveTaskConfig: ReportArchiveTaskConfig
) : BuildArtifactoryConfigResource {

    @Value("\${artifactory.realm:}")
    private var artifactoryRealm: String = ""

    override fun getRealm(): Result<String> {
        return Result(artifactoryRealm)
    }

    override fun getReportConfig(): Result<ReportPluginConfig> {
        return Result(ReportPluginConfig(
            enableCompress = reportArchiveTaskConfig.enabledCompress,
            enableCompressPipelines = reportArchiveTaskConfig.enableCompressPipelines,
            compressThreshold = reportArchiveTaskConfig.compressThreshold,
            compressSizeLimit = reportArchiveTaskConfig.compressSizeLimit
        ))
    }
}
