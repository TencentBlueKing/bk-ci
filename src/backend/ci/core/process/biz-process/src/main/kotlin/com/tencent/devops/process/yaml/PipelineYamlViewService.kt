package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.engine.dao.PipelineYamlViewDao
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByPacRepo
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Condition
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.pipeline.PipelineYamlView
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.process.yaml.pojo.PipelineYamlViewLock
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class PipelineYamlViewService(
    private val dslContext: DSLContext,
    private val pipelineYamlViewDao: PipelineYamlViewDao,
    private val pipelineViewService: PipelineViewService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val redisOperation: RedisOperation
) {

    fun createYamlViewIfAbsent(
        userId: String,
        projectId: String,
        repoHashId: String,
        gitProjectName: String,
        directoryList: Set<String>
    ) {
        PipelineYamlViewLock(
            redisOperation = redisOperation,
            projectId = projectId,
            repoHashId = repoHashId
        ).use { lock ->
            lock.lock()
            val repoYamlViewMap = pipelineYamlViewDao.listRepoYamlView(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId
            ).associateBy { it.directory }
            // 需要新创建的流水线组
            val addYamlViews = directoryList.filterNot { repoYamlViewMap.containsKey(it) }
            pipelineViewService.checkPipelineViewCount(
                projectId = projectId,
                userId = userId,
                projected = true,
                addCount = addYamlViews.size
            )
            addYamlViews.forEach { directory ->
                createYamlView(
                    userId = userId,
                    projectId = projectId,
                    repoHashId = repoHashId,
                    gitProjectName = gitProjectName,
                    directory = directory
                )
            }
        }
    }

    fun createYamlView(
        userId: String,
        projectId: String,
        repoHashId: String,
        gitProjectName: String,
        directory: String
    ) {
        val path = gitProjectName.substringAfterLast("/")
        val name = if (directory == Constansts.ciFileDirectoryName) {
            path
        } else {
            "$path-${directory.removePrefix(".ci/")}"
        }.coerceAtMaxLength(PipelineViewService.PIPELINE_VIEW_NAME_LENGTH_MAX)
        val pipelineView = PipelineViewForm(
            name = name,
            projected = true,
            viewType = PipelineViewType.DYNAMIC,
            logic = Logic.AND,
            filters = listOf(
                PipelineViewFilterByPacRepo(
                    condition = Condition.EQUAL,
                    repoHashId = repoHashId,
                    directory = directory
                )
            )
        )
        // 系统创建流水线组不需要校验权限
        val viewHashId = pipelineViewGroupService.addViewGroup(
            projectId = projectId,
            userId = userId,
            pipelineView = pipelineView,
            checkPermission = false
        )
        pipelineYamlViewDao.save(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory,
            viewId = HashUtil.decodeIdToLong(viewHashId)
        )
    }

    fun getPipelineYamlView(
        projectId: String,
        repoHashId: String,
        directory: String
    ): PipelineYamlView? {
        return pipelineYamlViewDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        )
    }

    fun listRepoYamlView(
        projectId: String,
        repoHashId: String
    ): List<PipelineYamlView> {
        return pipelineYamlViewDao.listRepoYamlView(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId
        )
    }

    fun deleteYamlView(
        projectId: String,
        repoHashId: String,
        directory: String
    ) {
        pipelineYamlViewDao.delete(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        )
    }
}
