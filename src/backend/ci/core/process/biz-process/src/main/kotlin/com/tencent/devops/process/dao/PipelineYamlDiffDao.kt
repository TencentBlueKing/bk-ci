package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineYamlDiff
import com.tencent.devops.model.process.tables.records.TPipelineYamlDiffRecord
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.enums.YamDiffFileStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
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
            val records = yamlDiffs.map { event ->
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    EVENT_ID,
                    EVENT_TYPE,
                    REPO_HASH_ID,
                    FILE_PATH,
                    FILE_TYPE,
                    ACTION_TYPE,
                    STATUS,
                    REF,
                    BLOB_ID,
                    COMMIT_ID,
                    COMMIT_TIME,
                    COMMIT_MSG,
                    COMMITTER,
                    PULL_REQUEST_ID,
                    PULL_REQUEST_URL,
                    SOURCE_BRANCH,
                    TARGET_BRANCH,
                    MERGED,
                    SOURCE_REPO_URL,
                    SOURCE_FULL_NAME,
                    TARGET_REPO_URL,
                    TARGET_FULL_NAME,
                    FORK,
                    OLD_FILE_PATH,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    event.projectId,
                    event.eventId,
                    event.eventType,
                    event.repoHashId,
                    event.filePath,
                    event.fileType.name,
                    event.actionType.name,
                    event.status.name,
                    event.ref,
                    event.blobId,
                    event.commitId,
                    event.commitTime,
                    event.commitMsg,
                    event.committer,
                    event.pullRequestId,
                    event.pullRequestUrl,
                    event.sourceBranch,
                    event.targetBranch,
                    event.merged,
                    event.sourceRepoUrl,
                    event.sourceFullName,
                    event.targetRepoUrl,
                    event.targetFullName,
                    event.fork,
                    event.oldFilePath,
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
        filePath: String
    ): PipelineYamlDiff? {
        return with(TPipelineYamlDiff.T_PIPELINE_YAML_DIFF) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(EVENT_ID.eq(eventId))
                .and(FILE_PATH.eq(filePath))
                .fetchOne(mapper)
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        eventId: Long,
        filePath: String,
        status: YamDiffFileStatus
    ) {
        with(TPipelineYamlDiff.T_PIPELINE_YAML_DIFF) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(EVENT_ID.eq(eventId))
                .and(FILE_PATH.eq(filePath))
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
                    pullRequestUrl = it.pullRequestUrl,
                    sourceBranch = it.sourceBranch,
                    targetBranch = it.targetBranch,
                    merged = it.merged,
                    sourceRepoUrl = it.sourceRepoUrl,
                    sourceFullName = it.sourceFullName,
                    targetRepoUrl = it.targetRepoUrl,
                    targetFullName = it.targetFullName,
                    fork = it.fork,
                    oldFilePath = it.oldFilePath
                )
            }
        }
    }

    companion object {
        private val mapper = PipelineYamlDiffMapper()
    }
}
