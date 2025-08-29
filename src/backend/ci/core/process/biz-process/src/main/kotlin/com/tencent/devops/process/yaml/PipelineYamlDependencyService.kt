package com.tencent.devops.process.yaml

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.dao.yaml.PipelineYamlDependencyDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.dao.yaml.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDependency
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDependencyResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.pojo.pipeline.enums.YamlRefValueType
import com.tencent.devops.process.pojo.template.TemplateRefType
import com.tencent.devops.process.service.template.v2.PipelineTemplatePipelineVersionService
import com.tencent.devops.process.yaml.common.Constansts
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlDependencyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineYamlDependencyDao: PipelineYamlDependencyDao,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val pipelineTemplatePipelineVersionService: PipelineTemplatePipelineVersionService,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao
) {

    /**
     * 分析版本依赖
     */
    fun analyzeVersionDependency(
        projectId: String,
        pipelineId: String,
        pipelineVersion: Int
    ): PipelineYamlDependencyResult? {
        val templatePipelineVersion = pipelineTemplatePipelineVersionService.get(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineVersion = pipelineVersion
        ) ?: return null
        if (templatePipelineVersion.refType == TemplateRefType.ID) {
            return null
        }
        // 获取依赖模版的版本
        val templateYamlVersion = pipelineYamlVersionDao.getPipelineYamlVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = templatePipelineVersion.templateId,
            version = templatePipelineVersion.templateVersion.toInt()
        ) ?: run {
            logger.info(
                "template yaml version not found|" +
                        "$projectId|${templatePipelineVersion.templateId}|${templatePipelineVersion.templateVersion}"
            )
            return null
        }
        val pipelineVersionSimple = pipelineResourceVersionDao.getPipelineVersionSimple(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = pipelineVersion
        ) ?: run {
            logger.info("pipeline version not found|$projectId|$pipelineId|$pipelineVersion")
            return null
        }
        return PipelineYamlDependencyResult(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineVersion = pipelineVersion,
            pipelineVersionStatus = pipelineVersionSimple.status,
            branchAction = pipelineVersionSimple.branchAction,
            dependentFilePath = templateYamlVersion.filePath,
            dependentFileType = YamlFileType.TEMPLATE,
            dependentBlobId = templateYamlVersion.blobId,
            dependentRef = templatePipelineVersion.inputTemplateRef ?: Constansts.DEFAULT_DEPENDENT_REF,
            dependentCommitId = templateYamlVersion.commitId,
            dependentCommitTime = templateYamlVersion.commitTime
        )
    }

    fun save(
        transactionContext: DSLContext? = null,
        projectId: String,
        repoHashId: String,
        filePath: String,
        blobId: String,
        ref: String,
        dependencyResult: PipelineYamlDependencyResult
    ) {
        with(dependencyResult) {
            // 文件内容的依赖
            val blobDependency = PipelineYamlDependency(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                fileType = YamlFileType.PIPELINE,
                ref = blobId,
                refValueType = YamlRefValueType.BLOB_ID,
                dependentFilePath = dependencyResult.dependentFilePath,
                dependentFileType = dependencyResult.dependentFileType,
                dependentRef = dependencyResult.dependentRef,
            )
            pipelineYamlDependencyDao.save(
                dslContext = transactionContext ?: dslContext,
                record = blobDependency
            )
            // 记录当前流水线活跃的版本(最新的分支版本和最新的正式版本),跨分支引用的情况
            if (pipelineVersionStatus == VersionStatus.RELEASED ||
                (pipelineVersionStatus == VersionStatus.BRANCH && branchAction == BranchVersionAction.ACTIVE) &&
                (dependentRef != Constansts.DEFAULT_DEPENDENT_REF && dependentRef != ref)
            ) {
                // 分支交叉依赖
                val acrossDependency = PipelineYamlDependency(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    fileType = YamlFileType.PIPELINE,
                    ref = ref,
                    refValueType = YamlRefValueType.BRANCH,
                    dependentFilePath = dependencyResult.dependentFilePath,
                    dependentFileType = dependencyResult.dependentFileType,
                    dependentRef = dependencyResult.dependentRef,
                )
                pipelineYamlDependencyDao.save(
                    dslContext = transactionContext ?: dslContext,
                    record = acrossDependency
                )
            }
        }
    }

    /**
     * 分析变更文件中的依赖
     */
    fun analyzeDiffDependencies(
        projectId: String,
        repoHashId: String,
        eventId: Long,
        yamlDiffs: List<PipelineYamlDiff>
    ): List<PipelineYamlDiff> {
        // 模版文件有变更时,才计算依赖
        val templateDiffs = yamlDiffs.filter { it.fileType == YamlFileType.TEMPLATE && it.actionType.isChange() }
        // 模版文件没有变更,不需要判断依赖
        if (templateDiffs.isEmpty()) {
            return yamlDiffs
        }
        val diffDependencies = mutableListOf<PipelineYamlDiff>()
        analyzeDependencyForEvent(
            projectId = projectId,
            repoHashId = repoHashId,
            yamlDiffs = yamlDiffs,
            templateDiffs = templateDiffs,
            diffDependencies = diffDependencies
        )
        analyzeDiffForRef(
            projectId = projectId,
            repoHashId = repoHashId,
            eventId = eventId,
            templateDiffFiles = templateDiffs,
            diffDependencies = diffDependencies
        )
        return diffDependencies
    }

    /**
     * 分析当前事件的依赖
     *
     * -
     */
    private fun analyzeDependencyForEvent(
        projectId: String,
        repoHashId: String,
        yamlDiffs: List<PipelineYamlDiff>,
        templateDiffs: List<PipelineYamlDiff>,
        diffDependencies: MutableList<PipelineYamlDiff>
    ) {
        yamlDiffs.forEach { diff ->
            if (diff.actionType == YamlFileActionType.TRIGGER) {
                analyzeTriggerAction(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    diff = diff,
                    templateDiffs = templateDiffs,
                    diffDependencies = diffDependencies
                )
            } else {
                diffDependencies.add(diff)
            }
        }
    }

    private fun analyzeTriggerAction(
        projectId: String,
        repoHashId: String,
        templateDiffs: List<PipelineYamlDiff>,
        diff: PipelineYamlDiff,
        diffDependencies: MutableList<PipelineYamlDiff>
    ) {
        val templateDiffMap = templateDiffs.associateBy { it.filePath }
        val dependency = pipelineYamlDependencyDao.getDependency(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = diff.filePath,
            ref = diff.blobId!!
        )
        // 没有依赖,直接添加
        if (dependency == null) {
            diffDependencies.add(diff)
            return
        }
        // 依赖的文件没有变更,直接添加
        val templateDiff = templateDiffMap[dependency.dependentFilePath]
        if (templateDiff == null) {
            diffDependencies.add(diff)
            return
        }
        // 依赖的分支是当前分支或者分支为默认,将触发改成依赖更新后再触发
        if (dependency.dependentRef == templateDiff.ref ||
            dependency.dependentRef == Constansts.DEFAULT_DEPENDENT_REF
        ) {
            diffDependencies.add(
                diff.copy(
                    actionType = YamlFileActionType.DEPENDENCY_UPGRADE_AND_TRIGGER,
                    dependentFilePath = templateDiff.filePath
                )
            )
        } else {
            diffDependencies.add(diff)
        }
    }

    /**
     * 分析当前分支的依赖
     */
    private fun analyzeDiffForRef(
        projectId: String,
        repoHashId: String,
        eventId: Long,
        templateDiffFiles: List<PipelineYamlDiff>,
        diffDependencies: MutableList<PipelineYamlDiff>
    ) {
        templateDiffFiles.forEach template@{ templateDiff ->
            // 获取所有依赖当前模版文件的分支
            pipelineYamlDependencyDao.listRefDependency(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                dependentFilePath = templateDiff.filePath,
                dependentRef = templateDiff.ref
            ).forEach dependency@{ dependency ->
                // 分支版本对应的最新版本信息,如果版本不存在,则跳过
                val pipelineYamlVersion = pipelineYamlVersionDao.getPipelineYamlVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = dependency.filePath,
                    ref = dependency.ref,
                    branchAction = BranchVersionAction.ACTIVE.name
                ) ?: run {
                    logger.info(
                        "pipeline yaml version not found|$projectId|$repoHashId|" +
                                "${dependency.filePath}|${dependency.ref}"
                    )
                    return@dependency
                }
                val dependencyDiff = PipelineYamlDiff(
                    projectId = projectId,
                    eventId = eventId,
                    eventType = templateDiff.eventType,
                    repoHashId = repoHashId,
                    defaultBranch = templateDiff.defaultBranch,
                    filePath = dependency.filePath,
                    fileType = dependency.fileType,
                    actionType = YamlFileActionType.DEPENDENCY_UPGRADE,
                    triggerUser = templateDiff.triggerUser,
                    ref = dependency.ref,
                    blobId = pipelineYamlVersion.blobId,
                    commitId = pipelineYamlVersion.commitId,
                    commitTime = pipelineYamlVersion.commitTime,
                    dependentFilePath = templateDiff.filePath
                )
                diffDependencies.add(dependencyDiff)
            }
        }
    }


    /**
     * 获取文件动态依赖项
     */
    fun getDependency(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String
    ): PipelineYamlDependency? {
        return pipelineYamlDependencyDao.getDependency(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlDependencyService::class.java)
    }
}
