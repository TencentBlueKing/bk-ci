/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.npm.dao.repository.ModuleDepsRepository
import com.tencent.bkrepo.npm.model.TModuleDeps
import com.tencent.bkrepo.npm.pojo.module.des.ModuleDepsInfo
import com.tencent.bkrepo.npm.pojo.module.des.service.DepsCreateRequest
import com.tencent.bkrepo.npm.pojo.module.des.service.DepsDeleteRequest
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ModuleDepsService(
    private val moduleDepsRepository: ModuleDepsRepository,
    private val mongoTemplate: MongoTemplate
) {

    /**
     * 创建依赖关系
     */
    @Transactional(rollbackFor = [Exception::class])
    fun create(depsCreateRequest: DepsCreateRequest): ModuleDepsInfo {
        with(depsCreateRequest) {
            checkParameter(this)
            if (exist(projectId, repoName, name, deps)) {
                if (!overwrite) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, name)
                } else {
                    delete(DepsDeleteRequest(projectId, repoName, name, deps, operator))
                }
            }
            val moduleDeps = TModuleDeps(
                projectId = projectId,
                repoName = repoName,
                name = name,
                deps = deps,
                createdBy = operator,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = operator,
                lastModifiedDate = LocalDateTime.now()
            )
            return moduleDepsRepository.insert(moduleDeps)
                .also { logger.info("Create module deps [$depsCreateRequest] success.") }
                .let { convert(it)!! }
        }
    }

    private fun checkParameter(depsCreateRequest: DepsCreateRequest) {
        with(depsCreateRequest) {
            this.takeIf { name.isNotBlank() } ?: throw ErrorCodeException(
                CommonMessageCode.PARAMETER_MISSING,
                this::name.name
            )
            this.takeIf { deps.isNotBlank() } ?: throw ErrorCodeException(
                CommonMessageCode.PARAMETER_MISSING,
                this::deps.name
            )
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun batchCreate(depsCreateRequestList: List<DepsCreateRequest>) {
        depsCreateRequestList.takeUnless { it.isNullOrEmpty() }
        val createList = mutableListOf<TModuleDeps>()
        depsCreateRequestList.forEach continuing@{
            with(it) {
                checkParameter(it)
                if (checkDepsExists()) return@continuing
                val moduleDeps = buildModuleDeps(it)
                createList.add(moduleDeps)
            }
        }
        if (createList.isNotEmpty()) {
            moduleDepsRepository.insert(createList)
            logger.info("batch insert module deps, size: [${createList.size}] success.")
        }
    }

    private fun DepsCreateRequest.checkDepsExists(): Boolean {
        if (exist(projectId, repoName, name, deps)) {
            if (!overwrite) {
                throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, name)
            }
            return true
        }
        return false
    }

    private fun exist(projectId: String, repoName: String, name: String?, deps: String): Boolean {
        if (deps.isBlank()) return false
        val criteria =
            Criteria.where(TModuleDeps::projectId.name).`is`(projectId)
                .and(TModuleDeps::repoName.name).`is`(repoName)
                .and(TModuleDeps::deps.name).`is`(deps)
        name?.run { criteria.and(TModuleDeps::name.name).`is`(name) }
        return mongoTemplate.exists(Query(criteria), TModuleDeps::class.java)
    }

    fun delete(depsDeleteRequest: DepsDeleteRequest) {
        with(depsDeleteRequest) {
            this.takeIf { !name.isNullOrBlank() } ?: throw ErrorCodeException(
                CommonMessageCode.PARAMETER_MISSING,
                this::name.name
            )
            if (!exist(projectId, repoName, name, deps)) throw ErrorCodeException(
                ArtifactMessageCode.NODE_EXISTED,
                deps
            )
            val depsQuery = depsQuery(this)
            mongoTemplate.remove(depsQuery, TModuleDeps::class.java).also {
                logger.info("Delete module deps [$depsDeleteRequest] by [$operator] success.")
            }
        }
    }

    fun deleteAllByName(depsDeleteRequest: DepsDeleteRequest, soft: Boolean = true) {
        with(depsDeleteRequest) {
            if (!exist(projectId, repoName, name, deps)) {
                logger.warn("npm package dependents with name [${depsDeleteRequest.deps}] not exists.")
                return
            }
            val depsQuery = depsQuery(this)
            mongoTemplate.remove(depsQuery, TModuleDeps::class.java).also {
                logger.info("Delete all module deps [$depsDeleteRequest] by [$operator] success.")
            }
        }
    }

    private fun depsQuery(depsDeleteRequest: DepsDeleteRequest): Query {
        with(depsDeleteRequest) {
            val criteria =
                Criteria.where(TModuleDeps::projectId.name).`is`(projectId).and(TModuleDeps::repoName.name)
                    .`is`(repoName)
                    .and(TModuleDeps::deps.name).`is`(deps)
            name?.let { criteria.and(TModuleDeps::name.name).`is`(it) }
            return Query(criteria)
        }
    }

    fun find(projectId: String, repoName: String, name: String, deps: String): ModuleDepsInfo {
        val criteria =
            Criteria.where(TModuleDeps::projectId.name).`is`(projectId)
                .and(TModuleDeps::repoName.name).`is`(repoName)
                .and(TModuleDeps::name.name).`is`(name).and(TModuleDeps::deps.name).`is`(deps)
        if (mongoTemplate.count(Query.query(criteria), TModuleDeps::class.java) >= THRESHOLD) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_LIST_TOO_LARGE)
        }
        return mongoTemplate.findOne(Query.query(criteria), TModuleDeps::class.java).let { convert(it)!! }
    }

    fun list(projectId: String, repoName: String, name: String): List<ModuleDepsInfo> {
        val criteria =
            Criteria.where(TModuleDeps::projectId.name).`is`(projectId).and(TModuleDeps::repoName.name).`is`(repoName)
                .and(TModuleDeps::name.name).`is`(name)
        val query = Query.query(criteria).with(Sort.by(TModuleDeps::createdDate.name))
        if (mongoTemplate.count(query, TModuleDeps::class.java) >= THRESHOLD) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_LIST_TOO_LARGE)
        }
        return mongoTemplate.find(query, TModuleDeps::class.java).map { convert(it)!! }
    }

    fun page(projectId: String, repoName: String, pageNumber: Int, pageSize: Int, name: String): Page<ModuleDepsInfo> {
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val criteria =
            Criteria.where(TModuleDeps::projectId.name).`is`(projectId).and(TModuleDeps::repoName.name).`is`(repoName)
                .and(TModuleDeps::name.name).`is`(name)
        val query = Query.query(criteria).with(Sort.by(TModuleDeps::createdDate.name))
        val listData = mongoTemplate.find(query.with(pageRequest), TModuleDeps::class.java).map { convert(it)!! }
        val count = mongoTemplate.count(query, TModuleDeps::class.java)
        return Pages.ofResponse(pageRequest, count, listData)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ModuleDepsService::class.java)

        private const val THRESHOLD: Long = 100000L

        fun convert(tModuleDeps: TModuleDeps?): ModuleDepsInfo? {
            return tModuleDeps?.let {
                ModuleDepsInfo(
                    name = it.name,
                    deps = it.deps,
                    projectId = it.projectId,
                    repoName = it.repoName,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }
        }

        fun buildModuleDeps(request: DepsCreateRequest): TModuleDeps {
            return with(request) {
                TModuleDeps(
                    projectId = projectId,
                    repoName = repoName,
                    name = name,
                    deps = deps,
                    createdBy = operator,
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = operator,
                    lastModifiedDate = LocalDateTime.now()
                )
            }
        }
    }
}
