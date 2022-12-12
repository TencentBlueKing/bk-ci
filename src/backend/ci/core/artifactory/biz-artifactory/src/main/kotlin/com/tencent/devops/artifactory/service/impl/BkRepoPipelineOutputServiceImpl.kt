package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PipelineOutput
import com.tencent.devops.artifactory.pojo.PipelineOutputSearchOption
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.PipelineOutputType
import com.tencent.devops.artifactory.service.BkRepoPipelineOutputService
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceReportResource
import com.tencent.devops.common.archive.pojo.ReportListDTO
import com.tencent.devops.common.archive.pojo.TaskReport
import org.springframework.stereotype.Service

@Service
class BkRepoPipelineOutputServiceImpl(
    private val client: Client,
    private val bkRepoArchiveFileServiceImpl: BkRepoArchiveFileServiceImpl
) : BkRepoPipelineOutputService{
    override fun search(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        option: PipelineOutputSearchOption?
    ): PipelineOutput {
        val artifacts = mutableListOf<FileInfo>()
        val reports = mutableListOf<TaskReport>()
        if (option?.pipelineOutputType == null || option.pipelineOutputType == PipelineOutputType.ARTIFACT) {
            val searchProps = SearchProps(
                fileNames = emptyList(),
                props = mapOf(
                    "pipelineId" to pipelineId,
                    "buildId" to buildId
                )
            )
            artifacts.addAll(
                bkRepoArchiveFileServiceImpl.searchFileList(
                    userId = userId,
                    projectId = projectId,
                    page = 1,
                    pageSize = 1000,
                    searchProps = searchProps
                ).records
            )
        }

        if (option?.pipelineOutputType == null || option.pipelineOutputType == PipelineOutputType.REPORT) {
            val reportListDTO = ReportListDTO(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                needPermission = true
            )
            reports.addAll(client.get(ServiceReportResource::class).get(reportListDTO).data!!)
        }

        return PipelineOutput(artifacts, reports)
    }
}
