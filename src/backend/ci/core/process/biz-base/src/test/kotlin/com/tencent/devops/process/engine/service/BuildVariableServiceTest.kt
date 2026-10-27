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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.constant.HIDDEN_SYMBOL
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.service.BuildVarOverflowStorage
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.PipelineVarOverflowConfig
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_VERSION
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.jooq.TransactionalRunnable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BuildVariableServiceTest {

    private val dslContext: DSLContext = mockk()
    private val pipelineBuildVarDao: PipelineBuildVarDao = mockk()
    private val redisOperation: RedisOperation = RedisOperation(
        masterRedisTemplate = mockk(),
        slaveRedisTemplate = mockk(),
        lockRedisTemplate = mockk(),
        splitMode = mockk()
    )
    private val pipelineAsCodeService: PipelineAsCodeService = mockk(relaxed = true)
    private val buildVarOverflowStorage: BuildVarOverflowStorage = mockk(relaxed = true)

    private val buildVariableService = BuildVariableService(
        commonDslContext = dslContext,
        pipelineBuildVarDao = pipelineBuildVarDao,
        redisOperation = redisOperation,
        pipelineAsCodeService = pipelineAsCodeService,
        pipelineVarOverflowConfig = PipelineVarOverflowConfig(),
        buildVarOverflowStorage = buildVarOverflowStorage
    )

    @Test
    fun getAllVariable() {
        val projectId = "devops"
        val buildId = "b-1234567890"
        val pipelineId = "p-1234567890"

        val mockVars = mutableMapOf(
            "pipeline.start.channel" to "BS",
            "pipeline.start.isMobile" to "false",
            "pipeline.name" to "流水线名称",
            "pipeline.build.num" to "210",
            "pipeline.start.type" to "SERVICE",
            "pipeline.start.user.id" to "user123",
            "pipeline.start.user.name" to "人名",
            "pipeline.version" to "34"
        )

        every { pipelineBuildVarDao.getVars(dslContext, projectId, buildId) } returns (mockVars)
        val allVariable = buildVariableService.getAllVariable(projectId, pipelineId, buildId)

        Assertions.assertEquals(mockVars["pipeline.start.channel"], allVariable[PIPELINE_START_CHANNEL])
        Assertions.assertEquals(allVariable["pipeline.start.channel"], allVariable[PIPELINE_START_CHANNEL])

        Assertions.assertEquals(mockVars["pipeline.start.isMobile"], allVariable[PIPELINE_START_MOBILE])
        Assertions.assertEquals(allVariable["pipeline.start.isMobile"], allVariable[PIPELINE_START_MOBILE])

        Assertions.assertEquals(mockVars["pipeline.name"], allVariable[PIPELINE_NAME])
        Assertions.assertEquals(allVariable["pipeline.name"], allVariable[PIPELINE_NAME])

        Assertions.assertEquals(mockVars["pipeline.build.num"], allVariable[PIPELINE_BUILD_NUM])
        Assertions.assertEquals(allVariable["pipeline.build.num"], allVariable[PIPELINE_BUILD_NUM])

        Assertions.assertEquals(mockVars["pipeline.name"], allVariable[PIPELINE_NAME])
        Assertions.assertEquals(allVariable["pipeline.name"], allVariable[PIPELINE_NAME])

        Assertions.assertEquals(mockVars["pipeline.start.type"], allVariable[PIPELINE_START_TYPE])
        Assertions.assertEquals(allVariable["pipeline.start.type"], allVariable[PIPELINE_START_TYPE])

        Assertions.assertEquals(mockVars["pipeline.start.user.id"], allVariable[PIPELINE_START_USER_ID])
        Assertions.assertEquals(allVariable["pipeline.start.user.id"], allVariable[PIPELINE_START_USER_ID])

        Assertions.assertEquals(mockVars["pipeline.start.user.name"], allVariable[PIPELINE_START_USER_NAME])
        Assertions.assertEquals(allVariable["pipeline.start.user.name"], allVariable[PIPELINE_START_USER_NAME])

        Assertions.assertEquals(mockVars["pipeline.version"], allVariable[PIPELINE_VERSION])
        Assertions.assertEquals(allVariable["pipeline.version"], allVariable[PIPELINE_VERSION])
    }

    /** 重试后变量表只剩最后一次的引用串，带上第 1 次执行的引用串应读回第 1 次的内容。 */
    @Test
    fun `reference wins over the current value in var table`() {
        val firstRef = reference(executeCount = 1, digest = "a")
        stubVar(BuildParameters(key = VAR_NAME, value = reference(executeCount = 2, digest = "b")))
        every { buildVarOverflowStorage.download(null, PROJECT_ID, PIPELINE_ID, BUILD_ID, firstRef) } returns "第一次"

        Assertions.assertEquals("第一次", getVariableValue(reference = firstRef))
    }

    /** VAR 表是分区表，保留期可能短于构建记录；那一行没了也要能按日志里的引用串取到内容。 */
    @Test
    fun `reference still works when the var row is gone`() {
        val ref = reference(executeCount = 1, digest = "a")
        every { pipelineBuildVarDao.getVarsWithType(dslContext, PROJECT_ID, BUILD_ID, VAR_NAME) } returns emptyList()
        every { buildVarOverflowStorage.download(null, PROJECT_ID, PIPELINE_ID, BUILD_ID, ref) } returns "内容"

        Assertions.assertEquals("内容", getVariableValue(reference = ref))
    }

    /** 敏感判定仍按变量名先行，带了引用串也不能绕过脱敏。 */
    @Test
    fun `sensitive variable is masked even with a reference`() {
        stubVar(BuildParameters(key = VAR_NAME, value = "any", sensitive = true))

        Assertions.assertEquals(HIDDEN_SYMBOL, getVariableValue(reference = reference(1, "a")))
    }

    /** 敏感大变量也要存进制品库，否则后续插件取不到值；脱敏靠变量表的敏感标记，不靠丢弃。 */
    @Test
    fun `sensitive large value is stored instead of discarded`() {
        val raw = "x".repeat(5000)
        val ref = reference(executeCount = 1, digest = "c")
        every { buildVarOverflowStorage.isEnabled() } returns true
        every { buildVarOverflowStorage.upload(any(), raw) } returns ref
        every {
            pipelineBuildVarDao.getVars(dslContext, PROJECT_ID, BUILD_ID, any())
        } returns mutableMapOf(PIPELINE_NAME to "流水线", PIPELINE_BUILD_NUM to "1")
        every { dslContext.transaction(any<TransactionalRunnable>()) } returns Unit

        buildVariableService.batchUpdateVariable(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            variables = mapOf(VAR_NAME to raw),
            sensitiveKeys = setOf(VAR_NAME)
        )

        verify(exactly = 1) { buildVarOverflowStorage.upload(any(), raw) }
    }

    /** 老构建机日志里的占位只有长度段，解析不出引用，按变量表当前值返回。 */
    @Test
    fun `malformed reference falls back to the current value`() {
        stubVar(BuildParameters(key = VAR_NAME, value = "短值"))

        Assertions.assertEquals("短值", getVariableValue(reference = "__BK_OVF__:123"))
    }

    @Test
    fun `missing variable without reference returns null`() {
        every { pipelineBuildVarDao.getVarsWithType(dslContext, PROJECT_ID, BUILD_ID, VAR_NAME) } returns emptyList()

        Assertions.assertNull(getVariableValue(reference = null))
    }

    private fun getVariableValue(reference: String?) = buildVariableService.getVariableValue(
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        buildId = BUILD_ID,
        varName = VAR_NAME,
        reference = reference,
        maskSensitive = true
    )

    private fun stubVar(param: BuildParameters) {
        every {
            pipelineBuildVarDao.getVarsWithType(dslContext, PROJECT_ID, BUILD_ID, VAR_NAME)
        } returns listOf(param)
    }

    /** [digest] 只取首字符铺满 64 位，够区分不同执行次数的文件即可。 */
    private fun reference(executeCount: Int, digest: String) =
        BuildVarOverflowUtils.toReference(
            BuildVarOverflowUtils.LargeVarRef(
                length = 5000,
                executeCount = executeCount,
                digest = digest.repeat(64)
            )
        )

    companion object {
        private const val PROJECT_ID = "devops"
        private const val PIPELINE_ID = "p-1234567890"
        private const val BUILD_ID = "b-1234567890"
        private const val VAR_NAME = "MY_JSON"
    }
}
