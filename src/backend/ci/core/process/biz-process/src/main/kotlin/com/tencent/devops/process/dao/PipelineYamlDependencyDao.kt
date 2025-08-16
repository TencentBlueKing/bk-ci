package com.tencent.devops.process.dao

import com.tencent.devops.process.pojo.pipeline.PipelineYamlDependency
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineYamlDependencyDao {

    /**
     * 获取依赖列表
     */
    fun listDependencies(
        dslContext: DSLContext,
        projectId: String,
        filePath: String,
        blobId: String,
        dependencyRef: String? = null
    ): List<PipelineYamlDependency> {
        return emptyList()
    }
}
