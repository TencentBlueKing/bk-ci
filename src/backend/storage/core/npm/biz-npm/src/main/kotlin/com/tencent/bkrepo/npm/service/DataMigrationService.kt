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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.migration.MigrateDetail
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.dao.repository.MigrationErrorDataRepository
import com.tencent.bkrepo.npm.model.TMigrationErrorData
import com.tencent.bkrepo.npm.pojo.migration.MigrationErrorDataInfo
import com.tencent.bkrepo.npm.pojo.migration.service.MigrationErrorDataCreateRequest
import com.tencent.bkrepo.npm.properties.NpmProperties
import com.tencent.bkrepo.npm.utils.GsonUtils
import com.tencent.bkrepo.npm.utils.OkHttpUtil
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

@Service
class DataMigrationService(
    private val npmProperties: NpmProperties,
    private val mongoTemplate: MongoTemplate,
    private val okHttpUtil: OkHttpUtil,
    private val migrationErrorDataRepository: MigrationErrorDataRepository
) {

    private final fun initTotalDataSetByUrl(): Set<String> {
        var totalDataSet: Set<String> = emptySet()
        val url = npmProperties.migration.dataUrl
        if (url.isEmpty()) {
            return totalDataSet
        }
        var response: Response? = null
        try {
            response = okHttpUtil.doGet(url)
            if (checkResponse(response)) {
                val pkgNameData =
                    response.body()!!.byteStream().use { JsonUtils.objectMapper.readValue<Map<String, Boolean>>(it) }
                totalDataSet =
                    pkgNameData.entries.stream().filter { it.value }.map { it.key }.collect(Collectors.toSet())
            }
        } catch (exception: IOException) {
            logger.error("http send [$url] for get all migration package name data failed, {}", exception.message)
            throw exception
        } finally {
            response?.body()?.close()
        }
        return totalDataSet
    }

    private final fun initTotalDataSetByFile(): Set<String> {
        val inputStream: InputStream = this.javaClass.classLoader.getResourceAsStream(FILE_NAME) ?: return emptySet()
        val pkgNameData = inputStream.use { GsonUtils.transferInputStreamToJson(it) }
        return pkgNameData.entrySet().stream().filter { it.value.asBoolean }.map { it.key }.collect(Collectors.toSet())
    }

    private final fun initTotalDataSetByPkgName(pkgName: String): Set<String> {
        if (pkgName.isBlank()) return emptySet()
        return pkgName.split(',').filter { it.isNotBlank() }.map { it.trim() }.toMutableSet()
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun dataMigrationByFile(artifactInfo: NpmArtifactInfo, useErrorData: Boolean): MigrateDetail {
        logger.info(
            "handling migration by file request parameter: " +
                "[isUseErrorData: $useErrorData, fileName: $FILE_NAME]"
        )
        val totalDataSet: Set<String>
        totalDataSet = if (useErrorData) {
            val result = find(artifactInfo.projectId, artifactInfo.repoName)
            result?.errorData ?: initTotalDataSetByFile()
        } else {
            initTotalDataSetByFile()
        }
        logger.info("migration by file filter results: [$totalDataSet], size: ${totalDataSet.size}")
        return dataMigration(totalDataSet, artifactInfo, useErrorData)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun dataMigrationByUrl(artifactInfo: NpmArtifactInfo, useErrorData: Boolean): MigrateDetail {
        logger.info(
            "handling migration by url request parameter: " +
                "[url: ${npmProperties.migration.dataUrl}, isUseErrorData: $useErrorData]"
        )
        val totalDataSet: Set<String>
        totalDataSet = if (useErrorData) {
            val result = find(artifactInfo.projectId, artifactInfo.repoName)
            result?.errorData ?: initTotalDataSetByUrl()
        } else {
            initTotalDataSetByUrl()
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
    ): MigrateDetail {
        logger.info(
            "handling migration by package name request parameter: [isUseErrorData: $useErrorData, pkgName: $pkgName]"
        )
        val pkgNameSet = initTotalDataSetByPkgName(pkgName)
        logger.info("migration by pkgName filter results: [$pkgNameSet], size: ${pkgNameSet.size}")
        return dataMigration(pkgNameSet, artifactInfo, useErrorData)
    }

    fun dataMigration(
        totalDataSet: Set<String>,
        artifactInfo: NpmArtifactInfo,
        useErrorData: Boolean
    ): MigrateDetail {
        val context = ArtifactMigrateContext()
        context.putAttribute("migrationDateSet", totalDataSet)
        return ArtifactContextHolder.getRepository().migrate(context)
        // if (useErrorData && migrateDetail.packageList.isEmpty()) {
        //     // 插入迁移失败数据
        //
        // }
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
        val criteria = Criteria.where(TMigrationErrorData::projectId.name).`is`(projectId)
            .and(TMigrationErrorData::repoName.name).`is`(repoName)
        val query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, TMigrationErrorData::counter.name)).limit(0)
        return mongoTemplate.findOne(query, TMigrationErrorData::class.java)?.let { convert(it)!! }
    }

    companion object {
        const val FILE_NAME = "pkgName.json"
        const val MILLIS_RATE = 1000L

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
    }
}
