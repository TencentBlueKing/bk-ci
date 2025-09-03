package com.tencent.devops.process.dao.yaml

import com.tencent.devops.model.process.tables.TPipelineYamlDependency
import com.tencent.devops.model.process.tables.records.TPipelineYamlDependencyRecord
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDependency
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.pojo.pipeline.enums.YamlRefValueType
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineYamlDependencyDao {

    fun save(
        dslContext: DSLContext,
        record: PipelineYamlDependency
    ) {
        val now = LocalDateTime.now()
        with(TPipelineYamlDependency.T_PIPELINE_YAML_DEPENDENCY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                REPO_HASH_ID,
                FILE_PATH,
                FILE_PATH_MD5,
                FILE_TYPE,
                REF,
                REF_VALUE_TYPE,
                DEPENDENT_FILE_PATH,
                DEPENDENT_FILE_PATH_MD5,
                DEPENDENT_FILE_TYPE,
                DEPENDENT_REF,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                record.projectId,
                record.repoHashId,
                record.filePath,
                DigestUtils.md5Hex(record.filePath),
                record.fileType.name,
                record.ref,
                record.refValueType.name,
                record.dependentFilePath,
                DigestUtils.md5Hex(record.dependentFilePath),
                record.dependentFileType.name,
                record.dependentRef,
                now,
                now
            ).onDuplicateKeyUpdate()
                .set(DEPENDENT_FILE_PATH, record.dependentFilePath)
                .set(DEPENDENT_FILE_PATH_MD5, DigestUtils.md5Hex(record.dependentFilePath))
                .set(DEPENDENT_FILE_TYPE, record.dependentFileType.name)
                .set(DEPENDENT_REF, record.dependentRef)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String? = null
    ) {
        with(TPipelineYamlDependency.T_PIPELINE_YAML_DEPENDENCY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .let { if (ref != null) it.and(REF.eq(ref)) else it }
                .execute()
        }
    }

    fun getDependency(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String
    ): PipelineYamlDependency? {
        return with(TPipelineYamlDependency.T_PIPELINE_YAML_DEPENDENCY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .and(REF.eq(ref))
                .fetchOne(mapper)
        }
    }

    /**
     * 获取分支依赖列表
     */
    fun listRefDependency(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        dependentFilePath: String,
        dependentRefs: List<String>
    ): List<PipelineYamlDependency> {
        return with(TPipelineYamlDependency.T_PIPELINE_YAML_DEPENDENCY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(DEPENDENT_FILE_PATH_MD5.eq(DigestUtils.md5Hex(dependentFilePath)))
                .and(DEPENDENT_REF.`in`(dependentRefs))
                .and(REF_VALUE_TYPE.eq(YamlRefValueType.BRANCH.name))
                .fetch(mapper)
        }
    }

    class PipelineYamlDependencyMapper :
        RecordMapper<TPipelineYamlDependencyRecord, PipelineYamlDependency> {
        override fun map(record: TPipelineYamlDependencyRecord?): PipelineYamlDependency? {
            return record?.let {
                PipelineYamlDependency(
                    projectId = it.projectId,
                    repoHashId = it.repoHashId,
                    filePath = it.filePath,
                    fileType = YamlFileType.valueOf(it.fileType),
                    ref = it.ref,
                    refValueType = YamlRefValueType.valueOf(it.refValueType),
                    dependentFilePath = it.dependentFilePath,
                    dependentFileType = YamlFileType.valueOf(it.dependentFileType),
                    dependentRef = it.dependentRef
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineYamlDependencyMapper()
    }
}
