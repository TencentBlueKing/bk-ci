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

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_METADATA_FULL_PATH
import com.tencent.bkrepo.npm.constants.PKG_NAME
import com.tencent.bkrepo.npm.pojo.migration.NpmPackageDependentMigrationResponse
import com.tencent.bkrepo.npm.utils.GsonUtils
import com.tencent.bkrepo.npm.utils.MigrationUtils
import com.tencent.bkrepo.npm.utils.ThreadPoolManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

@Service
class PackageDependentService {
    @Value("\${npm.migration.data.url: ''}")
    private val url: String = StringPool.EMPTY

    @Value("\${npm.migration.package.count: 100}")
    private val count: Int = DEFAULT_COUNT

    @Autowired
    private lateinit var asyncExecutor: ThreadPoolTaskExecutor

    private val okHttpClient: OkHttpClient by lazy {
        HttpClientBuilderFactory.create().readTimeout(TIMEOUT, TimeUnit.SECONDS).build()
    }

    private final fun initTotalDataSetByUrl(): Set<String> {
        var totalDataSet: Set<String> = emptySet()
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
            logger.error(
                "http send [$url] for get all package name data failed, {}",
                exception.message
            )
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

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun dependentMigrationByUrl(artifactInfo: NpmArtifactInfo): NpmPackageDependentMigrationResponse {
        logger.info("dependent migration by url request parameter: [url: $url]")
        val totalDataSet = initTotalDataSetByUrl()
        logger.info("dependent migration by url filter results: [$totalDataSet], size: ${totalDataSet.size}")
        return dependentMigration(artifactInfo, totalDataSet)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun dependentMigrationByFile(artifactInfo: NpmArtifactInfo): NpmPackageDependentMigrationResponse {
        logger.info("dependent migration by file request parameter:[fileName: $FILE_NAME]")
        val totalDataSet = initTotalDataSetByFile()
        logger.info("dependent migration by file filter results: [$totalDataSet], size: ${totalDataSet.size}")
        return dependentMigration(artifactInfo, totalDataSet)
    }

    fun dependentMigration(
        artifactInfo: NpmArtifactInfo,
        totalDataSet: Set<String>
    ): NpmPackageDependentMigrationResponse {
        val attributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        RequestContextHolder.setRequestAttributes(attributes, true)
        val start = System.currentTimeMillis()
        val list = MigrationUtils.split(totalDataSet, count)
        val callableList: MutableList<Callable<Set<String>>> = mutableListOf()
        var migrationDependentResult: Pair<Set<String>, Set<String>> = Pair(emptySet(), emptySet())
        list.forEach {
            callableList.add(
                Callable {
                    RequestContextHolder.setRequestAttributes(attributes)
                    migrationDependentResult = doDependentMigration(artifactInfo, it.toSet(), totalDataSet)
                    migrationDependentResult.second
                }
            )
        }
        val resultList = ThreadPoolManager.submit(callableList)
        val elapseTimeMillis = System.currentTimeMillis() - start
        logger.info(
            "npm package dependent migrate, total size[${totalDataSet.size}], " +
                "success[${migrationDependentResult.first.size}], fail[${migrationDependentResult.second.size}], " +
                "elapse [${millisToSecond(elapseTimeMillis)}] s totally."
        )
        val collect = resultList.stream().flatMap { set -> set.stream() }.collect(Collectors.toSet())
        return NpmPackageDependentMigrationResponse(
            "npm dependent migration information display：",
            totalDataSet.size,
            migrationDependentResult.first.size,
            migrationDependentResult.second.size,
            millisToSecond(elapseTimeMillis),
            collect
        )
    }

    fun doDependentMigration(
        artifactInfo: NpmArtifactInfo,
        data: Set<String>,
        totalDataSet: Set<String>
    ): Pair<Set<String>, Set<String>> {
        val successSet = mutableSetOf<String>()
        val errorSet = mutableSetOf<String>()
        data.forEach { pkgName ->
            try {
                dependentMigrate(artifactInfo, pkgName)
                logger.info("npm package name: [$pkgName] dependent migration success!")
                successSet.add(pkgName)
                if (isMultipleOfTen(successSet.size)) {
                    logger.info(
                        "dependent migrate progress rate : successRate:[${successSet.size}/${totalDataSet.size}], " +
                            "failRate[${errorSet.size}/${totalDataSet.size}]"
                    )
                }
            } catch (exception: IOException) {
                logger.error("failed to query [$pkgName.json] file, {}", exception.message)
                errorSet.add(pkgName)
            } catch (exception: InterruptedException) {
                logger.error("failed to query [$pkgName.json] file, {}", exception.message)
                errorSet.add(pkgName)
            }
        }
        return Pair(successSet, errorSet)
    }

    fun dependentMigrate(artifactInfo: NpmArtifactInfo, pkgName: String) {
        val context = ArtifactMigrateContext()
        context.putAttribute(NPM_FILE_FULL_PATH, String.format(NPM_PKG_METADATA_FULL_PATH, pkgName))
        context.putAttribute(PKG_NAME, pkgName)
        val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
        // (repository as NpmLocalRepository).dependentMigrate(context)
    }

    fun checkResponse(response: Response): Boolean {
        if (!response.isSuccessful) {
            logger.warn("Download file from remote failed: [${response.code()}]")
            return false
        }
        return true
    }

    companion object {
        private const val FILE_NAME = "pkgName.json"
        const val TIMEOUT = 60L
        const val DEFAULT_COUNT = 100
        val logger: Logger = LoggerFactory.getLogger(PackageDependentService::class.java)

        fun millisToSecond(millis: Long): Long {
            return millis / DataMigrationService.MILLIS_RATE
        }

        fun isMultipleOfTen(size: Int): Boolean {
            return size.rem(10) == 0
        }
    }
}
