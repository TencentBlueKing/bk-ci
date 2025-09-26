package com.tencent.devops.process.dao.yaml

import com.tencent.devops.model.process.tables.TPipelineYamlDiff
import com.tencent.devops.model.process.tables.records.TPipelineYamlDiffRecord
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.enums.YamDiffFileStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineYamlDiffDao {

    fun batchSave(
        dslContext: DSLContext,
        yamlDiffs: List<PipelineYamlDiff>
    ) {
        val now = LocalDateTime.now()
        with(TPipelineYamlDiff.T_PIPELINE_YAML_DIFF) {
            val records = yamlDiffs.map { diff ->
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    EVENT_ID,
                    EVENT_TYPE,
                    REPO_HASH_ID,
                    DEFAULT_BRANCH,
                    FILE_PATH,
                    FILE_PATH_MD5,
                    FILE_TYPE,
                    ACTION_TYPE,
                    STATUS,
                    TRIGGER_USER,
                    REF,
                    BLOB_ID,
                    COMMIT_ID,
                    COMMIT_TIME,
                    COMMIT_MSG,
                    COMMITTER,
                    FORK,
                    USE_FORK_TOKEN,
                    MERGED,
                    PULL_REQUEST_ID,
                    PULL_REQUEST_NUMBER,
                    PULL_REQUEST_URL,
                    SOURCE_BRANCH,
                    TARGET_BRANCH,
                    SOURCE_REPO_URL,
                    SOURCE_FULL_NAME,
                    TARGET_REPO_URL,
                    TARGET_FULL_NAME,
                    OLD_FILE_PATH,
                    DEPENDENT_FILE_PATH,
                    DEPENDENT_REF,
                    DEPENDENT_BLOB_ID,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    diff.projectId,
                    diff.eventId,
                    diff.eventType,
                    diff.repoHashId,
                    diff.defaultBranch,
                    diff.filePath,
                    DigestUtils.md5Hex(diff.filePath),
                    diff.fileType.name,
                    diff.actionType.name,
                    diff.status.name,
                    diff.triggerUser,
                    diff.ref,
                    diff.blobId,
                    diff.commitId,
                    diff.commitTime,
                    diff.commitMsg,
                    diff.committer,
                    diff.fork,
                    diff.useForkToken,
                    diff.merged,
                    diff.pullRequestId,
                    diff.pullRequestNumber,
                    diff.pullRequestUrl,
                    diff.sourceBranch,
                    diff.targetBranch,
                    diff.sourceRepoUrl,
                    diff.sourceFullName,
                    diff.targetRepoUrl,
                    diff.targetFullName,
                    diff.oldFilePath,
                    diff.dependentFilePath,
                    diff.dependentRef,
                    diff.dependentBlobId,
                    now,
                    now,
                )
            }
            dslContext.batch(records).execute()
        }
    }

    fun listYamlDiffs(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long
    ): List<PipelineYamlDiff> {
        return with(TPipelineYamlDiff.T_PIPELINE_YAML_DIFF) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(EVENT_ID.eq(eventId))
                .fetch(mapper)
        }
    }

    fun getYamlDiff(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long,
        filePath: String,
        ref: String
    ): PipelineYamlDiff? {
        return with(TPipelineYamlDiff.T_PIPELINE_YAML_DIFF) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(EVENT_ID.eq(eventId))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .and(REF.eq(ref))
                .fetchOne(mapper)
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long,
        filePath: String,
        ref: String,
        status: YamDiffFileStatus,
        errorMsg: String? = null
    ) {
        with(TPipelineYamlDiff.T_PIPELINE_YAML_DIFF) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .let { if (!errorMsg.isNullOrBlank()) it.set(ERROR_MSG, errorMsg) else it }
                .where(PROJECT_ID.eq(projectId))
                .and(EVENT_ID.eq(eventId))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .and(REF.eq(ref))
                .execute()
        }
    }

    class PipelineYamlDiffMapper: RecordMapper<TPipelineYamlDiffRecord, PipelineYamlDiff> {
        override fun map(record: TPipelineYamlDiffRecord?): PipelineYamlDiff? {
            return record?.let {
                PipelineYamlDiff(
                    projectId = it.projectId,
                    eventId = it.eventId,
                    eventType = it.eventType,
                    repoHashId = it.repoHashId,
                    filePath = it.filePath,
                    fileType = YamlFileType.valueOf(it.fileType),
                    actionType = YamlFileActionType.valueOf(it.actionType),
                    status = YamDiffFileStatus.valueOf(it.status),
                    triggerUser = it.triggerUser,
                    defaultBranch = it.defaultBranch,
                    ref = it.ref,
                    blobId = it.blobId,
                    commitId = it.commitId,
                    commitTime = it.commitTime,
                    commitMsg = it.commitMsg,
                    committer = it.committer,
                    pullRequestId = it.pullRequestId,
                    pullRequestNumber = it.pullRequestNumber,
                    pullRequestUrl = it.pullRequestUrl,
                    sourceBranch = it.sourceBranch,
                    targetBranch = it.targetBranch,
                    merged = it.merged,
                    sourceRepoUrl = it.sourceRepoUrl,
                    sourceFullName = it.sourceFullName,
                    targetRepoUrl = it.targetRepoUrl,
                    targetFullName = it.targetFullName,
                    fork = it.fork,
                    oldFilePath = it.oldFilePath,
                    dependentFilePath = it.dependentFilePath,
                    dependentRef = it.dependentRef,
                    dependentBlobId = it.dependentBlobId
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineYamlDiffMapper()
    }
}
