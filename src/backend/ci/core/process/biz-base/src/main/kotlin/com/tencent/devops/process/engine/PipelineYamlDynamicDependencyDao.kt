package com.tencent.devops.process.engine

import com.tencent.devops.model.process.tables.TPipelineYamlDynamicDependency
import com.tencent.devops.model.process.tables.records.TPipelineYamlDynamicDependencyRecord
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDynamicDependency
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineYamlDynamicDependencyDao {

    fun save(
        dslContext: DSLContext,
        record: PipelineYamlDynamicDependency
    ) {
        val now = LocalDateTime.now()
        with(TPipelineYamlDynamicDependency.T_PIPELINE_YAML_DYNAMIC_DEPENDENCY) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                REPO_HASH_ID,
                FILE_PATH,
                FILE_TYPE,
                BLOB_ID,
                REF,
                COMMIT_ID,
                COMMIT_TIME,
                DEPENDENT_FILE_PATH,
                DEPENDENT_FILE_TYPE,
                DEPENDENT_REF,
                DEPENDENT_BLOB_ID,
                DEPENDENT_COMMIT_ID,
                DEPENDENT_COMMIT_TIME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                record.id,
                record.projectId,
                record.repoHashId,
                record.filePath,
                record.fileType.name,
                record.blobId,
                record.ref,
                record.commitId,
                record.commitTime,
                record.dependentFilePath,
                record.dependentFileType.name,
                record.dependentRef,
                record.dependentBlobId,
                record.dependentCommitId,
                record.dependentCommitTime,
                now,
                now
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        ref: String,
        filePath: String
    ) {
        with(TPipelineYamlDynamicDependency.T_PIPELINE_YAML_DYNAMIC_DEPENDENCY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH.eq(filePath))
                .and(REF.eq(ref))
        }
    }

    fun getDependency(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String,
        blobId: String? = null,
        ref: String? = null
    ): PipelineYamlDynamicDependency? {
        with(TPipelineYamlDynamicDependency.T_PIPELINE_YAML_DYNAMIC_DEPENDENCY) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH.eq(filePath))
            if (!blobId.isNullOrBlank()) {
                query.and(BLOB_ID.eq(blobId))
            }
            if (!ref.isNullOrBlank()) {
                query.and(REF.eq(ref))
            }
            query.orderBy(DEPENDENT_COMMIT_TIME.desc()).limit(1)
            return query.fetchOne(mapper)
        }
    }

    class PipelineYamlDynamicDependencyMapper :
        RecordMapper<TPipelineYamlDynamicDependencyRecord, PipelineYamlDynamicDependency> {
        override fun map(record: TPipelineYamlDynamicDependencyRecord?): PipelineYamlDynamicDependency? {
            return record?.let {
                PipelineYamlDynamicDependency(
                    id = it.id,
                    projectId = it.projectId,
                    repoHashId = it.repoHashId,
                    filePath = it.filePath,
                    fileType = YamlFileType.valueOf(it.fileType),
                    blobId = it.blobId,
                    commitId = it.commitId,
                    commitTime = it.commitTime,
                    ref = it.ref,
                    dependentFilePath = it.dependentFilePath,
                    dependentFileType = YamlFileType.valueOf(it.dependentFileType),
                    dependentRef = it.dependentRef,
                    dependentBlobId = it.dependentBlobId,
                    dependentCommitId = it.dependentCommitId,
                    dependentCommitTime = it.dependentCommitTime
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineYamlDynamicDependencyMapper()
    }
}
