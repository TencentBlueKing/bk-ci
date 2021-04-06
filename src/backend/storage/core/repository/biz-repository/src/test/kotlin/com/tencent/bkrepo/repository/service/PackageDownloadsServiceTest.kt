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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.repository.UT_PACKAGE_KEY
import com.tencent.bkrepo.repository.UT_PACKAGE_NAME
import com.tencent.bkrepo.repository.UT_PACKAGE_VERSION
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageDownloadsDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.download.DetailsQueryRequest
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.stage.ArtifactStageEnum
import com.tencent.bkrepo.repository.search.packages.PackageSearchInterpreter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.time.LocalDate
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

@DisplayName("下载统计服务测试")
@DataMongoTest
@Import(
    PackageDao::class,
    PackageVersionDao::class,
    PackageDownloadsDao::class
)
class PackageDownloadsServiceTest @Autowired constructor(
    private val packageDownloadsService: PackageDownloadsService,
    private val packageService: PackageService,
    private val mongoTemplate: MongoTemplate
) : ServiceBaseTest() {

    @MockBean
    private lateinit var repositoryService: RepositoryService

    @MockBean
    private lateinit var packageSearchInterpreter: PackageSearchInterpreter

    @BeforeEach
    fun beforeEach() {
        initMock()
        mongoTemplate.remove(Query(), TPackage::class.java)
        mongoTemplate.remove(Query(), TPackageVersion::class.java)
    }

    @Test
    @DisplayName("创建下载量相关测试")
    fun createTest() {
        val packageVersionRequest = buildPackageCreateRequest(version = UT_PACKAGE_VERSION, overwrite = false)
        packageService.createPackageVersion(packageVersionRequest)

        val count = 100
        val cyclicBarrier = CyclicBarrier(count)
        val threadList = mutableListOf<Thread>()
        val request = buildDownloadStatRequest()
        repeat(count) {
            val thread = thread {
                cyclicBarrier.await()
                packageDownloadsService.record(request)
            }
            threadList.add(thread)
        }
        threadList.forEach { it.join() }
        val result = packageDownloadsService.queryDetails(DetailsQueryRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            packageKey = UT_PACKAGE_KEY,
            packageVersion = null,
            fromDate = LocalDate.now(),
            toDate = LocalDate.now()
        ))
        println(result)

        val packageInfo = packageService.findPackageByKey(UT_PROJECT_ID, UT_REPO_NAME, UT_PACKAGE_KEY)!!
        Assertions.assertEquals(count.toLong(), packageInfo.downloads)
    }

    private fun buildPackageCreateRequest(
        projectId: String = UT_PROJECT_ID,
        repoName: String = UT_REPO_NAME,
        packageName: String = UT_PACKAGE_NAME,
        packageKey: String = UT_PACKAGE_KEY,
        version: String = UT_PACKAGE_VERSION,
        overwrite: Boolean = false
    ): PackageVersionCreateRequest {
        return PackageVersionCreateRequest(
            projectId = projectId,
            repoName = repoName,
            packageName = packageName,
            packageKey = packageKey,
            packageType = PackageType.MAVEN,
            packageDescription = "some description",
            versionName = version,
            size = 1024,
            manifestPath = "/com/tencent/bkrepo/test/$version",
            artifactPath = "/com/tencent/bkrepo/test/$version",
            stageTag = listOf(ArtifactStageEnum.RELEASE.toString()),
            metadata = mapOf("key" to "value"),
            overwrite = overwrite,
            createdBy = UT_USER
        )
    }

    private fun buildDownloadStatRequest(
        projectId: String = UT_PROJECT_ID,
        repoName: String = UT_REPO_NAME,
        packageName: String = UT_PACKAGE_NAME,
        packageKey: String = UT_PACKAGE_KEY,
        version: String = UT_PACKAGE_VERSION
    ): PackageDownloadRecord {
        return PackageDownloadRecord(
            projectId = projectId,
            repoName = repoName,
            packageKey = packageKey,
            packageVersion = version
        )
    }
}
