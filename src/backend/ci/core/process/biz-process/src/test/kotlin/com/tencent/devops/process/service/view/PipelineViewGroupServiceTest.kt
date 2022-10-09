package com.tencent.devops.process.service.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.Tables.T_PIPELINE_VIEW
import com.tencent.devops.model.process.tables.records.TPipelineViewGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.BkAbstractTest
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.dao.label.PipelineViewTopDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PipelineViewGroupServiceTest : BkAbstractTest() {
    private val pipelineViewService: PipelineViewService = mockk()
    private val pipelinePermissionService: PipelinePermissionService = mockk()
    private val pipelineViewDao: PipelineViewDao = mockk()
    private val pipelineViewGroupDao: PipelineViewGroupDao = mockk()
    private val pipelineViewTopDao: PipelineViewTopDao = mockk()
    private val pipelineInfoDao: PipelineInfoDao = mockk()
    private val redisOperation: RedisOperation = mockk()
    private val objectMapper: ObjectMapper = mockk()

    private val pipelineViewGroupService: PipelineViewGroupService = spyk(
        PipelineViewGroupService(
            pipelineViewService = pipelineViewService,
            pipelinePermissionService = pipelinePermissionService,
            pipelineViewDao = pipelineViewDao,
            pipelineViewGroupDao = pipelineViewGroupDao,
            pipelineViewTopDao = pipelineViewTopDao,
            pipelineInfoDao = pipelineInfoDao,
            dslContext = dslContext,
            redisOperation = redisOperation,
            objectMapper = objectMapper
        ),
        recordPrivateCalls = true
    )

    private val now = LocalDateTime.now()

    private val pvg = TPipelineViewGroupRecord(
        /*id*/1,
        /*projectId*/"test",
        /*viewId*/1,
        /*pipelineId*/"p-test",
        /*createTime*/now,
        /*creator*/"test"
    )

    private val pv = TPipelineViewRecord(
        /*id*/1,
        /*projectId*/"test",
        /*name*/"test",
        /*filterByPipeineName*/"",
        /*filterByCreator*/"",
        /*createTime*/now,
        /*updateTime*/ now,
        /*createUser*/"test",
        /*isProject*/true,
        /*logic*/"AND",
        /*filters*/"",
        /*viewType*/PipelineViewType.DYNAMIC
    )

    @BeforeEach
    fun mockPermissionTrue() {
        every { pipelinePermissionService.checkProjectManager("true", any()) } returns true
    }

    @BeforeEach
    fun mockPermissionFalse() {
        every { pipelinePermissionService.checkProjectManager("false", any()) } returns false
    }

    fun <R : Record> newResult(t: Table<R>): Result<R> {
        return DSL.using(SQLDialect.MYSQL).newResult(t)
    }

    @Nested
    inner class GetView {
        @Test
        @DisplayName("ViewGroup列表为空")
        fun test_1() {
            every { pipelineViewGroupDao.listByPipelineIds(any(), any(), any()) } returns emptyList()
            Assertions.assertTrue(pipelineViewGroupService.getViewNameMap("", mutableSetOf()).isEmpty())
        }

        @Test
        @DisplayName("View列表为空")
        fun test_2() {
            every { pipelineViewGroupDao.listByPipelineIds(any(), any(), any()) } returns listOf(pvg)

            val empty = newResult(T_PIPELINE_VIEW)
            every {
                pipelineViewDao.list(
                    dslContext = any(),
                    projectId = any(),
                    viewIds = any()
                )
            } returns empty

            Assertions.assertTrue(pipelineViewGroupService.getViewNameMap("", mutableSetOf()).isEmpty())
        }

        @Test
        @DisplayName("正常数据")
        fun test_3() {
            every { pipelineViewGroupDao.listByPipelineIds(any(), any(), any()) } returns listOf(pvg)

            val viewResult = newResult(T_PIPELINE_VIEW)
            viewResult.add(pv)
            every {
                pipelineViewDao.list(
                    dslContext = any(),
                    projectId = any(),
                    viewIds = any()
                )
            } returns viewResult

            val viewNameMap = pipelineViewGroupService.getViewNameMap("", mutableSetOf())
            Assertions.assertEquals(viewNameMap[pvg.pipelineId]?.get(0), pv.name)
        }
    }

    @Nested
    inner class ADDViewGroup {
        private val pipelineViewForm = PipelineViewForm(
            id = "test",
            name = "test",
            projected = true
        )

        @Test
        @DisplayName("项目流水线组 & 没有权限")
        fun test_1() {
            Assertions.assertThrows(ErrorCodeException::class.java) {
                pipelineViewGroupService.addViewGroup(
                    "test",
                    "false",
                    pipelineViewForm
                )
            }
        }

        @Test
        @DisplayName("项目流水线组 & 有权限")
        fun test_2() {
            val projectId = "test"
            val userId = "true"
            val viewId = 1L
            every { pipelineViewService.addView(any(), any(), any(), any()) } returns viewId
            justRun {
                pipelineViewGroupService["initViewGroup"](
                    anyDslContext(),
                    pipelineViewForm,
                    projectId,
                    viewId,
                    userId
                )
            }
            Assertions.assertDoesNotThrow {
                pipelineViewGroupService.addViewGroup(
                    projectId,
                    userId,
                    pipelineViewForm
                )
            }
        }
    }
}
