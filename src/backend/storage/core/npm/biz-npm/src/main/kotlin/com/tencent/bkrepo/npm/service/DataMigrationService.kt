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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.npm.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.util.http.HttpClientBuilderFactory
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_FULL_PATH
import com.tencent.bkrepo.npm.constants.PKG_NAME
import com.tencent.bkrepo.npm.dao.repository.MigrationErrorDataRepository
import com.tencent.bkrepo.npm.model.TMigrationErrorData
import com.tencent.bkrepo.npm.pojo.migration.NpmDataMigrationResponse
import com.tencent.bkrepo.npm.pojo.migration.MigrationErrorDataInfo
import com.tencent.bkrepo.npm.pojo.migration.MigrationFailDataDetailInfo
import com.tencent.bkrepo.npm.pojo.migration.service.MigrationErrorDataCreateRequest
import com.tencent.bkrepo.npm.utils.GsonUtils
import com.tencent.bkrepo.npm.utils.ThreadPoolManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.system.measureTimeMillis

@Service
class DataMigrationService {

    @Value("\${npm.migration.data.url: ''}")
    private val url: String = StringPool.EMPTY

    @Value("\${npm.migration.package.count: 100}")
    private val count: Int = DEFAULT_COUNT

    @Autowired
    private lateinit var migrationErrorDataRepository: MigrationErrorDataRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val okHttpClient: OkHttpClient by lazy {
        HttpClientBuilderFactory.create().readTimeout(TIMEOUT, TimeUnit.SECONDS).build()
    }

    private final fun initTotalDataSetByUrl(): Set<String> {
        var totalDataSet: Set<String> = emptySet()
        if (url.isEmpty()) {
            return totalDataSet
        }
        var response: Response? = null
        try {
            val request = Request.Builder().url(url).get().build()
            response = okHttpClient.newCall(request).execute()
            if (checkResponse(response)) {
                val use = response.body()!!.byteStream().use { GsonUtils.transferInputStreamToJson(it) }
                totalDataSet =
                    use.entrySet().stream().filter { it.value.asBoolean }.map { it.key }.collect(Collectors.toSet())
            }
        } catch (exception: IOException) {
            logger.error("http send [$url] for get all package name data failed, {}", exception.message)
            throw exception
        } finally {
            response?.body()?.close()
        }
        return totalDataSet
    }

    private final fun initTotalDataSetByFile(): Set<String> {
        val inputStream: InputStream = this.javaClass.classLoader.getResourceAsStream(FILE_NAME) ?: return emptySet()
        val use = inputStream.use { GsonUtils.transferInputStreamToJson(it) }
        return use.entrySet().stream().filter { it.value.asBoolean }.map { it.key }.collect(Collectors.toSet())
    }

