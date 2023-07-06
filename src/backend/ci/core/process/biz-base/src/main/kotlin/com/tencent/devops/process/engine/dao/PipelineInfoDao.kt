/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.model.process.Tables.T_PIPELINE_INFO
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.OrderField
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.Result
import org.jooq.SortField
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Suppress("TooManyFunctions", "LongParameterList")
@Repository
class PipelineInfoDao {

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String,
        version: Int,
        pipelineName: String,
        pipelineDesc: String,
        userId: String,
        channelCode: ChannelCode,
        manualStartup: Boolean,
        canElementSkip: Boolean,
        taskCount: Int,
        id: Long? = null
    ): Int {
        val count = with(T_PIPELINE_INFO) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                PROJECT_ID,
                VERSION,
                PIPELINE_NAME,
                PIPELINE_DESC,
                CREATE_TIME,
                UPDATE_TIME,
                CHANNEL,
                CREATOR,
                LAST_MODIFY_USER,
                MANUAL_STARTUP,
                ELEMENT_SKIP,
                TASK_COUNT,
                ID
            )
                .values(
                    pipelineId,
                    projectId,
                    version,
                    pipelineName,
                    pipelineDesc,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    channelCode.name, userId, userId,
                    if (manualStartup) 1 else 0,
                    if (canElementSkip) 1 else 0,
                    taskCount,
                    id
                )
                .execute()
        }
        logger.info("Create the pipeline $pipelineId result=${count == 1}")
        return version
    }

    @Suppress("ComplexMethod")
    fun update(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String?,
        updateVersion: Boolean = true,
        pipelineName: String? = null,
        pipelineDesc: String? = null,
        manualStartup: Boolean? = null,
        canElementSkip: Boolean? = null,
        taskCount: Int = 0,
        latestVersion: Int = 0,
        updateLastModifyUser: Boolean? = true
    ): Int {
        val count = with(T_PIPELINE_INFO) {

            val update = dslContext.update(this)

            if (updateVersion) { // 刷新版本号，每次递增1
                update.set(VERSION, VERSION + 1)
            }

            if (!pipelineName.isNullOrBlank()) {
                update.set(PIPELINE_NAME, pipelineName)
            }
            if (!pipelineDesc.isNullOrBlank()) {
                update.set(PIPELINE_DESC, pipelineDesc)
            }
            if (manualStartup != null) {
                update.set(MANUAL_STARTUP, if (manualStartup) 1 else 0)
            }
            if (canElementSkip != null) {
                update.set(ELEMENT_SKIP, if (canElementSkip) 1 else 0)
            }
            if (taskCount > 0) {
                update.set(TASK_COUNT, taskCount)
            }
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(PIPELINE_ID.eq(pipelineId))
            if (latestVersion > 0) {
                conditions.add(VERSION.eq(latestVersion))
            }
            if (userId != null && updateLastModifyUser == true) {
                update.set(LAST_MODIFY_USER, userId)
            }
            update.set(UPDATE_TIME, LocalDateTime.now())
                .where(conditions)
                .execute()
        }
        if (count < 1) {
            logger.warn("Update the pipeline $pipelineId with the latest version($latestVersion) failed")
            // 版本号为0则为更新失败, 异常在业务层抛出, 只有pipelineId和version不符合的情况会走这里, 统一成一个异常应该問題ありません
            return 0
        }
        val version = with(T_PIPELINE_INFO) {
            dslContext.select(VERSION)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchOne(0, Int::class.java)!!
        }
        logger.info(
            "Update the pipeline $pipelineId add new version($version) old version($latestVersion) " +
                "and result=${count == 1}"
        )
        return version
    }

    fun countByPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        channelCode: ChannelCode,
        pipelineIds: List<String>
    ): Int {
        return with(T_PIPELINE_INFO) {
            dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .and(CHANNEL.eq(channelCode.name))
                .and(DELETE.eq(false))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByProjectIds(
        dslContext: DSLContext,
        projectIds: Collection<String>,
        channelCode: ChannelCode? = null,
        keyword: String? = null
    ): Int {
        return with(T_PIPELINE_INFO) {
            val query = dslContext.selectCount().from(this)
                .where(PROJECT_ID.`in`(projectIds))

            if (channelCode != null) {
                query.and(CHANNEL.eq(channelCode.name))
            }

            if (!keyword.isNullOrBlank()) {
                query.and(PIPELINE_NAME.like("%$keyword%"))
            }

            query.and(DELETE.eq(false)).fetchOne(0, Int::class.java)!!
        }
    }

    fun countByProjectIds(
        dslContext: DSLContext,
        projectIds: Collection<String>,
        channelCodes: List<ChannelCode>? = null,
        keyword: String? = null
    ): Int {
        return with(T_PIPELINE_INFO) {
            val query = dslContext.selectCount().from(this)
                .where(PROJECT_ID.`in`(projectIds))

            if (channelCodes != null) {
                query.and(CHANNEL.`in`(channelCodes))
            }

            if (!keyword.isNullOrBlank()) {
                query.and(PIPELINE_NAME.like("%$keyword%"))
            }

            query.and(DELETE.eq(false)).fetchOne(0, Int::class.java)!!
        }
    }

    fun listPipelineIdByProject(dslContext: DSLContext, projectId: String): List<String> {
        return with(T_PIPELINE_INFO) {
            dslContext.select(PIPELINE_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch(PIPELINE_ID, String::class.java)
        }
    }

    fun listPipelineInfoByProject(
        dslContext: DSLContext,
        projectId: String? = null,
        limit: Int,
        offset: Int,
        deleteFlag: Boolean? = false,
        timeDescFlag: Boolean = true,
        channelCode: ChannelCode? = null
    ): Result<TPipelineInfoRecord>? {
        return with(T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (null != deleteFlag) {
                conditions.add(DELETE.eq(deleteFlag))
            }
            if (null != channelCode) {
                conditions.add(CHANNEL.eq(channelCode.name))
            }
            val baseQuery = dslContext.selectFrom(this).where(conditions)
            if (timeDescFlag) {
                baseQuery.orderBy(CREATE_TIME.desc(), PIPELINE_ID)
            } else {
                baseQuery.orderBy(CREATE_TIME.asc(), PIPELINE_ID)
            }
            baseQuery.limit(limit).offset(offset).fetch()
        }
    }

    fun searchByProject(
        dslContext: DSLContext,
        pipelineName: String?,
        projectCode: String,
        limit: Int,
        offset: Int,
        channelCode: ChannelCode? = ChannelCode.BS
    ): Result<TPipelineInfoRecord>? {
        return with(T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectCode))
            conditions.add(DELETE.eq(false))
            if (!pipelineName.isNullOrEmpty()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            conditions.add(CHANNEL.eq(channelCode!!.name))
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc(), PIPELINE_ID)
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun countPipelineInfoByProject(dslContext: DSLContext, pipelineName: String?, projectCode: String): Int {
        return with(T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectCode))
            conditions.add(DELETE.eq(false))
            if (!pipelineName.isNullOrEmpty()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun searchByProject(dslContext: DSLContext, projectId: String): Result<TPipelineInfoRecord>? {
        return with(T_PIPELINE_INFO) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(DELETE.eq(false))
                .fetch()
        }
    }

    @SuppressWarnings("ComplexMethod")
    fun listDeletePipelineIdByProject(
        dslContext: DSLContext,
        projectId: String,
        days: Long?,
        offset: Int? = null,
        limit: Int? = null,
        sortType: PipelineSortType,
        collation: PipelineCollation
    ): Result<TPipelineInfoRecord>? {
        with(T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(DELETE.eq(true))
            if (days != null) {
                conditions.add(UPDATE_TIME.greaterOrEqual(LocalDateTime.now().minusDays(days)))
            }
            return dslContext
                .selectFrom(this)
                .where(conditions)
                .let {
                    val st = when (sortType) {
                        PipelineSortType.UPDATE_TIME -> transferOrder(
                            UPDATE_TIME,
                            collation
                        )
                        PipelineSortType.NAME -> transferOrder(
                            PIPELINE_NAME,
                            collation
                        )
                        else -> transferOrder(
                            CREATE_TIME,
                            collation
                        )
                    }
                    it.orderBy(st)
                }
                .let { if (offset != null && limit != null) it.limit(offset, limit) else it }
                .fetch()
        }
    }

    private fun <T> transferOrder(
        field: Field<T>,
        collation: PipelineCollation,
        desc: ((field: Field<T>) -> SortField<T>) = { it.desc() },
        asc: ((field: Field<T>) -> SortField<T>) = { it.asc() }
    ): OrderField<T> {
        return if (collation == PipelineCollation.DEFAULT || collation == PipelineCollation.DESC) {
            desc(field)
        } else {
            asc(field)
        }
    }

    fun countDeletePipeline(
        dslContext: DSLContext,
        projectId: String,
        days: Long?
    ): Int {
        with(T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(DELETE.eq(true))
            if (days != null) {
                conditions.add(UPDATE_TIME.greaterOrEqual(LocalDateTime.now().minusDays(days)))
            }
            return dslContext
                .selectCount().from(this)
                .where(conditions)
                .fetchOne()?.value1() ?: 0
        }
    }

    /**
     * 查找updateTime之前被删除的流水线
     */
    @Suppress("ComplexCondition", "unused")
    fun listDeletePipelineBefore(
        dslContext: DSLContext,
        updateTime: LocalDateTime,
        offset: Int?,
        limit: Int?
    ): Result<TPipelineInfoRecord>? {
        with(T_PIPELINE_INFO) {
            val baseQuery = dslContext.selectFrom(this)
                .where(DELETE.eq(true))
                .and(UPDATE_TIME.le(updateTime))
            return if (offset != null && offset >= 0 && limit != null && limit >= 0) {
                baseQuery.limit(offset, limit).fetch()
            } else {
                baseQuery.fetch()
            }
        }
    }

    fun isNameExist(dslContext: DSLContext, projectId: String, pipelineName: String, channelCode: ChannelCode) =
        isNameExist(dslContext, projectId, pipelineName, channelCode, null)

    fun isNameExist(
        dslContext: DSLContext,
        projectId: String,
        pipelineName: String,
        channelCode: ChannelCode,
        excludePipelineId: String?
    ): Boolean {
        return with(T_PIPELINE_INFO) {
            val where = dslContext.select(PIPELINE_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))

            if (excludePipelineId != null) {
                where.and(PIPELINE_ID.notEqual(excludePipelineId))
            }

            where.and(CHANNEL.eq(channelCode.name))
                .and(PIPELINE_NAME.eq(pipelineName))
                .and(DELETE.eq(false)).fetch().isNotEmpty
        }
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode? = null
    ): TPipelineInfoRecord? {
        return with(T_PIPELINE_INFO) {
            val query = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            if (channelCode != null) {
                query.and(CHANNEL.eq(channelCode.name))
            }
            query.and(DELETE.eq(false)).fetchAny()
        }
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode? = null,
        delete: Boolean? = false,
        days: Long? = null // 搜索范围：{days}天内的流水线
    ): TPipelineInfoRecord? {
        return with(T_PIPELINE_INFO) {
            val query = if (!projectId.isBlank()) {
                dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
            } else {
                dslContext.selectFrom(this).where(PIPELINE_ID.eq(pipelineId))
            }

            if (channelCode != null) {
                query.and(CHANNEL.eq(channelCode.name))
            }

            if (days != null && days > 0) {
                query.and(UPDATE_TIME.greaterOrEqual(LocalDateTime.now().minusDays(days)))
            }
            if (delete != null) {
                query.and(DELETE.eq(delete))
            }
            query.fetchAny()
        }
    }

    fun softDelete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        changePipelineName: String,
        userId: String,
        channelCode: ChannelCode?
    ): Int {
        return with(T_PIPELINE_INFO) {
            val update = dslContext.update(this).set(DELETE, true)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_MODIFY_USER, userId)
                .set(PIPELINE_NAME, changePipelineName)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            if (channelCode != null) {
                update.and(CHANNEL.eq(channelCode.name))
            }
            update.execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        return with(T_PIPELINE_INFO) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun listInfoByPipelineIds(
        dslContext: DSLContext,
        projectId: String? = null,
        pipelineIds: Set<String>,
        filterDelete: Boolean = true
    ): Result<TPipelineInfoRecord> {
        return with(T_PIPELINE_INFO) {
            val query =
                if (projectId.isNullOrBlank()) {
                    dslContext.selectFrom(this).where(PIPELINE_ID.`in`(pipelineIds))
                } else {
                    dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(PIPELINE_ID.`in`(pipelineIds))
                }
            if (filterDelete) query.and(DELETE.eq(false))
            query.fetch()
        }
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): TPipelineInfoRecord {
        return with(T_PIPELINE_INFO) {
            val query = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(VERSION.eq(version))
            query.fetch().first()
        }
    }

    fun getPipelineInfoNum(
        dslContext: DSLContext,
        projectIds: Set<String>?,
        channelCodes: Set<ChannelCode>?
    ): Record1<Int>? {
        val conditions = mutableListOf<Condition>()
        conditions.add(T_PIPELINE_INFO.DELETE.eq(false))
        if (projectIds != null && projectIds.isNotEmpty()) {
            conditions.add(T_PIPELINE_INFO.PROJECT_ID.`in`(projectIds))
        }
        if (channelCodes != null && channelCodes.isNotEmpty()) {
            conditions.add(T_PIPELINE_INFO.CHANNEL.`in`(channelCodes.map { it.name }))
        }
        return dslContext.select(DSL.count(T_PIPELINE_INFO.PROJECT_ID)).from(T_PIPELINE_INFO)
            .where(conditions).fetch().first()
    }

    fun listInfoByPipelineName(
        dslContext: DSLContext,
        projectId: String,
        pipelineNames: Set<String>,
        filterDelete: Boolean = true
    ): Result<TPipelineInfoRecord> {
        return with(T_PIPELINE_INFO) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_NAME.`in`(pipelineNames))
            if (filterDelete) query.and(DELETE.eq(false))
            query.fetch()
        }
    }

    fun convert(t: TPipelineInfoRecord?, templateId: String?): PipelineInfo? {
        return if (t != null) {
            with(t) {
                PipelineInfo(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    templateId = templateId,
                    pipelineName = pipelineName,
                    pipelineDesc = pipelineDesc,
                    version = version,
                    createTime = createTime.timestampmilli(),
                    updateTime = updateTime.timestampmilli(),
                    creator = creator,
                    lastModifyUser = lastModifyUser,
                    channelCode = ChannelCode.valueOf(channel),
                    canManualStartup = manualStartup == 1,
                    canElementSkip = elementSkip == 1,
                    taskCount = taskCount,
                    id = id
                )
            }
        } else {
            null
        }
    }

    fun restore(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        channelCode: ChannelCode
    ) {
        return with(T_PIPELINE_INFO) {
            dslContext.update(this).set(DELETE, false)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_MODIFY_USER, userId)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(CHANNEL.eq(channelCode.name))
                .execute()
        }
    }

    fun searchByProjectId(
        dslContext: DSLContext,
        pipelineName: String?,
        projectCode: String,
        limit: Int,
        offset: Int,
        channelCodes: List<ChannelCode>
    ): Result<TPipelineInfoRecord>? {
        return with(T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectCode))
            conditions.add(DELETE.eq(false))

            if (!pipelineName.isNullOrEmpty()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            conditions.add(CHANNEL.`in`(channelCodes))
            dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc(), PIPELINE_ID)
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getPipelineVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        channelCode: ChannelCode
    ): Int {
        return with(T_PIPELINE_INFO) {
            dslContext.select(this.VERSION)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(CHANNEL.eq(channelCode.name))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun listByProject(dslContext: DSLContext, projectId: String): Result<Record2<String, Long>> {
        return with(T_PIPELINE_INFO) {
            dslContext.select(PIPELINE_ID.`as`("pipelineId"), ID.`as`("id")).from(this)
                .where(PROJECT_ID.eq(projectId).and(DELETE.eq(false))).fetch()
        }
    }

    fun getPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineInfoRecord? {
        return with(T_PIPELINE_INFO) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetchAny()
        }
    }

    fun countExcludePipelineIds(
        dslContext: DSLContext,
        projectId: String,
        excludePipelineIds: List<String>,
        channelCode: ChannelCode? = null,
        includeDelete: Boolean = false
    ): Int {
        with(T_PIPELINE_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.notIn(excludePipelineIds))
                .let { if (channelCode == null) it else it.and(CHANNEL.eq(channelCode.name)) }
                .let { if (includeDelete) it else it.and(DELETE.eq(false)) }
                .fetchOne()?.value1() ?: 0
        }
    }

    fun getPipelineByAutoId(
        dslContext: DSLContext,
        ids: List<Long>,
        projectId: String? = null
    ): Result<TPipelineInfoRecord> {
        return with(T_PIPELINE_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ID.`in`(ids))
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun updateLatestStartTime(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        startTime: LocalDateTime
    ) {
        with(T_PIPELINE_INFO) {
            dslContext.update(this)
                .set(LATEST_START_TIME, startTime)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun getIdByCreateTimePeriod(
        dslContext: DSLContext,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        page: Int,
        pageSize: Int
    ): List<String> {
        with(T_PIPELINE_INFO) {
            return dslContext.select(PIPELINE_ID)
                .from(this)
                .where(CREATE_TIME.between(startTime, endTime))
                .orderBy(CREATE_TIME, PIPELINE_ID)
                .limit((page - 1) * pageSize, pageSize)
                .fetchInto(String::class.java)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineInfoDao::class.java)
    }
}
