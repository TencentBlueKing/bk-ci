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

package com.tencent.devops.process.service.`var`

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.VarRefDetailDao
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupVersionSummaryDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.project.pojo.Result
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.script.RedisScript

/**
 * 覆盖计数体系最核心的“绝对值重算 + 覆盖 + 清零桶”算法（refreshVarLevelSummary）。
 * 这是本次公共变量引用计数准确性与一致性修复的核心逻辑，保证：
 * - 已存在的 (变量名,版本) 桶用绝对值覆盖（updateReferCount 命中）；
 * - 新出现的桶批量取号后插入（UPDATE 命中 0 行）；
 * - 已归零/消失的桶被删除（deleteByGroupNameExceptVarVersions 的 keep 精确等于重算结果）；
 * - 不做增量增减，因此不会累积误差、可自愈。
 */
class PublicVarGroupReferManageServiceTest {

    private val dslContext: DSLContext = mockk(relaxed = true)
    private val publicVarGroupDao: PublicVarGroupDao = mockk(relaxed = true)
    private val client: Client = mockk()
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao = mockk(relaxed = true)
    private val publicVarGroupVersionSummaryDao: PublicVarGroupVersionSummaryDao = mockk(relaxed = true)
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao = mockk(relaxUnitFun = true)
    private val templatePipelineDao: TemplatePipelineDao = mockk(relaxed = true)
    private val templateDao: TemplateDao = mockk(relaxed = true)
    private val publicVarDao: PublicVarDao = mockk(relaxed = true)
    private val sampleEventDispatcher: SampleEventDispatcher = mockk(relaxed = true)
    private val publicVarGroupReferCountService: PublicVarGroupReferCountService = mockk(relaxed = true)
    private val varRefDetailDao: VarRefDetailDao = mockk(relaxed = true)
    private val redisOperation: RedisOperation = mockk(relaxed = true)

    private val allocIdResource: ServiceAllocIdResource = mockk()

    private val service = PublicVarGroupReferManageService(
        dslContext = dslContext,
        publicVarGroupDao = publicVarGroupDao,
        client = client,
        publicVarGroupReferInfoDao = publicVarGroupReferInfoDao,
        publicVarGroupVersionSummaryDao = publicVarGroupVersionSummaryDao,
        publicVarVersionSummaryDao = publicVarVersionSummaryDao,
        templatePipelineDao = templatePipelineDao,
        templateDao = templateDao,
        publicVarDao = publicVarDao,
        sampleEventDispatcher = sampleEventDispatcher,
        publicVarGroupReferCountService = publicVarGroupReferCountService,
        varRefDetailDao = varRefDetailDao,
        redisOperation = redisOperation
    )

    companion object {
        private const val PROJECT = "proj"
        private const val USER = "user"
        private const val GROUP = "g1"
        private const val DYNAMIC = -1
        private const val VAR_SUMMARY_BIZ_TAG = "T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY"
    }

    /**
     * RedisLock.unlock() 会走 RedisOperation.execute(Lua) 释放锁；clearMocks 会清掉 relaxed 默认应答，
     * 未显式 stub 时 execute 会抛异常并触发 unlock() 的 catch 重试，污染 decorateKey 调用计数。
     * 这里用与 BkCiAbstractTest 一致的命名参数写法（args = anyVararg()）确保匹配成功、解锁一次即成功。
     */
    private fun stubUnlockExecute() {
        every {
            redisOperation.execute(
                script = any<RedisScript<Long>>(),
                keys = any(),
                args = anyVararg(),
                isRedisLock = any()
            )
        } returns 1L
    }

