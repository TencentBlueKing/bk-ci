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

package com.tencent.bkrepo.repository.service.query

import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import com.tencent.bkrepo.repository.pojo.stage.ArtifactStageEnum
import com.tencent.bkrepo.repository.search.common.RepoNameRuleInterceptor
import com.tencent.bkrepo.repository.search.common.RepoTypeRuleInterceptor
import com.tencent.bkrepo.repository.search.node.NodeQueryInterpreter
import com.tencent.bkrepo.repository.service.ServiceBaseTest
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean

@DisplayName("节点自定义查询解释器测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DataMongoTest
class NodeQueryInterpreterTest : ServiceBaseTest() {

    @MockBean
    lateinit var repositoryService: RepositoryService

    @MockBean
    private lateinit var repoNameRuleInterceptor: RepoNameRuleInterceptor

    @MockBean
    private lateinit var repoTypeRuleInterceptor: RepoTypeRuleInterceptor

    @BeforeAll
    fun beforeAll() {
        initMock()
    }

    @Test
    fun testQueryWithMetadata() {
        val queryModel = NodeQueryBuilder()
            .projectId("1")
            .repoName("repoName")
            .metadata("key", "value")
            .page(1, 10)
            .sort(Sort.Direction.ASC, "name")
            .select("projectId", "repoName", "fullPath", "metadata")
            .build()
        val interpreter = NodeQueryInterpreter(repoNameRuleInterceptor, repoTypeRuleInterceptor)
        val query = interpreter.interpret(queryModel)
        println(query.queryModel)
    }

    @Test
    fun testQueryWithStageTag() {
        val queryModel = NodeQueryBuilder()
            .projectId("1")
            .repoName("repoName")
            .metadata("key", "value")
            .stage(ArtifactStageEnum.RELEASE)
            .page(1, 10)
            .sort(Sort.Direction.ASC, "name")
            .select("projectId", "repoName", "fullPath", "metadata")
            .build()
        val interpreter = NodeQueryInterpreter(repoNameRuleInterceptor, repoTypeRuleInterceptor)
        val query = interpreter.interpret(queryModel)
        println(query.queryModel)
    }
}
