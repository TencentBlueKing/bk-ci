/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.metrics.constant.Constants.BK_MAX_CREATE_TIME
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.pojo.`do`.PipelineLabelInfo
import com.tencent.devops.metrics.pojo.po.SaveProjectAtomRelationDataPO
import com.tencent.devops.metrics.pojo.qo.QueryProjectInfoQO
import com.tencent.devops.model.metrics.tables.TAtomOverviewData
import com.tencent.devops.model.metrics.tables.TErrorTypeDict
import com.tencent.devops.model.metrics.tables.TProjectAtom
import com.tencent.devops.model.metrics.tables.TProjectPipelineLabelInfo
import com.tencent.devops.model.metrics.tables.records.TProjectPipelineLabelInfoRecord
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class ProjectInfoDao {

    fun queryProjectAtomList(
        dslContext: DSLContext,
        projectId: String,
        page: Int,
        pageSize: Int,
        keyWord: String? = null
    ): List<AtomBaseInfoDO> {
        with(TProjectAtom.T_PROJECT_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (!keyWord.isNullOrBlank()) {
                conditions.add(ATOM_NAME.like("%$keyWord%"))
            }
            return dslContext.select(ATOM_CODE, ATOM_NAME)
                .from(this)
                .where(conditions)
                .orderBy(ATOM_CODE, ID)
                .limit((page - 1) * pageSize, pageSize)
                .fetchInto(AtomBaseInfoDO::class.java)
        }
    }

    fun queryProjectAtomCount(
        dslContext: DSLContext,
        projectId: String,
        keyWord: String? = null
    ): Long {
        with(TProjectAtom.T_PROJECT_ATOM) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            if (!keyWord.isNullOrBlank()) {
                conditions.add(ATOM_NAME.like("%$keyWord%"))
            }
            return dslContext.select(ATOM_CODE)
                .from(this)
                .where(conditions)
                .execute().toLong()
        }
    }

    fun queryProjectPipelineLabels(
        dslContext: DSLContext,
        queryCondition: QueryProjectInfoQO
    ): List<PipelineLabelInfo> {
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(queryCondition.projectId))
            if (!queryCondition.pipelineIds.isNullOrEmpty()) {
                conditions.add(PIPELINE_ID.`in`(queryCondition.pipelineIds))
            }
            if (!queryCondition.keyword.isNullOrBlank()) {
                conditions.add(LABEL_NAME.like("%${queryCondition.keyword}%"))
            }
            return dslContext.select(LABEL_ID, LABEL_NAME).from(this)
                .where(conditions)
                .groupBy(LABEL_ID)
                .orderBy(LABEL_ID)
                .limit((queryCondition.page - 1) * queryCondition.pageSize, queryCondition.pageSize)
                .fetchInto(PipelineLabelInfo::class.java)
        }
    }

    fun queryProjectPipelineLabelsCount(
        dslContext: DSLContext,
        queryCondition: QueryProjectInfoQO
    ): Long {
        val conditions = mutableListOf<Condition>()
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {

            if (!queryCondition.pipelineIds.isNullOrEmpty()) {
                conditions.add(PIPELINE_ID.`in`(queryCondition.pipelineIds))
            }
            conditions.add(PROJECT_ID.eq(queryCondition.projectId))
            if (!queryCondition.keyword.isNullOrBlank()) {
                conditions.add(LABEL_NAME.like("%${queryCondition.keyword}%"))
            }
            return dslContext.selectDistinct(LABEL_ID).from(this)
                .where(conditions)
                .execute().toLong()
        }
    }

    fun queryPipelineErrorTypes(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int,
        keyWord: String?
    ): List<Int> {
        with(TErrorTypeDict.T_ERROR_TYPE_DICT) {
            val conditions = mutableListOf<Condition>()
            if (!keyWord.isNullOrBlank()) {
                conditions.add(this.NAME.like("%$keyWord%"))
            }
            val step = dslContext.select(
                ERROR_TYPE
            ).from(this)
                .where(conditions)
                .groupBy(ERROR_TYPE)
                .orderBy(ERROR_TYPE)
                .limit((page - 1) * pageSize, pageSize)
            return step.fetchInto(Int::class.java)
        }
    }

    fun queryPipelineErrorTypeCount(dslContext: DSLContext, keyWord: String?): Long {
        with(TErrorTypeDict.T_ERROR_TYPE_DICT) {
            val conditions = mutableListOf<Condition>()
            if (!keyWord.isNullOrBlank()) {
                conditions.add(this.NAME.like("%$keyWord%"))
            }
            return dslContext.selectDistinct(ERROR_TYPE)
                .from(this)
                .where(conditions)
                .execute().toLong()
        }
    }

    fun batchCreatePipelineLabelData(
        dslContext: DSLContext,
        pipelineLabelRelateInfos: List<TProjectPipelineLabelInfoRecord>
    ) {
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {
            pipelineLabelRelateInfos.forEach {
                dslContext.insertInto(this)
                    .set(ID, it.id)
                    .set(PROJECT_ID, it.projectId)
                    .set(PIPELINE_ID, it.pipelineId)
                    .set(LABEL_ID, it.labelId)
                    .set(LABEL_NAME, it.labelName)
                    .set(CREATOR, it.creator)
                    .set(MODIFIER, it.modifier)
                    .set(UPDATE_TIME, it.updateTime)
                    .set(CREATE_TIME, it.createTime)
                    .onDuplicateKeyUpdate()
                    .set(LABEL_NAME, it.labelName)
                    .set(MODIFIER, it.modifier)
                    .set(UPDATE_TIME, it.updateTime)
                    .execute()
            }
        }
    }

    fun batchDeletePipelineLabelData(
        dslContext: DSLContext,
        pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
    ) {
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {

            return dslContext.batched {
                pipelineLabelRelateInfos.forEach { pipelineLabelRelateInfo ->
                    val conditions = mutableListOf<Condition>()
                    conditions.add(this.PROJECT_ID.eq(pipelineLabelRelateInfo.projectId))
                    if (!pipelineLabelRelateInfo.pipelineId.isNullOrBlank()) {
                        conditions.add(this.PIPELINE_ID.eq(pipelineLabelRelateInfo.pipelineId))
                    }
                    if (pipelineLabelRelateInfo.labelId != null) {
                        conditions.add(LABEL_ID.eq(pipelineLabelRelateInfo.labelId))
                    }
                    it.dsl().deleteFrom(this)
                        .where(conditions)
                        .execute()
                }
            }
        }
    }

    fun batchUpdatePipelineLabelData(
        dslContext: DSLContext,
        userId: String,
        statisticsTime: LocalDateTime,
        pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
    ) {
        val conditions = mutableListOf<Condition>()
        with(TProjectPipelineLabelInfo.T_PROJECT_PIPELINE_LABEL_INFO) {
            return dslContext.batched {
                pipelineLabelRelateInfos.forEach { pipelineLabelRelateInfo ->
                    if (!pipelineLabelRelateInfo.pipelineId.isNullOrBlank()) {
                        conditions.add(this.PIPELINE_ID.eq(pipelineLabelRelateInfo.pipelineId))
                    }
                    conditions.add(this.PROJECT_ID.eq(pipelineLabelRelateInfo.projectId))
                    if (pipelineLabelRelateInfo.labelId != null) {
                        conditions.add(LABEL_ID.eq(pipelineLabelRelateInfo.labelId))
                    }
                    it.dsl().update(this)
                        .set(this.LABEL_NAME, pipelineLabelRelateInfo.name)
                        .set(MODIFIER, userId)
                        .set(UPDATE_TIME, statisticsTime)
                        .where(conditions)
                        .execute()
                }
            }
        }
    }

    fun projectAtomRelationCountByAtomCode(
        dslContext: DSLContext,
        projectId: String,
        atomCode: String,
        atomName: String
    ): Int {
        with(TProjectAtom.T_PROJECT_ATOM) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ATOM_CODE.eq(atomCode))
                .and(ATOM_NAME.eq(atomName))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun queryProjectAtomNewNameInfos(
        dslContext: DSLContext,
        projectIds: List<String>,
        page: Int,
        pageSize: Int
    ): Result<Record3<String, String, String>> {
        with(TAtomOverviewData.T_ATOM_OVERVIEW_DATA) {
            val subQuery = dslContext.select(
                PROJECT_ID,
                ATOM_CODE,
                DSL.max(CREATE_TIME).`as`(BK_MAX_CREATE_TIME)
            ).from(this)
                .where(PROJECT_ID.`in`(projectIds))
                .groupBy(PROJECT_ID, ATOM_CODE)
            return dslContext.select(
                PROJECT_ID,
                ATOM_CODE,
                ATOM_NAME
            )
                .from(this)
                .join(subQuery)
                .on(PROJECT_ID.eq(subQuery.field(PROJECT_ID.name, String::class.java))
                    .and(ATOM_CODE.eq(subQuery.field(ATOM_CODE.name, String::class.java)))
                    .and(CREATE_TIME.eq(subQuery.field(BK_MAX_CREATE_TIME, LocalDateTime::class.java)))
                )
                .where(PROJECT_ID.`in`(projectIds))
                .fetch()
        }
    }

    /**
     * 批量保存项目-插件关联关系。
     *
     * 并发优化要点：
     *   1. 入参先按唯一键 (projectId, atomCode) 去重，避免同批次内对同一行重复发起插入，
     *      减少 UNI_TPA_PROJECT_CODE 上的重复锁竞争；
     *   2. 同时按 (projectId, atomCode) 排序，让多个实例对相邻键的加锁顺序一致，
     *      显著降低跨事务的死锁概率；
     *   3. 拆分小批次(50)逐批 batch.execute()，避免单批次过大导致长事务/大范围加锁；
     *   4. 通过 CASE WHEN 把 atomName 没变化时的 UPDATE 退化为 no-op（不写 redo/binlog），
     *      减少不必要的 redo / 主从同步压力（注意：行的 X 锁仍会获取，这一点要靠 service
     *      层缓存把"大多数已存在记录"挡在 DAO 外才能真正缓解）。
     *
     * 注意：此方法不应再被包裹在外层大事务内调用，否则上述短事务优化会失效。
     */
    fun batchSaveProjectAtomInfo(
        dslContext: DSLContext,
        saveProjectAtomRelationPOs: List<SaveProjectAtomRelationDataPO>
    ) {
        if (saveProjectAtomRelationPOs.isEmpty()) return
        // 按唯一键去重 + 排序，控制并发加锁顺序，降低锁竞争和死锁概率
        val deduped = saveProjectAtomRelationPOs
            .distinctBy { it.projectId to it.atomCode }
            .sortedWith(compareBy({ it.projectId }, { it.atomCode }))
        deduped.chunked(BATCH_SAVE_CHUNK_SIZE).forEach { chunk ->
            with(TProjectAtom.T_PROJECT_ATOM) {
                dslContext.batch(
                    chunk.map { po ->
                        dslContext.insertInto(
                            this,
                            ID,
                            PROJECT_ID,
                            ATOM_CODE,
                            ATOM_NAME,
                            CREATOR,
                            MODIFIER
                        ).values(
                            po.id,
                            po.projectId,
                            po.atomCode,
                            po.atomName,
                            po.creator,
                            po.modifier
                        ).onDuplicateKeyUpdate()
                            // 仅当 atomName 真的发生变化才写入新值，否则赋为旧值让 InnoDB 跳过 redo
                            .set(ATOM_NAME, DSL.`when`(ATOM_NAME.ne(po.atomName), po.atomName).otherwise(ATOM_NAME))
                    }
                ).execute()
            }
        }
    }

    companion object {
        // 单批最多 50 条，避免长事务在唯一索引上长时间持有 next-key lock
        private const val BATCH_SAVE_CHUNK_SIZE = 50
    }
}