    /**
     * 已存在桶走覆盖更新，新桶批量取号后插入，keep 精确等于重算结果。
     * 动态版本(-1)与固定版本(5)分桶并存，验证两类桶都被正确处理。
     */
    @Test
    fun refreshVarLevelSummary_overwritesExisting_insertsNew_andPrunesZeroedBuckets() {
        val ctx = mockk<DSLContext>(relaxed = true)
        every {
            publicVarVersionSummaryDao.getLatestReferCountByVarAndVersion(ctx, PROJECT, GROUP)
        } returns mapOf(
            ("varA" to DYNAMIC) to 3, // 动态版本桶，已存在 -> 覆盖
            ("varB" to 5) to 1        // 固定版本桶，新出现 -> 插入
        )
        every {
            publicVarVersionSummaryDao.updateReferCount(ctx, PROJECT, GROUP, "varA", DYNAMIC, 3, USER)
        } returns 1
        every {
            publicVarVersionSummaryDao.updateReferCount(ctx, PROJECT, GROUP, "varB", 5, 1, USER)
        } returns 0
        every { client.get(ServiceAllocIdResource::class) } returns allocIdResource
        every {
            allocIdResource.batchGenerateSegmentId(VAR_SUMMARY_BIZ_TAG, 1)
        } returns Result(listOf<Long?>(9001L))

        service.refreshVarLevelSummary(
            context = ctx,
            projectId = PROJECT,
            userId = USER,
            groupNames = listOf(GROUP)
        )

        // 已存在桶：覆盖更新
        verify(exactly = 1) {
            publicVarVersionSummaryDao.updateReferCount(ctx, PROJECT, GROUP, "varA", DYNAMIC, 3, USER)
        }
        // 新桶：批量取号后插入，绝对值写入
        verify(exactly = 1) { allocIdResource.batchGenerateSegmentId(VAR_SUMMARY_BIZ_TAG, 1) }
        verify(exactly = 1) {
            publicVarVersionSummaryDao.save(
                ctx,
                match { it.id == 9001L && it.varName == "varB" && it.version == 5 && it.referCount == 1 }
            )
        }
        // 清理：keep 精确等于重算得到的 (变量名,版本) 集合
        verify(exactly = 1) {
            publicVarVersionSummaryDao.deleteByGroupNameExceptVarVersions(
                ctx,
                PROJECT,
                GROUP,
                match { it.toSet() == setOf("varA" to DYNAMIC, "varB" to 5) }
            )
        }
    }

