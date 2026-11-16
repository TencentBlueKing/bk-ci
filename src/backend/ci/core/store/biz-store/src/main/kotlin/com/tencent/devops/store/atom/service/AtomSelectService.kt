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

package com.tencent.devops.store.atom.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.atom.dao.AtomQueryParam
import com.tencent.devops.store.atom.util.AtomSelectScorer
import com.tencent.devops.store.common.dao.StoreComponentContractDao
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.AtomSelectItem
import com.tencent.devops.store.pojo.atom.AtomSelectResp
import com.tencent.devops.store.pojo.atom.enums.AtomAvailabilityEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 插件选型检索：用契约选型字段召回，再复用插件列表查询做可见性、安装状态和环境过滤
 */
@Service
class AtomSelectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val atomService: AtomService,
    private val storeComponentContractDao: StoreComponentContractDao,
    private val atomSelectionIndexCache: AtomSelectionIndexCache
) {

    @Suppress("LongParameterList")
    fun selectAtoms(
        userId: String,
        projectCode: String,
        keywords: String,
        jobType: String?,
        os: String?,
        category: String?,
        limit: Int?
    ): AtomSelectResp {
        val queries = AtomSelectScorer.parseQueries(keywords)
        if (queries.isEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf("keywords"))
        }
        checkProjectPermission(userId, projectCode)
        val entries = atomSelectionIndexCache.entries()
        val hits = AtomSelectScorer.recall(entries, queries, RECALL_SIZE).associateBy { it.atomCode }
        val pinnedCodes = queries.filter { ATOM_CODE_REGEX.matches(it) }
        val candidateCodes = (pinnedCodes + hits.keys).distinct()
        if (candidateCodes.isEmpty()) {
            return AtomSelectResp(contractCount = entries.size, items = emptyList())
        }
        val atoms = atomService.serviceGetPipelineAtoms(
            userId = userId,
            queryParam = AtomQueryParam(
                serviceScope = ServiceScopeEnum.PIPELINE,
                jobType = jobType,
                os = os,
                projectCode = projectCode,
                category = category,
                classifyId = null,
                recommendFlag = null,
                keyword = null,
                fitOsFlag = true,
                queryFitAgentBuildLessAtomFlag = true,
                queryProjectAtomFlag = false,
                atomCodes = candidateCodes
            ),
            page = 1,
            pageSize = candidateCodes.size
        ).data?.records ?: emptyList()

        val ranked = atoms.sortedWith(
            compareByDescending<AtomRespItem> { atom -> pinnedCodes.any { it.equals(atom.atomCode, true) } }
                .thenByDescending { hits[it.atomCode]?.hitCount ?: 0 }
                .thenByDescending { hits[it.atomCode]?.score ?: 0 }
                .thenByDescending { it.recentExecuteNum ?: 0 }
        ).take((limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT))
        val entryMap = entries.associateBy { it.atomCode }
        val contractCodes = ranked.map { it.atomCode }.filter { entryMap.containsKey(it) }
        val bodies = if (contractCodes.isEmpty()) {
            emptyMap()
        } else {
            storeComponentContractDao.listContractBodies(dslContext, StoreTypeEnum.ATOM, contractCodes)
        }
        logger.info(
            "atom select|$userId|$projectCode|$queries|contracts=${entries.size}|recalled=${hits.size}" +
                "|visible=${atoms.size}|returned=${ranked.size}"
        )
        return AtomSelectResp(
            contractCount = entries.size,
            items = ranked.map { atom ->
                buildItem(
                    atom = atom,
                    hit = hits[atom.atomCode],
                    entry = entryMap[atom.atomCode],
                    contract = bodies[atom.atomCode]?.let { parseContract(atom.atomCode, it) }
                )
            }
        )
    }

    private fun checkProjectPermission(userId: String, projectCode: String) {
        val permitted = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
            projectCode = projectCode,
            userId = userId
        ).data
        if (permitted != true) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_QUERY_PROJECT_PERMISSION_IS_INVALID,
                params = arrayOf(projectCode)
            )
        }
    }

    private fun buildItem(
        atom: AtomRespItem,
        hit: AtomSelectScorer.Hit?,
        entry: AtomSelectScorer.Entry?,
        contract: Map<String, Any?>?
    ) = AtomSelectItem(
        atomCode = atom.atomCode,
        name = atom.name,
        defaultVersion = atom.defaultVersion,
        availability = when {
            atom.defaultFlag || atom.installed == true -> AtomAvailabilityEnum.READY
            atom.installFlag == true -> AtomAvailabilityEnum.INSTALLABLE
            else -> AtomAvailabilityEnum.NEED_APPLY
        },
        recentExecuteNum = atom.recentExecuteNum,
        matched = hit?.matched ?: emptyList(),
        hasContract = entry != null,
        selectionSummary = entry?.summary ?: atom.summary,
        notFor = textItems(contract?.get("not_for")),
        prerequisites = textItems(contract?.get("prerequisites")),
        coverage = entry?.coverage,
        stale = entry?.stale
    )

    private fun parseContract(atomCode: String, body: String): Map<String, Any?>? {
        return try {
            JsonUtil.to(body, object : TypeReference<Map<String, Any?>>() {})
        } catch (ignored: Exception) {
            logger.warn("atom select parse contract failed|$atomCode", ignored)
            null
        }
    }

    private fun textItems(section: Any?): List<String> {
        return (section as? List<*>)
            ?.filterIsInstance<String>()
            ?.take(MAX_TEXT_ITEMS)
            ?.map { if (it.length > MAX_TEXT_LENGTH) it.take(MAX_TEXT_LENGTH) + "…" else it }
            ?: emptyList()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AtomSelectService::class.java)
        private const val DEFAULT_LIMIT = 10
        private const val MAX_LIMIT = 20

        // 过滤会去掉环境不匹配、不可见的插件，召回量要明显大于返回条数
        private const val RECALL_SIZE = 100
        private const val MAX_TEXT_ITEMS = 5
        private const val MAX_TEXT_LENGTH = 200
        private val ATOM_CODE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_-]{1,63}$")
    }
}
