/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.scanner.dao

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.ENQUEUED
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.EXECUTING
import com.tencent.bkrepo.common.scanner.pojo.scanner.SubScanTaskStatus.PULLED
import com.tencent.bkrepo.scanner.model.TSubScanTask
import com.tencent.bkrepo.scanner.pojo.request.CredentialsKeyFiles
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SubScanTaskDao(
    private val planArtifactLatestSubScanTaskDao: PlanArtifactLatestSubScanTaskDao
) : AbsSubScanTaskDao<TSubScanTask>() {

    fun findByCredentialsKeyAndSha256List(credentialsKeyFiles: List<CredentialsKeyFiles>): List<TSubScanTask> {
        val criteria = Criteria()
        credentialsKeyFiles.forEach {
            criteria.orOperator(
                Criteria
                    .where(TSubScanTask::credentialsKey.name).isEqualTo(it.credentialsKey)
                    .and(TSubScanTask::sha256.name).`in`(it.sha256List)
            )
        }
        return find(Query(criteria))
    }

    fun findByCredentialsAndSha256(credentialsKey: String?, sha256: String): TSubScanTask? {
        val query = Query(
            TSubScanTask::credentialsKey.isEqualTo(credentialsKey).and(TSubScanTask::sha256.name).isEqualTo(sha256)
        )
        return findOne(query)
    }

    fun deleteById(subTaskId: String): DeleteResult {
        val query = Query(Criteria.where(ID).isEqualTo(subTaskId))
        return remove(query)
    }

    /**
     * 更新任务状态
     *
     * @param subTaskId 待更新的子任务id
     * @param status 更新后的任务状态
     * @param oldStatus 更新前的任务状态，只有旧状态匹配时才会更新
     * @param lastModifiedDate 最后更新时间，用于充当乐观锁，只有最后修改时间匹配时候才更新
     * @param timeoutDateTime 扫描执行超时时间点
     *
     * @return 更新结果
     */
    fun updateStatus(
        subTaskId: String,
        status: SubScanTaskStatus,
        oldStatus: SubScanTaskStatus? = null,
        lastModifiedDate: LocalDateTime? = null,
        timeoutDateTime: LocalDateTime? = null
    ): UpdateResult {
        val now = LocalDateTime.now()
        val criteria = Criteria.where(ID).isEqualTo(subTaskId)

        oldStatus?.let { criteria.and(TSubScanTask::status.name).isEqualTo(it.name) }
        lastModifiedDate?.let { criteria.and(TSubScanTask::lastModifiedDate.name).isEqualTo(it) }

        val query = Query(criteria)
        val update = Update()
            .set(TSubScanTask::lastModifiedDate.name, now)
            .set(TSubScanTask::status.name, status.name)
        if (status == EXECUTING) {
            update.set(TSubScanTask::timeoutDateTime.name, timeoutDateTime)
            update.set(TSubScanTask::startDateTime.name, now)
            update.inc(TSubScanTask::executedTimes.name, 1)
        } else {
            update.unset(TSubScanTask::timeoutDateTime.name)
        }

        val updateResult = updateFirst(query, update)
        if (updateResult.modifiedCount == 1L) {
            logger.debug(
                "update status success, subTaskId[$subTaskId], newStatus[$status]," +
                    " oldStatus[$oldStatus], lastModifiedDate[$lastModifiedDate], newModifiedDate[$now]"
            )
            planArtifactLatestSubScanTaskDao.updateStatus(subTaskId, status.name, now = now)
        }

        return updateResult
    }

    fun countStatus(status: SubScanTaskStatus): Long {
        return count(Query(TSubScanTask::status.isEqualTo(status.name)))
    }

    /**
     * 唤醒[projectId]一个子任务为可执行状态
     */
    fun notify(projectId: String, count: Int = 1): UpdateResult? {
        if (count <= 0) {
            return null
        }

        val criteria = Criteria
            .where(TSubScanTask::projectId.name).isEqualTo(projectId)
            .and(TSubScanTask::status.name).isEqualTo(SubScanTaskStatus.BLOCKED.name)
        val subtaskIds = find(Query(criteria).limit(count)).map { it.id!! }

        if (subtaskIds.isEmpty()) {
            return null
        }

        criteria.and(ID).inValues(subtaskIds)
        val update = Update()
            .set(TSubScanTask::lastModifiedDate.name, LocalDateTime.now())
            .set(TSubScanTask::status.name, SubScanTaskStatus.CREATED.name)

        logger.info("notify subtasks$subtaskIds of project[$projectId]")
        return updateMulti(Query(criteria), update)
    }

    /**
     * 获取项目[projectId]扫描中的任务数量
     */
    fun scanningCount(projectId: String): Long {
        val criteria = Criteria
            .where(TSubScanTask::projectId.name).isEqualTo(projectId)
            .and(TSubScanTask::status.name).inValues(SubScanTaskStatus.RUNNING_STATUS)
        return count(Query(criteria))
    }

    fun updateStatus(
        subTaskIds: List<String>,
        status: SubScanTaskStatus
    ): UpdateResult {
        val query = Query(Criteria.where(ID).`in`(subTaskIds))
        val update = Update()
            .set(TSubScanTask::lastModifiedDate.name, LocalDateTime.now())
            .set(TSubScanTask::status.name, status.name)
        val updateResult = updateFirst(query, update)
        planArtifactLatestSubScanTaskDao.updateStatus(subTaskIds, status.name)
        return updateResult
    }

    fun firstTaskByStatusIn(status: List<String>): TSubScanTask? {
        val query = Query(TSubScanTask::status.inValues(status))
        return findOne(query)
    }

    /**
     * 获取一个执行超时的任务
     *
     * @param timeoutSeconds 允许执行的最长时间
     */
    fun firstTimeoutTask(timeoutSeconds: Long): TSubScanTask? {
        val now = LocalDateTime.now()

        val lastModifiedCriteria = Criteria
            .where(TSubScanTask::lastModifiedDate.name).lt(now.minusSeconds(timeoutSeconds))
            .and(TSubScanTask::timeoutDateTime.name).exists(false)

        val timeoutCriteria = Criteria().orOperator(
            TSubScanTask::timeoutDateTime.lt(now),
            lastModifiedCriteria
        )

        val criteria = Criteria().andOperator(
            timeoutCriteria,
            TSubScanTask::status.inValues(PULLED.name, ENQUEUED.name, EXECUTING.name)
        )

        return findOne(Query(criteria))
    }

    /**
     * 获取处于阻塞状态超时的任务
     */
    fun blockedTimeoutTasks(timeoutSeconds: Long): Page<TSubScanTask> {
        val now = LocalDateTime.now()
        val criteria = Criteria
            .where(TSubScanTask::lastModifiedDate.name).lt(now.minusSeconds(timeoutSeconds))
            .and(TSubScanTask::status.name).isEqualTo(SubScanTaskStatus.BLOCKED.name)
        return page(Query(criteria), Pages.ofRequest(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubScanTaskDao::class.java)
    }
}
