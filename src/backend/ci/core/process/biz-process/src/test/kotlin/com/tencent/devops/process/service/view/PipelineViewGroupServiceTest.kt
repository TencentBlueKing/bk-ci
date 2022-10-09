package com.tencent.devops.process.service.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.Tables.T_PIPELINE_VIEW
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.BkAbstractTest
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.dao.label.PipelineViewTopDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
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

    private val self: PipelineViewGroupService = spyk(
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

    private val p = TPipelineInfoRecord(

    )

    private val pipelineViewForm = PipelineViewForm(
        id = "test",
        name = "test",
        projected = true
    )

    @BeforeEach
    fun mockViewId() {
        mockkObject(HashUtil)
        every { HashUtil.decodeIdToLong(any()) } returns 1L
        justRun { self["checkPermission"](any() as String, any() as String, any() as Boolean, any() as String?) }
    }

    @Nested
    inner class GetViewNameMap {
        @Test
        @DisplayName("ViewGroup列表为空")
        fun test_1() {
            every { pipelineViewGroupDao.listByPipelineIds(any(), any(), any()) } returns emptyList()
            Assertions.assertTrue(self.getViewNameMap("", mutableSetOf()).isEmpty())
        }

        @Test
        @DisplayName("View列表为空")
        fun test_2() {
            every { pipelineViewGroupDao.listByPipelineIds(any(), any(), any()) } returns listOf(pvg)

            every {
                pipelineViewDao.list(
                    dslContext = any(),
                    projectId = any(),
                    viewIds = any()
                )
            } returns dslContext.mockResult(T_PIPELINE_VIEW)

            Assertions.assertTrue(self.getViewNameMap("", mutableSetOf()).isEmpty())
        }

        @Test
        @DisplayName("正常数据")
        fun test_3() {
            every { pipelineViewGroupDao.listByPipelineIds(any(), any(), any()) } returns listOf(pvg)

            every {
                pipelineViewDao.list(
                    dslContext = any(),
                    projectId = any(),
                    viewIds = any()
                )
            } returns dslContext.mockResult(T_PIPELINE_VIEW, pv)

            val viewNameMap = self.getViewNameMap("", mutableSetOf())
            Assertions.assertEquals(viewNameMap[pvg.pipelineId]?.get(0), pv.name)
        }
    }

    @Nested
    inner class ADDViewGroup {


        @BeforeEach
        fun mockPermissionTrue() {
            every { pipelinePermissionService.checkProjectManager("true", any()) } returns true
        }

        @BeforeEach
        fun mockPermissionFalse() {
            every { pipelinePermissionService.checkProjectManager("false", any()) } returns false
        }

        @Test
        @DisplayName("项目流水线组 & 没有权限")
        fun test_1() {
            Assertions.assertThrows(ErrorCodeException::class.java) {
                self.addViewGroup(
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
                self["initViewGroup"](
                    anyDslContext(),
                    pipelineViewForm,
                    projectId,
                    viewId,
                    userId
                )
            }
            Assertions.assertDoesNotThrow {
                self.addViewGroup(
                    projectId,
                    userId,
                    pipelineViewForm
                )
            }
        }
    }

    @Nested
    inner class UpdateViewGroup {
        @Test
        @DisplayName("获取不到View")
        fun test_1() {
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns null

            try {
                self.updateViewGroup("test", "test", "test", pipelineViewForm)
            } catch (e: Throwable) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND
                )
            }
        }

        @Test
        @DisplayName("有view & 权限通过 & 两个组的范围不一致")
        fun test_2() {
            val pipelineViewFormCopy = pipelineViewForm.copy(projected = true)
            val pvCopy = pv.copy()
            pvCopy.isProject = false
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pvCopy
            try {
                self.updateViewGroup("test", "test", "test", pipelineViewFormCopy)
            } catch (e: Throwable) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_VIEW_GROUP_IS_PROJECT_NO_SAME
                )
            }
        }

        @Test
        @DisplayName("有view & 权限通过 & 范围一致 & 正常更新")
        fun test_3() {
            val pipelineViewFormCopy = pipelineViewForm.copy(projected = true)
            val pvCopy = pv.copy()
            pvCopy.isProject = true
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pvCopy
            every { pipelineViewService.updateView(any(), any(), any(), any(), anyDslContext()) } returns true
            justRun { pipelineViewGroupDao.remove(anyDslContext(), any(), any()) }
            every { self["firstInitMark"](any() as String, any() as Long) } returns "test"
            justRun { redisOperation.delete(any() as String) }
            justRun {
                self["initViewGroup"](
                    anyDslContext(),
                    pipelineViewFormCopy,
                    any() as String,
                    any() as Long,
                    any() as String
                )
            }
            Assertions.assertTrue(self.updateViewGroup("test", "test", "test", pipelineViewFormCopy))
        }
    }

    @Nested
    inner class GetView {
        @Test
        @DisplayName("View为空")
        fun test_1() {
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns null
            try {
                self.getView("test", "test", "test")
            } catch (e: Throwable) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND
                )
            }
        }

        @Test
        @DisplayName("View不为空 , filter返回空列表 , ViewGroup返回空列表 , 正常返回")
        fun test_2() {
            val viewId = "test"
            val projectId = "test"
            val userId = "test"

            val pvCopy = pv.copy()
            pvCopy.projectId = projectId
            pvCopy.createUser = userId

            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pv
            every { pipelineViewService.getFilters(any(), any(), any()) } returns emptyList()
            every { pipelineViewGroupDao.listByViewId(anyDslContext(), any(), any()) } returns emptyList()
            self.getView(userId, projectId, viewId).let {
                Assertions.assertTrue(it.filters.isEmpty())
                Assertions.assertTrue(it.pipelineIds.isEmpty())
                Assertions.assertEquals(it.id, viewId)
                Assertions.assertEquals(it.projectId, projectId)
                Assertions.assertEquals(it.creator, userId)
            }
        }
    }

    @Nested
    inner class DeleteViewGroup {
        @Test
        @DisplayName("获取不到view")
        fun test_1() {
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns null
            try {
                self.deleteViewGroup("test", "test", "test")
            } catch (e: Throwable) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_PIPELINE_VIEW_NOT_FOUND
                )
            }
        }

        @Test
        @DisplayName("获取到view , 正常执行")
        fun test_2() {
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pv
            every { pipelineViewService.deleteView(any(), any(), any()) } returns true
            justRun { pipelineViewGroupDao.remove(anyDslContext(), any(), any()) }
            Assertions.assertTrue(self.deleteViewGroup("test", "test", "test"))
        }
    }

    @Nested
    inner class GetClassifiedPipelineIds {
        @Test
        @DisplayName("正常执行")
        fun test_1() {
            every { pipelineViewGroupDao.distinctPipelineIds(anyDslContext(), any()) } returns listOf("test")
            self.getClassifiedPipelineIds("test").let {
                Assertions.assertTrue(it.size == 1)
                Assertions.assertTrue(it[0] == "test")
            }
        }
    }

    @Nested
    inner class ListPipelineIdsByViewIds {
        @Test
        @DisplayName("当ViewGroup为空列表时")
        fun test_1() {
            every { pipelineViewGroupDao.listByViewIds(anyDslContext(), any(), any()) } returns emptyList()
            self.listPipelineIdsByViewIds("test", listOf("test")).let {
                Assertions.assertTrue(it.size == 1)
                Assertions.assertEquals(it[0], "##NONE##")
            }
        }

        @Test
        @DisplayName("当ViewGroup不为空列表时")
        fun test_2() {
            every { pipelineViewGroupDao.listByViewIds(anyDslContext(), any(), any()) } returns listOf(pvg)
            self.listPipelineIdsByViewIds("test", listOf("test")).let {
                Assertions.assertTrue(it.size == 1)
                Assertions.assertEquals(it[0], pvg.pipelineId)
            }
        }
    }

    @Nested
    inner class ListPipelineIdsByViewId {
        @Test
        @DisplayName("正常返回")
        fun test_1() {
            every {
                self["listPipelineIdsByViewIds"](
                    any() as String,
                    any() as List<String>
                )
            } returns emptyList<String>()
            Assertions.assertTrue(self.listPipelineIdsByViewId("test", "test").isEmpty())
        }
    }

    @Nested
    inner class UpdateGroupAfterPipelineCreate{
        @Test
        @DisplayName("正常执行")
        fun test_1(){
            every { pipelineInfoDao.getPipelineId(anyDslContext(),any(),any()) } returns p
            every { pipelineViewGroupDao.countByPipelineId(anyDslContext(),any(),any()) } returns 1
            Assertions.assertDoesNotThrow(){self.updateGroupAfterPipelineCreate("test","test","test")}
        }
    }
}
