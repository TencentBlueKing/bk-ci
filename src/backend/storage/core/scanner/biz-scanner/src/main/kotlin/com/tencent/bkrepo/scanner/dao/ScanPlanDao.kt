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

import com.mongodb.client.result.UpdateResult
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.scanner.model.TScanPlan
import com.tencent.bkrepo.scanner.pojo.ScanPlan
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.size
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ScanPlanDao : ScannerSimpleMongoDao<TScanPlan>() {
    fun get(id: String): TScanPlan {
        val query = Query(criteria().and(ID).isEqualTo(id))
        return findOne(query) ?: throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND)
    }

    fun find(projectId: String, id: String): TScanPlan? {
        val criteria = projectCriteria(projectId).and(ID).isEqualTo(id)
        return findOne(Query(criteria))
    }

    fun findByProjectIdAndRepoName(
        projectId: String,
        repoName: String,
        planType: String,
        scanOnNewArtifact: Boolean = true,
        includeEmptyRepoNames: Boolean = true
    ): List<TScanPlan> {
        val criteria = Criteria
            .where(TScanPlan::projectId.name).isEqualTo(projectId)
            .and(TScanPlan::scanOnNewArtifact.name).isEqualTo(scanOnNewArtifact)
            .and(TScanPlan::type.name).isEqualTo(planType)
        if (!includeEmptyRepoNames) {
            criteria.and(TScanPlan::repoNames.name).isEqualTo(repoName)
        } else {
            val repoNamesCriteria = Criteria().orOperator(
                TScanPlan::repoNames.size(0),
                TScanPlan::repoNames.isEqualTo(repoName)
            )
            criteria.andOperator(repoNamesCriteria)
        }

        return find(Query(criteria))
    }

    fun findByIds(ids: List<String>, includeDeleted: Boolean = false): List<TScanPlan> {
        val query = Query(criteria(includeDeleted).and(ID).inValues(ids))
        return find(query)
    }

    fun exists(projectId: String, id: String): Boolean {
        val criteria = projectCriteria(projectId).and(ID).isEqualTo(id)
        return exists(Query(criteria))
    }

    fun delete(projectId: String, id: String): UpdateResult {
        val criteria = projectCriteria(projectId).and(ID).isEqualTo(id)
        val now = LocalDateTime.now()
        val update = update(now).set(TScanPlan::deleted.name, now)
        return updateFirst(Query(criteria), update)
    }

    fun existsByProjectIdAndName(projectId: String, name: String): Boolean {
        val query = Query(
            projectCriteria(projectId).and(TScanPlan::name.name).isEqualTo(name)
        )
        return exists(query)
    }

    fun list(projectId: String, type: String? = null): List<TScanPlan> {
        val criteria = projectCriteria(projectId)
        type?.let { criteria.and(TScanPlan::type.name).isEqualTo(type) }
        val query = Query(criteria).with(Sort.by(TScanPlan::createdDate.name).descending())
        return find(query)
    }

    fun page(projectId: String, type: String?, planNameContains: String?, pageLimit: PageLimit): Page<TScanPlan> {
        val criteria = projectCriteria(projectId)
        type?.let { criteria.and(TScanPlan::type.name).isEqualTo(type) }
        planNameContains?.let { criteria.and(TScanPlan::name.name).regex(".*$planNameContains.*") }
        val pageRequest = Pages.ofRequest(pageLimit.getNormalizedPageNumber(), pageLimit.getNormalizedPageSize())
        val query = Query(criteria).with(pageRequest).with(Sort.by(TScanPlan::createdDate.name).descending())

        return Pages.ofResponse(pageRequest, count(query), find(query))
    }

    fun update(scanPlan: ScanPlan): UpdateResult {
        with(scanPlan) {
            val criteria = projectCriteria(projectId!!).and(ID).`is`(id)
            val update = update()
            name?.let { update.set(TScanPlan::name.name, it) }
            description?.let { update.set(TScanPlan::description.name, it) }
            scanOnNewArtifact?.let { update.set(TScanPlan::scanOnNewArtifact.name, it) }
            repoNames?.let { update.set(TScanPlan::repoNames.name, it) }
            rule?.let { update.set(TScanPlan::rule.name, it.toJsonString()) }

            val query = Query(criteria)
            return updateFirst(query, update)
        }
    }

    fun updateLatestScanTaskId(planId: String, scanTaskId: String): UpdateResult {
        val query = Query(Criteria.where(ID).isEqualTo(planId))
        val update = Update.update(TScanPlan::latestScanTaskId.name, scanTaskId)
        return updateFirst(query, update)
    }

    /**
     * 批量更新扫描方案扫描结果预览信息
     *
     * @param planOverviewMap key 为扫描方案id， value为扫描预览结果
     */
    fun decrementScanResultOverview(planOverviewMap: Map<String, Map<String, Number>>) {
        val updates = ArrayList<org.springframework.data.util.Pair<Query, Update>>(planOverviewMap.size)
        for (entry in planOverviewMap) {
            val planId = entry.key
            val overview = entry.value

            val query = Query(Criteria.where(ID).isEqualTo(planId))
            val update = buildOverviewUpdate(overview, true) ?: continue

            updates.add(org.springframework.data.util.Pair.of(query, update))
        }

        // 没有更新时直接返回
        if (updates.isEmpty()) {
            return
        }

        determineMongoTemplate()
            .bulkOps(BulkOperations.BulkMode.UNORDERED, determineCollectionName())
            .updateOne(updates)
            .execute()
    }

    fun updateScanResultOverview(latestScanTaskId: String, overview: Map<String, Any?>) {
        val criteria = TScanPlan::latestScanTaskId.isEqualTo(latestScanTaskId)
        val update = buildOverviewUpdate(overview) ?: return
        updateFirst(Query(criteria), update)
    }

    private fun buildOverviewUpdate(overview: Map<String, Any?>, dec: Boolean = false): Update? {
        val update = Update()
        var hasUpdate = false

        overview.forEach {
            if (it.value is Number) {
                val value = if (dec) {
                    -(it.value as Number).toLong()
                } else {
                    it.value as Number
                }
                update.inc("${TScanPlan::scanResultOverview.name}.${it.key}", value)
                hasUpdate = true
            }
        }

        return if (hasUpdate) {
            update
        } else {
            null
        }
    }

    private fun projectCriteria(projectId: String, includeDeleted: Boolean = false): Criteria {
        val criteria = TScanPlan::projectId.isEqualTo(projectId)
        if (!includeDeleted) {
            criteria.and(TScanPlan::deleted.name).isEqualTo(null)
        }
        return criteria
    }

    private fun criteria(includeDeleted: Boolean = false): Criteria {
        return if (!includeDeleted) {
            TScanPlan::deleted.isEqualTo(null)
        } else {
            Criteria()
        }
    }

    private fun update(
        now: LocalDateTime = LocalDateTime.now(),
        lastModifiedBy: String = SecurityUtils.getUserId()
    ): Update {
        return Update.update(TScanPlan::lastModifiedDate.name, now)
            .set(TScanPlan::lastModifiedBy.name, lastModifiedBy)
    }
}
