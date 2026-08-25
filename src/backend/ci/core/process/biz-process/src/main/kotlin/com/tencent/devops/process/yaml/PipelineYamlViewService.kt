package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.dao.yaml.PipelineYamlViewDao
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
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineYamlViewService(
    private val dslContext: DSLContext,
    private val pipelineYamlViewDao: PipelineYamlViewDao,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineViewService: PipelineViewService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val redisOperation: RedisOperation
) {

    fun createYamlViewIfAbsent(
        userId: String,
        projectId: String,
        repoHashId: String,
        aliasName: String,
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
                    aliasName = aliasName,
                    directory = directory
                )
            }
        }
    }

    fun createYamlView(
        userId: String,
        projectId: String,
        repoHashId: String,
        aliasName: String,
        directory: String
    ) {
        val name = if (directory == Constansts.ciFileDirectoryName) {
            aliasName
        } else {
            "$aliasName-${directory.removePrefix(".ci/")}"
        }.coerceAtMaxLength(PipelineViewService.YAML_PIPELINE_VIEW_NAME_LENGTH_MAX)
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
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val viewId = pipelineViewService.addView(
                userId = userId,
                projectId = projectId,
                pipelineView = pipelineView,
                context = transactionContext
            )
            pipelineYamlViewDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                repoHashId = repoHashId,
                directory = directory,
                viewId = viewId
            )
        }
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

    /**
     * 删除 YAML 流水线后清理流水线组：
     * 1. 从对应 YAML 流水线组中移除该流水线
     * 2. 若该目录下已无 YAML 流水线，则删除空的流水线组
     */
    fun cleanupYamlView(
        userId: String,
        projectId: String,
        repoHashId: String,
        directory: String,
        pipelineId: String
    ) {
        val yamlView = getPipelineYamlView(
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        ) ?: return
        pipelineViewGroupDao.batchRemove(
            dslContext = dslContext,
            projectId = projectId,
            viewId = yamlView.viewId,
            pipelineIds = listOf(pipelineId)
        )
        PipelineYamlViewLock(
            redisOperation = redisOperation,
            projectId = projectId,
            repoHashId = repoHashId
        ).use { lock ->
            lock.lock()
            val yamlPipelineCnt = pipelineYamlInfoDao.countYamlPipeline(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                directory = directory
            )
            if (yamlPipelineCnt == 0L) {
                logger.info("delete pipeline yaml view|$projectId|$repoHashId|$directory")
                deleteYamlView(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    directory = directory
                )
                pipelineViewGroupService.deleteViewGroup(
                    projectId = projectId,
                    userId = userId,
                    viewIdEncode = HashUtil.encodeLongId(yamlView.viewId),
                    checkPac = false,
                    checkPermission = false
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlViewService::class.java)
    }
}
