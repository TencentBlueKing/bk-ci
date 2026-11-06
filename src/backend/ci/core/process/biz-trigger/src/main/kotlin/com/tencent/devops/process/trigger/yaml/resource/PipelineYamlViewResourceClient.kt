package com.tencent.devops.process.trigger.yaml.resource

import com.tencent.devops.process.api.service.ServicePipelineYamlViewResource
import com.tencent.devops.process.dao.yaml.PipelineYamlViewDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewCleanupReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewCreateReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewGroupUpdateReq
import com.tencent.devops.process.trigger.TriggerProcessClient
import org.jooq.DSLContext
import org.springframework.stereotype.Service

/**
 * PAC 流水线组操作，流水线组属于 process，通过接口调用
 */
@Service
class PipelineYamlViewResourceClient(
    private val dslContext: DSLContext,
    private val pipelineYamlViewDao: PipelineYamlViewDao,
    private val triggerProcessClient: TriggerProcessClient
) {

    fun createYamlViewIfAbsent(
        userId: String,
        projectId: String,
        repoHashId: String,
        aliasName: String,
        directoryList: Set<String>
    ) {
        val existedDirectories = pipelineYamlViewDao.listRepoYamlView(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId
        ).map { it.directory }.toSet()
        if (existedDirectories.containsAll(directoryList)) {
            return
        }
        triggerProcessClient.call(projectId, ServicePipelineYamlViewResource::class) {
            it.createYamlViewIfAbsent(
                userId = userId,
                projectId = projectId,
                request = PipelineYamlViewCreateReq(
                    repoHashId = repoHashId,
                    aliasName = aliasName,
                    directoryList = directoryList
                )
            )
        }
    }

    fun cleanupYamlView(
        userId: String,
        projectId: String,
        repoHashId: String,
        directory: String,
        pipelineId: String
    ) {
        pipelineYamlViewDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        ) ?: return
        triggerProcessClient.call(projectId, ServicePipelineYamlViewResource::class) {
            it.cleanupYamlView(
                userId = userId,
                projectId = projectId,
                request = PipelineYamlViewCleanupReq(
                    repoHashId = repoHashId,
                    directory = directory,
                    pipelineId = pipelineId
                )
            )
        }
    }

    fun updateGroupAfterPipelineUpdate(
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        creator: String,
        userId: String
    ) {
        triggerProcessClient.call(projectId, ServicePipelineYamlViewResource::class) {
            it.updateGroupAfterPipelineUpdate(
                userId = userId,
                projectId = projectId,
                request = PipelineYamlViewGroupUpdateReq(
                    pipelineId = pipelineId,
                    pipelineName = pipelineName,
                    creator = creator
                )
            )
        }
    }
}
