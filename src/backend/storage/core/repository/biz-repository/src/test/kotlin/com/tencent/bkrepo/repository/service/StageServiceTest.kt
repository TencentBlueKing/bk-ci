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

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.repository.UT_PACKAGE_KEY
import com.tencent.bkrepo.repository.UT_PACKAGE_VERSION
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.stage.ArtifactStageEnum
import com.tencent.bkrepo.repository.pojo.stage.StageUpgradeRequest
import com.tencent.bkrepo.repository.search.packages.PackageSearchInterpreter
import com.tencent.bkrepo.repository.service.PackageServiceTest.Companion.buildCreateRequest
import com.tencent.bkrepo.repository.service.packages.PackageService
import com.tencent.bkrepo.repository.service.packages.StageService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query

@DisplayName("包服务测试")
@DataMongoTest
@Import(
    PackageDao::class,
    PackageVersionDao::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StageServiceTest @Autowired constructor(
    private val stageService: StageService,
    private val packageService: PackageService,
    private val mongoTemplate: MongoTemplate
) : ServiceBaseTest() {

    @MockBean
    private lateinit var packageSearchInterpreter: PackageSearchInterpreter

    @BeforeAll
    fun beforeAll() {
        initMock()
    }

    @BeforeEach
    fun beforeEach() {
        mongoTemplate.remove(Query(), TPackage::class.java)
        mongoTemplate.remove(Query(), TPackageVersion::class.java)
    }

    @Test
    @DisplayName("测试制品晋级")
    fun `test stage upgrade without tag`() {
        packageService.createPackageVersion(buildCreateRequest())
        var stageTagList = stageService.query(UT_PROJECT_ID, UT_REPO_NAME, UT_PACKAGE_KEY, UT_PACKAGE_VERSION)
        Assertions.assertEquals(0, stageTagList.size)

        val upgradeRequest = buildUpgradeRequest()
        stageService.upgrade(upgradeRequest)
        stageTagList = stageService.query(UT_PROJECT_ID, UT_REPO_NAME, UT_PACKAGE_KEY, UT_PACKAGE_VERSION)
        Assertions.assertEquals(1, stageTagList.size)
        Assertions.assertEquals(ArtifactStageEnum.PRE_RELEASE.tag, stageTagList[0])

        stageService.upgrade(upgradeRequest)
        stageTagList = stageService.query(UT_PROJECT_ID, UT_REPO_NAME, UT_PACKAGE_KEY, UT_PACKAGE_VERSION)
        Assertions.assertEquals(2, stageTagList.size)
        Assertions.assertEquals(ArtifactStageEnum.PRE_RELEASE.tag, stageTagList[0])
        Assertions.assertEquals(ArtifactStageEnum.RELEASE.tag, stageTagList[1])
    }

    @Test
    @DisplayName("测试制品晋级(指定tag)")
    fun `test stage upgrade with tag`() {
        packageService.createPackageVersion(buildCreateRequest())
        var stageTagList = stageService.query(UT_PROJECT_ID, UT_REPO_NAME, UT_PACKAGE_KEY, UT_PACKAGE_VERSION)
        Assertions.assertEquals(0, stageTagList.size)

        val upgradeRequest = buildUpgradeRequest(ArtifactStageEnum.RELEASE)
        stageService.upgrade(upgradeRequest)
        stageTagList = stageService.query(UT_PROJECT_ID, UT_REPO_NAME, UT_PACKAGE_KEY, UT_PACKAGE_VERSION)
        Assertions.assertEquals(1, stageTagList.size)
        Assertions.assertEquals(ArtifactStageEnum.RELEASE.tag, stageTagList[0])
    }

    @Test
    @DisplayName("测试不合法的tag晋级")
    fun `test stage upgrade with illegal tag`() {
        packageService.createPackageVersion(buildCreateRequest())

        stageService.upgrade(buildUpgradeRequest())
        assertThrows<ErrorCodeException> { stageService.upgrade(buildUpgradeRequest(ArtifactStageEnum.PRE_RELEASE)) }

        stageService.upgrade(buildUpgradeRequest())
        assertThrows<ErrorCodeException> { stageService.upgrade(buildUpgradeRequest(ArtifactStageEnum.PRE_RELEASE)) }
        assertThrows<ErrorCodeException> { stageService.upgrade(buildUpgradeRequest(ArtifactStageEnum.RELEASE)) }
    }

    private fun buildUpgradeRequest(newTag: ArtifactStageEnum? = null): StageUpgradeRequest {
        return StageUpgradeRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            packageKey = UT_PACKAGE_KEY,
            version = UT_PACKAGE_VERSION,
            newTag = newTag?.tag,
            operator = UT_USER
        )
    }
}