    /**
     * 变量组已无任何生效引用：不更新、不插入、不取号，仅删除全部桶（keep 为空）。
     * 覆盖“引用归零后计数被清理”的关键路径（删除保护据此判定变量组可删）。
     */
    @Test
    fun refreshVarLevelSummary_whenNoActiveRefer_clearsAllBuckets() {
        val ctx = mockk<DSLContext>(relaxed = true)
        every {
            publicVarVersionSummaryDao.getLatestReferCountByVarAndVersion(ctx, PROJECT, GROUP)
        } returns emptyMap()

        service.refreshVarLevelSummary(
            context = ctx,
            projectId = PROJECT,
            userId = USER,
            groupNames = listOf(GROUP)
        )

        verify(exactly = 0) {
            publicVarVersionSummaryDao.updateReferCount(any(), any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 0) { publicVarVersionSummaryDao.save(any(), any()) }
        verify(exactly = 0) { client.get(ServiceAllocIdResource::class) }
        verify(exactly = 1) {
            publicVarVersionSummaryDao.deleteByGroupNameExceptVarVersions(
                ctx, PROJECT, GROUP, match { it.isEmpty() }
            )
        }
    }

    /**
     * 空的受影响组集合：整体空操作，绝不触碰 DAO / 远程取号。
     */
    @Test
    fun refreshVarLevelSummary_whenNoGroups_isNoOp() {
        val ctx = mockk<DSLContext>(relaxed = true)

        service.refreshVarLevelSummary(
            context = ctx,
            projectId = PROJECT,
            userId = USER,
            groupNames = emptyList()
        )

        verify(exactly = 0) {
            publicVarVersionSummaryDao.getLatestReferCountByVarAndVersion(any(), any(), any())
        }
        verify(exactly = 0) { client.get(ServiceAllocIdResource::class) }
    }

    /**
     * 多个变量组同时出现新桶时，只做一次批量取号（number=新桶总数），验证批量取号（减少远程调用）优化。
     */
    @Test
    fun refreshVarLevelSummary_batchesIdAllocationAcrossGroups() {
        val ctx = mockk<DSLContext>(relaxed = true)
        every {
            publicVarVersionSummaryDao.getLatestReferCountByVarAndVersion(ctx, PROJECT, "g1")
        } returns mapOf(("a" to DYNAMIC) to 1)
        every {
            publicVarVersionSummaryDao.getLatestReferCountByVarAndVersion(ctx, PROJECT, "g2")
        } returns mapOf(("b" to DYNAMIC) to 2)
        every {
            publicVarVersionSummaryDao.updateReferCount(ctx, PROJECT, "g1", "a", DYNAMIC, 1, USER)
        } returns 0
        every {
            publicVarVersionSummaryDao.updateReferCount(ctx, PROJECT, "g2", "b", DYNAMIC, 2, USER)
        } returns 0
        every { client.get(ServiceAllocIdResource::class) } returns allocIdResource
        every {
            allocIdResource.batchGenerateSegmentId(VAR_SUMMARY_BIZ_TAG, 2)
        } returns Result(listOf<Long?>(1L, 2L))

        service.refreshVarLevelSummary(
            context = ctx,
            projectId = PROJECT,
            userId = USER,
            groupNames = listOf("g1", "g2")
        )

        // 关键：跨组只取号一次，number 等于新桶总数
        verify(exactly = 1) { allocIdResource.batchGenerateSegmentId(VAR_SUMMARY_BIZ_TAG, 2) }
        verify(exactly = 1) {
            publicVarVersionSummaryDao.save(ctx, match { it.varName == "a" && it.referCount == 1 })
        }
        verify(exactly = 1) {
            publicVarVersionSummaryDao.save(ctx, match { it.varName == "b" && it.referCount == 2 })
        }
    }

    /**
     * 组级串行化锁：多组必须按 groupName 排序加锁（避免 AB-BA 死锁），且 action 被执行、返回值透传。
     */
    @Test
    fun executeWithGroupSummaryLocks_acquiresLocksInSortedOrder_andRunsAction() {
        clearMocks(redisOperation)
        val acquiredKeys = mutableListOf<String>()
        every { redisOperation.getKeyByRedisName(any()) } answers { firstArg() }
        every { redisOperation.setNxEx(capture(acquiredKeys), any(), any(), any()) } returns true
        stubUnlockExecute()

        var executed = false
        val result = service.executeWithGroupSummaryLocks(PROJECT, listOf("groupB", "groupA", "groupC")) {
            executed = true
            "ok"
        }

        Assertions.assertTrue(executed)
        Assertions.assertEquals("ok", result)
        // 锁 key 形如 "...:summary:proj:<groupName>"，取末段即组名，验证加锁顺序为字典序
        val lockedGroupsInOrder = acquiredKeys.map { it.substringAfterLast(":") }
        Assertions.assertEquals(listOf("groupA", "groupB", "groupC"), lockedGroupsInOrder)
    }

    /**
     * action 抛异常时，异常向上传播，且已获取的锁全部释放（不泄漏）。
     * 注意：类级 redisOperation mock 会跨用例累积调用，必须 clearMocks 后再断言次数。
     * 释放校验用 decorateKey（getKeyByRedisName）的调用次数间接衡量：
     * 每把锁 lock 一次 + unlock 一次各调用一次 decorateKey，2 组共 4 次。
     * 前提：stubUnlockExecute() 保证 unlock 的 Lua 一次成功、不走 catch 重试，计数才稳定为 4。
     */
    @Test
    fun executeWithGroupSummaryLocks_releasesLocksOnException() {
        clearMocks(redisOperation)
        every { redisOperation.getKeyByRedisName(any()) } answers { firstArg() }
        every { redisOperation.setNxEx(any(), any(), any(), any()) } returns true
        stubUnlockExecute()

        val boom = RuntimeException("boom")
        val thrown = Assertions.assertThrows(RuntimeException::class.java) {
            service.executeWithGroupSummaryLocks(PROJECT, listOf("g1", "g2")) {
                throw boom
            }
        }
        Assertions.assertEquals("boom", thrown.message)
        // 2 组加锁
        verify(exactly = 2) { redisOperation.setNxEx(any(), any(), any(), any()) }
        // 2 组：加锁 2 次 + 解锁 2 次，decorateKey 共调用 getKeyByRedisName 4 次，证明锁已释放
        verify(exactly = 4) { redisOperation.getKeyByRedisName(any()) }
    }

    /**
     * 空组集合：不加任何锁，直接执行 action。
     */
    @Test
    fun executeWithGroupSummaryLocks_withEmptyGroups_runsActionWithoutLocking() {
        clearMocks(redisOperation)
        val result = service.executeWithGroupSummaryLocks(PROJECT, emptyList()) { 42 }

        Assertions.assertEquals(42, result)
        verify(exactly = 0) { redisOperation.setNxEx(any(), any(), any(), any()) }
    }
}
