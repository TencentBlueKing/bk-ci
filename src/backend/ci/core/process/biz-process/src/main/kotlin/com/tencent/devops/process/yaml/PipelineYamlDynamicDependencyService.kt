package com.tencent.devops.process.yaml

import com.tencent.devops.process.engine.PipelineYamlDynamicDependencyDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDynamicDependency
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.pojo.template.TemplateRefType
import com.tencent.devops.process.service.template.v2.PipelineTemplatePipelineVersionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineYamlDynamicDependencyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineYamlDynamicDependencyDao: PipelineYamlDynamicDependencyDao,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val pipelineTemplatePipelineVersionService: PipelineTemplatePipelineVersionService
) {

    fun analyzeDynamicDependency(
        projectId: String,
        pipelineId: String,
        pipelineVersion: Int,
        repoHashId: String,
        filePath: String,
        blobId: String,
        commitId: String,
        commitTime: LocalDateTime,
        ref: String
    ): PipelineYamlDynamicDependency? {
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
        ) ?: return null
        return PipelineYamlDynamicDependency(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            fileType = YamlFileType.PIPELINE,
            blobId = blobId,
            commitId = commitId,
            commitTime = commitTime,
            ref = ref,
            dependentFilePath = templateYamlVersion.filePath,
            dependentFileType = YamlFileType.TEMPLATE,
            dependentBlobId = templateYamlVersion.blobId,
            dependentRef = templatePipelineVersion.inputTemplateRef ?: DEFAULT_REF,
            dependentCommitId = templateYamlVersion.commitId,
            dependentCommitTime = templateYamlVersion.commitTime
        )
    }

    /**
     * 获取文件动态依赖项
     */
    fun getDynamicDependency(
        projectId: String,
        repoHashId: String,
        filePath: String,
        blobId: String? = null,
        ref: String? = null
    ): PipelineYamlDynamicDependency? {
        return pipelineYamlDynamicDependencyDao.getDependency(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            blobId = blobId,
            ref = ref
        )
    }

    companion object {
        private const val DEFAULT_REF = "*"
    }
}