    private final fun initTotalDataSetByPkgName(pkgName: String): Set<String> {
        if (pkgName.isNotBlank()) {
            val pkgNameSet = pkgName.split(',').filter { it.isNotBlank() }.map { it.trim() }.toMutableSet()
            return pkgNameSet
        }
        return emptySet()
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun dataMigrationByFile(artifactInfo: NpmArtifactInfo, useErrorData: Boolean): NpmDataMigrationResponse {
        logger.info("migraion by file request parameter:[isUseErrorData: $useErrorData, fileName: $FILE_NAME]")
        var totalDataSet: Set<String>
        if (useErrorData) {
            val result = find(artifactInfo.projectId, artifactInfo.repoName)
            if (result == null) {
                totalDataSet = initTotalDataSetByFile()
            } else {
                totalDataSet = result.errorData
            }
        } else {
            totalDataSet = initTotalDataSetByFile()
        }
        logger.info("migration by file filter results: [$totalDataSet], size: ${totalDataSet.size}")
        return dataMigration(totalDataSet, artifactInfo, useErrorData)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun dataMigrationByUrl(artifactInfo: NpmArtifactInfo, useErrorData: Boolean): NpmDataMigrationResponse {
        logger.info("migraion by url request parameter: [url: $url, isUseErrorData: $useErrorData]")
        var totalDataSet: Set<String>
        if (useErrorData) {
            val result = find(artifactInfo.projectId, artifactInfo.repoName)
            if (result == null) {
                totalDataSet = initTotalDataSetByUrl()
            } else {
                totalDataSet = result.errorData
            }
        } else {
            totalDataSet = initTotalDataSetByUrl()
        }
        logger.info("migration by url filter results: [$totalDataSet], size: ${totalDataSet.size}")
        return dataMigration(totalDataSet, artifactInfo, useErrorData)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun dataMigrationByPkgName(
        artifactInfo: NpmArtifactInfo,
        useErrorData: Boolean,
        pkgName: String
    ): NpmDataMigrationResponse {
        logger.info("request parameter: [isUseErrorData: $useErrorData, pkgName: $pkgName]")
        val pkgNameSet = initTotalDataSetByPkgName(pkgName)
        logger.info("migration by pkgName filter results: [$pkgNameSet], size: ${pkgNameSet.size}")
        return dataMigration(pkgNameSet, artifactInfo, useErrorData)
    }

    fun dataMigration(
        totalDataSet: Set<String>,
        artifactInfo: NpmArtifactInfo,
        useErrorData: Boolean
    ): NpmDataMigrationResponse {
        val attributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        RequestContextHolder.setRequestAttributes(attributes, true)

        val start = System.currentTimeMillis()
        // val list = MigrationUtils.split(totalDataSet, count)
        val callableList: MutableList<Callable<MigrationFailDataDetailInfo>> = mutableListOf()
        var successCount = 0
        var failCount = 0
        totalDataSet.forEach {
            callableList.add(
                Callable {
                    RequestContextHolder.setRequestAttributes(attributes, true)
                    val migrationResult = doDataMigration(artifactInfo, it, totalDataSet.size)
                    if (!hasMigrationFailData(migrationResult)) {
                        successCount++
                    } else {
                        failCount++
                    }
                    if (isMultipleOfFive(totalDataSet.size)) {
                        logger.info(
                            "progress rate : successRate:[$successCount/${totalDataSet.size}], " +
                                "failRate[$failCount/${totalDataSet.size}]"
                        )
                    }
                    migrationResult
                }
            )
        }
        val resultList = ThreadPoolManager.submit(callableList)
        val elapseTimeMillis = System.currentTimeMillis() - start
        logger.info(
            "npm history data migration, total size[${totalDataSet.size}], success[$successCount], " +
                "fail[$failCount], elapse [${millisToSecond(elapseTimeMillis)}] s totally."
        )
        val resultDetailInfoSet = resultList.stream().filter { hasMigrationFailData(it) }.collect(Collectors.toSet())
        val collect = resultList.stream().map { it.pkgName }.collect(Collectors.toSet())
        if (collect.isNotEmpty() && useErrorData) {
            insertErrorData(artifactInfo, collect)
        }
        return NpmDataMigrationResponse(
            "Data migration information display：",
            totalDataSet.size,
            successCount,
            failCount,
            millisToSecond(elapseTimeMillis),
            resultDetailInfoSet
        )
    }

    fun doDataMigration(
        artifactInfo: NpmArtifactInfo,
        pkgName: String,
        totalDataSet: Int
    ): MigrationFailDataDetailInfo {
        var failDataDetailInfo: MigrationFailDataDetailInfo? = null
        try {
            Thread.sleep(SLEEP_MILLIS)
            measureTimeMillis {
                failDataDetailInfo = migrate(artifactInfo, pkgName)
            }.apply {
                if (!hasMigrationFailData(failDataDetailInfo!!)) {
                    logger.info("migrate npm package [$pkgName] success, elapse $this ms.")
                }
            }
        } catch (exception: IOException) {
            logger.error("failed to migrate [$pkgName.json] file, {}", exception.message)
        } catch (exception: InterruptedException) {
            logger.error("failed to migrate [$pkgName.json] file, {}", exception.message)
        }
        return failDataDetailInfo!!
    }

    fun migrate(artifactInfo: NpmArtifactInfo, pkgName: String): MigrationFailDataDetailInfo {
        val context = ArtifactMigrateContext()
        context.contextAttributes[NPM_FILE_FULL_PATH] = String.format(NPM_PKG_FULL_PATH, pkgName)
        context.contextAttributes[PKG_NAME] = pkgName
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        return repository.migrate(context) as MigrationFailDataDetailInfo
    }

    fun checkResponse(response: Response): Boolean {
        if (!response.isSuccessful) {
            logger.warn("Download file from remote failed: [${response.code()}]")
            return false
        }
        return true
    }

    private fun insertErrorData(artifactInfo: NpmArtifactInfo, collect: Set<String>) {
        val result = find(artifactInfo.projectId, artifactInfo.repoName)
        val counter = result?.counter?.plus(1) ?: 0
        val dataCreateRequest = MigrationErrorDataCreateRequest(
            projectId = artifactInfo.projectId,
            repoName = artifactInfo.repoName,
            counter = counter,
            errorData = jacksonObjectMapper().writeValueAsString(collect)
        )
        create(dataCreateRequest)
    }

    @Transactional(rollbackFor = [Throwable::class])
    fun create(dataCreateRequest: MigrationErrorDataCreateRequest) {
        with(dataCreateRequest) {
            this.takeIf { errorData.isNotBlank() } ?: throw ErrorCodeException(
                CommonMessageCode.PARAMETER_MISSING,
                this::errorData.name
            )
            val errorData = TMigrationErrorData(
                projectId = projectId,
                repoName = repoName,
                counter = counter,
                errorData = errorData,
                createdBy = operator,
                createdDate = LocalDateTime.now(),
                lastModifiedBy = operator,
                lastModifiedDate = LocalDateTime.now()
            )
            migrationErrorDataRepository.insert(errorData)
                .also { logger.info("Create migration error data [$dataCreateRequest] success.") }
        }
    }

    fun find(projectId: String, repoName: String): MigrationErrorDataInfo? {
        // repositoryService.checkRepository(projectId, repoName)
        val criteria = Criteria.where(TMigrationErrorData::projectId.name).`is`(projectId)
            .and(TMigrationErrorData::repoName.name).`is`(repoName)
        val query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, TMigrationErrorData::counter.name)).limit(0)
        return mongoTemplate.findOne(query, TMigrationErrorData::class.java)?.let { convert(it)!! }
    }

    companion object {
        const val FILE_NAME = "pkgName.json"
        const val TIMEOUT = 5 * 60L
        const val DEFAULT_COUNT = 1
        const val MILLIS_RATE = 1000L
        const val SLEEP_MILLIS = 20L

        val logger: Logger = LoggerFactory.getLogger(DataMigrationService::class.java)

        fun convert(tMigrationErrorData: TMigrationErrorData?): MigrationErrorDataInfo? {
            return tMigrationErrorData?.let {
                MigrationErrorDataInfo(
                    counter = it.counter,
                    errorData = jacksonObjectMapper().readValue(
                        it.errorData, object : TypeReference<MutableSet<String>>() {}
                    ),
                    projectId = it.projectId,
                    repoName = it.repoName,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate.format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }
        }

        fun millisToSecond(millis: Long): Long {
            return millis / MILLIS_RATE
        }

        fun isMultipleOfFive(size: Int): Boolean {
            return size.rem(1) == 0
        }

        fun hasMigrationFailData(failDataDetailInfo: MigrationFailDataDetailInfo): Boolean {
            return failDataDetailInfo.versionSet.size > 0
        }
    }
}
