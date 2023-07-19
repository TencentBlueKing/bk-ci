package com.tencent.devops.process.service.view

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.model.process.Tables.T_PIPELINE_INFO
import com.tencent.devops.model.process.Tables.T_PIPELINE_VIEW
import com.tencent.devops.model.process.Tables.T_PIPELINE_VIEW_GROUP
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewGroupRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewTopRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.dao.label.PipelineViewTopDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewBulkAdd
import com.tencent.devops.process.pojo.classify.PipelineViewBulkRemove
import com.tencent.devops.process.pojo.classify.PipelineViewDict
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewPipelineCount
import com.tencent.devops.process.pojo.classify.PipelineViewPreview
import com.tencent.devops.process.utils.PIPELINE_VIEW_UNCLASSIFIED
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SpringContextUtil::class, CommonConfig::class])
class PipelineViewGroupServiceTest : BkCiAbstractTest() {
    private val pipelineViewService: PipelineViewService = mockk()
    private val pipelinePermissionService: PipelinePermissionService = mockk()
    private val pipelineViewDao: PipelineViewDao = mockk()
    private val pipelineViewGroupDao: PipelineViewGroupDao = mockk()
    private val pipelineViewTopDao: PipelineViewTopDao = mockk()
    private val pipelineInfoDao: PipelineInfoDao = mockk()
    private val clientTokenService: ClientTokenService = mockk()

    private val self: PipelineViewGroupService = spyk(
        PipelineViewGroupService(
            pipelineViewService = pipelineViewService,
            pipelineViewDao = pipelineViewDao,
            pipelineViewGroupDao = pipelineViewGroupDao,
            pipelineViewTopDao = pipelineViewTopDao,
            pipelineInfoDao = pipelineInfoDao,
            dslContext = dslContext,
            redisOperation = redisOperation,
            objectMapper = objectMapper,
            client = client,
            clientTokenService = clientTokenService
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

    private val pi = TPipelineInfoRecord(
        "p-test", //        setPipelineId(pipelineId);
        "test", //    setProjectId(projectId);
        "test", //    setPipelineName(pipelineName);
        "test", //    setPipelineDesc(pipelineDesc);
        1, //    setVersion(version);
        now, //    setCreateTime(createTime);
        "test", //    setCreator(creator);
        now, //    setUpdateTime(updateTime);
        "test", //    setLastModifyUser(lastModifyUser);
        "test", //    setChannel(channel);
        1, //    setManualStartup(manualStartup);
        1, //    setElementSkip(elementSkip);
        1, //    setTaskCount(taskCount);
        false, //    setDelete(delete);
        1, //    setId(id);
        "test", //    setPipelineNamePinyin(pipelineNamePinyin);
        now //    setLatestStartTime(latestStartTime);
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
        fun beforeEach() {
            every { pipelinePermissionService.checkProjectManager("true", any()) } returns true
            every { pipelinePermissionService.checkProjectManager("false", any()) } returns false
            justRun { self["checkPermission"](any() as String, any() as String, any() as Boolean, any() as String?) }
        }

        @Test
        @DisplayName("项目流水线组 & 有权限")
        fun test_1() {
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
        @BeforeEach
        fun beforeEach() {
            justRun { self["checkPermission"](any() as String, any() as String, any() as Boolean, any() as String?) }
        }

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
        @BeforeEach
        fun beforeEach() {
            justRun { self["checkPermission"](any() as String, any() as String, any() as Boolean, any() as String?) }
        }

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
            every { pipelineViewGroupDao.distinctPipelineIds(anyDslContext(), any(), any()) } returns listOf("test")
            every {
                pipelineViewDao.list(
                    anyDslContext(),
                    any() as String,
                    any() as Boolean
                )
            } returns dslContext.mockResult(
                T_PIPELINE_VIEW, pv
            )
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
    inner class UpdateGroupAfterPipelineCreate {
        @Test
        @DisplayName("正常执行 , viewGroup 数量不为0")
        fun test_1() {
            every { pipelineInfoDao.getPipelineId(anyDslContext(), any(), any()) } returns pi
            every { pipelineViewGroupDao.countByPipelineId(anyDslContext(), any(), any()) } returns 1
            Assertions.assertDoesNotThrow { self.updateGroupAfterPipelineCreate("test", "test", "test") }
        }

        @Test
        @DisplayName("正常执行 , viewGroup 数量为0")
        fun test_2() {
            every { pipelineInfoDao.getPipelineId(anyDslContext(), any(), any()) } returns pi
            every { pipelineViewGroupDao.countByPipelineId(anyDslContext(), any(), any()) } returns 0
            every {
                pipelineViewDao.list(anyDslContext(), any() as String, any() as Int)
            } returns dslContext.mockResult(T_PIPELINE_VIEW, pv)
            every { pipelineViewService.matchView(any(), any()) } returns true
            justRun { pipelineViewGroupDao.create(anyDslContext(), any(), any(), any(), any()) }
            Assertions.assertDoesNotThrow { self.updateGroupAfterPipelineCreate("test", "test", "test") }
        }
    }

    @Nested
    inner class UpdateGroupAfterPipelineUpdate {
        @Test
        @DisplayName("正常调用 , 没有新命中的流水线组 , 没有需要删除的流水线组")
        fun test_1() {
            every { pipelineInfoDao.getPipelineId(anyDslContext(), any(), any()) } returns pi
            every {
                pipelineViewDao.list(anyDslContext(), any() as String, any() as Int)
            } returns dslContext.mockResult(T_PIPELINE_VIEW, pv)
            every { pipelineViewService.matchView(any(), any()) } returns true
            every { pipelineViewGroupDao.listByPipelineId(anyDslContext(), any(), any()) } returns listOf(pvg)
            justRun { pipelineViewGroupDao.create(anyDslContext(), any(), any(), any(), any()) }
            justRun { pipelineViewGroupDao.remove(anyDslContext(), any(), any(), any()) }
            Assertions.assertDoesNotThrow { self.updateGroupAfterPipelineUpdate("test", "p-test", "test") }
        }
    }

    @Nested
    inner class InitVIewGroup {
        @Test
        @DisplayName("初始化动态项目组")
        fun test_1() {
            val pipelineViewFormCopy = pipelineViewForm.copy(viewType = PipelineViewType.DYNAMIC)
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pv
            every {
                self["initDynamicViewGroup"](
                    any<TPipelineViewRecord>(),
                    any<String>(),
                    anyDslContext()
                )
            } returns emptyList<String>()
            Assertions.assertDoesNotThrow {
                self.invokePrivate<Unit>("initViewGroup", dslContext, pipelineViewFormCopy, "test", 1, "test")
            }
        }

        @Test
        @DisplayName("初始化静态项目组")
        fun test_2() {
            val pipelineViewFormCopy =
                pipelineViewForm.copy(viewType = PipelineViewType.STATIC, pipelineIds = listOf("1"))
            justRun { pipelineViewGroupDao.create(anyDslContext(), any(), any(), any(), any()) }
            Assertions.assertDoesNotThrow {
                self.invokePrivate<Unit>("initViewGroup", dslContext, pipelineViewFormCopy, "test", 1, "test")
            }
        }
    }

    @Nested
    inner class InitDynamicViewGroup {
        @Test
        @DisplayName("不是首次初始化")
        fun test_1() {
            every { redisOperation.setIfAbsent(any(), any()) } returns false
            self.invokePrivate<List<String>>("initDynamicViewGroup", pv, "test", dslContext).let {
                Assertions.assertTrue(it!!.isEmpty())
            }
        }

        @Test
        @DisplayName("首次初始化")
        fun test_2() {
            every { redisOperation.setIfAbsent(any(), any(), any(), any()) } returns true
            every { self["allPipelineInfos"](any() as String, any() as Boolean) } returns listOf(pi)
            every { pipelineViewService.matchView(any(), any()) } returns true
            every { pipelineViewGroupDao.create(anyDslContext(), any(), any(), any(), any()) } returns Unit
            self.invokePrivate<List<String>>("initDynamicViewGroup", pv, "test", dslContext).let {
                Assertions.assertEquals(it!!.size, 1)
                Assertions.assertEquals(it[0], "p-test")
            }
        }
    }

    @Nested
    inner class CheckPermission {
        @Test
        @DisplayName("项目流水线组 & 校验不通过")
        fun test_1() {
            every { self["hasPermission"](any() as String, any() as String) } returns false
            try {
                self.invokePrivate<Unit>("checkPermission", "test", "test", true, "test")
            } catch (e: Throwable) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_VIEW_GROUP_NO_PERMISSION
                )
            }
        }

        @Test
        @DisplayName("个人流水线组 & 校验不通过")
        fun test_2() {
            try {
                self.invokePrivate<Unit>("checkPermission", "test", "test", false, "test")
            } catch (e: Throwable) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_DEL_PIPELINE_VIEW_NO_PERM
                )
            }
        }
    }

    @Nested
    inner class Preview {
        @Test
        @DisplayName("流水线列表为空")
        fun test_1() {
            every {
                self["allPipelineInfos"](
                    any() as String,
                    any() as Boolean
                )
            } returns emptyList<TPipelineInfoRecord>()
            self.preview("test", "test", pipelineViewForm).let {
                Assertions.assertEquals(it, PipelineViewPreview.EMPTY)
            }
        }

        @Test
        @DisplayName("viewId为空 , viewType为动态项目组")
        fun test_2() {
            val pipelineViewFormCopy = pipelineViewForm.copy(id = null, viewType = PipelineViewType.DYNAMIC)
            every {
                self["allPipelineInfos"](
                    any() as String,
                    any() as Boolean
                )
            } returns listOf(pi)
            every { pipelineViewService.matchView(any(), any()) } returns true
            self.preview("test", "test", pipelineViewFormCopy).let {
                Assertions.assertTrue(it.addedPipelineInfos.size == 1)
                Assertions.assertTrue(it.removedPipelineInfos.isEmpty())
                Assertions.assertTrue(it.reservePipelineInfos.isEmpty())
            }
        }

        @Test
        @DisplayName("viewId为空,viewType为静态项目组")
        fun test_3() {
            val pipelineViewFormCopy =
                pipelineViewForm.copy(id = null, viewType = PipelineViewType.STATIC, pipelineIds = listOf("p-test"))
            every {
                self["allPipelineInfos"](
                    any() as String,
                    any() as Boolean
                )
            } returns listOf(pi)
            every { pipelineViewService.matchView(any(), any()) } returns true
            self.preview("test", "test", pipelineViewFormCopy).let {
                Assertions.assertTrue(it.addedPipelineInfos.size == 1)
                Assertions.assertTrue(it.removedPipelineInfos.isEmpty())
                Assertions.assertTrue(it.reservePipelineInfos.isEmpty())
            }
        }

        @Test
        @DisplayName("viewId不为空,viewType为动态项目组")
        fun test_4() {
            val pipelineViewFormCopy = pipelineViewForm.copy(id = "test", viewType = PipelineViewType.DYNAMIC)
            every {
                self["allPipelineInfos"](
                    any() as String,
                    any() as Boolean
                )
            } returns listOf(pi)
            every { pipelineViewGroupDao.listByViewId(anyDslContext(), any(), any()) } returns emptyList()
            every { pipelineViewService.matchView(any(), any()) } returns true
            self.preview("test", "test", pipelineViewFormCopy).let {
                Assertions.assertTrue(it.addedPipelineInfos.size == 1)
                Assertions.assertTrue(it.removedPipelineInfos.isEmpty())
                Assertions.assertTrue(it.reservePipelineInfos.isEmpty())
            }
        }

        @Test
        @DisplayName("viewId不为空,viewType为静态项目组")
        fun test_5() {
            val pipelineViewFormCopy =
                pipelineViewForm.copy(id = "test", viewType = PipelineViewType.STATIC, pipelineIds = listOf("p-test"))
            every {
                self["allPipelineInfos"](
                    any() as String,
                    any() as Boolean
                )
            } returns listOf(pi)
            every { pipelineViewGroupDao.listByViewId(anyDslContext(), any(), any()) } returns emptyList()
            every { pipelineViewService.matchView(any(), any()) } returns true
            self.preview("test", "test", pipelineViewFormCopy).let {
                Assertions.assertTrue(it.addedPipelineInfos.size == 1)
                Assertions.assertTrue(it.removedPipelineInfos.isEmpty())
                Assertions.assertTrue(it.reservePipelineInfos.isEmpty())
            }
        }
    }

    @Nested
    inner class Dict {
        @Test
        @DisplayName("ViewInfo列表 不为空 , viewInfoGroup列表不为空, pipelineInfo列表为空")
        fun test_1() {
            every { pipelineViewDao.list(anyDslContext(), any()) } returns dslContext.mockResult(T_PIPELINE_VIEW, pv)
            every {
                pipelineViewGroupDao.listByProjectId(anyDslContext(), any())
            } returns dslContext.mockResult(T_PIPELINE_VIEW_GROUP, pvg)
            every {
                self["allPipelineInfos"](
                    any() as String,
                    any() as Boolean
                )
            } returns emptyList<TPipelineInfoRecord>()
            self.dict("test", "test").let {
                Assertions.assertEquals(it, PipelineViewDict.EMPTY)
            }
        }

        @Test
        @DisplayName("ViewInfo列表 不为空 , viewInfoGroup列表不为空, pipelineInfo列表不为空")
        fun test_2() {
            every { pipelineViewDao.list(anyDslContext(), any()) } returns dslContext.mockResult(T_PIPELINE_VIEW, pv)
            every {
                pipelineViewGroupDao.listByProjectId(anyDslContext(), any())
            } returns dslContext.mockResult(T_PIPELINE_VIEW_GROUP, pvg)
            every {
                self["allPipelineInfos"](
                    any() as String,
                    any() as Boolean
                )
            } returns listOf(pi)
            self.dict("test", "test").let {
                Assertions.assertTrue(it.projectViewList.size == 2)
                Assertions.assertTrue(it.personalViewList.isEmpty())
                Assertions.assertTrue(
                    it.projectViewList.filter { p -> p.viewId == PIPELINE_VIEW_UNCLASSIFIED }.size == 1
                )
            }
        }
    }

    @Nested
    inner class AllPipelineInfos {
        @Test
        @DisplayName("PipelineInfo有数据")
        fun test_1() {
            every {
                pipelineInfoDao.listPipelineInfoByProject(anyDslContext(), any(), any(), any(), any(), any(), any())
            } returns dslContext.mockResult(T_PIPELINE_INFO, pi)
            self.invokePrivate<List<TPipelineInfoRecord>>("allPipelineInfos", "test", false).let {
                Assertions.assertEquals(it!!.size, 1)
                Assertions.assertEquals(it[0].pipelineId, "p-test")
            }
        }

        @Test
        @DisplayName("PipelineInfo无数据")
        fun test_2() {
            every {
                pipelineInfoDao.listPipelineInfoByProject(anyDslContext(), any(), any(), any(), any(), any(), any())
            } returns dslContext.mockResult(T_PIPELINE_INFO)
            self.invokePrivate<List<TPipelineInfoRecord>>("allPipelineInfos", "test", false).let {
                Assertions.assertEquals(it!!.size, 0)
            }
        }
    }

    @Nested
    inner class BulkAdd {
        @BeforeEach
        fun permissionFalse() {
            every { self["hasPermission"]("false", any() as String) } returns false
            every { self["hasPermission"]("true", any() as String) } returns true
        }

        private val ba = PipelineViewBulkAdd(
            pipelineIds = listOf("p-test"),
            viewIds = listOf("test")
        )

        @Test
        @DisplayName("ViewIds 为空")
        fun test_1() {
            every {
                pipelineViewDao.list(anyDslContext(), any() as String, any() as Set<Long>)
            } returns dslContext.mockResult(T_PIPELINE_VIEW)
            self.bulkAdd("true", "test", ba).let {
                Assertions.assertEquals(it, false)
            }
        }

        @Test
        @DisplayName("项目管理员 , 为项目流水线组,  但流水线信息为空")
        fun test_2() {
            val pvCopy = pv.copy()
            pvCopy.viewType = PipelineViewType.STATIC
            pvCopy.isProject = true
            pvCopy.id = 1
            every {
                pipelineViewDao.list(anyDslContext(), any() as String, any() as Set<Long>)
            } returns dslContext.mockResult(T_PIPELINE_VIEW, pvCopy)
            every {
                pipelineInfoDao.listInfoByPipelineIds(anyDslContext(), any(), any())
            } returns dslContext.mockResult(T_PIPELINE_INFO)
            self.bulkAdd("true", "test", ba).let {
                Assertions.assertEquals(it, false)
            }
        }

        @Test
        @DisplayName("项目管理员 , 为项目流水线组,  流水线信息不为空 , 正常运行")
        fun test_3() {
            val pvCopy = pv.copy()
            pvCopy.viewType = PipelineViewType.STATIC
            pvCopy.isProject = true
            pvCopy.id = 1
            every {
                pipelineViewDao.list(anyDslContext(), any() as String, any() as Set<Long>)
            } returns dslContext.mockResult(T_PIPELINE_VIEW, pvCopy)
            every {
                pipelineInfoDao.listInfoByPipelineIds(anyDslContext(), any(), any())
            } returns dslContext.mockResult(T_PIPELINE_INFO, pi)
            every {
                pipelineViewGroupDao.listByViewId(anyDslContext(), any(), any())
            } returns dslContext.mockResult(T_PIPELINE_VIEW_GROUP)
            self.bulkAdd("true", "test", ba).let {
                Assertions.assertEquals(it, true)
            }
        }
    }

    @Nested
    inner class BulkRemove {
        private val br = PipelineViewBulkRemove(
            pipelineIds = listOf("p-test"),
            viewId = "test"
        )

        @BeforeEach
        fun permissionFalse() {
            every { self["hasPermission"]("false", any() as String) } returns false
            every { self["hasPermission"]("true", any() as String) } returns true
        }

        @Test
        @DisplayName("view 为空")
        fun test_1() {
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns null
            self.bulkRemove("test", "test", br).let {
                Assertions.assertEquals(it, false)
            }
        }

        @Test
        @DisplayName("view不为空 ,静态组, 项目管理员 , 不是项目流水线组 , 创建者不是自己")
        fun test_2() {
            val pvCopy = pv.copy()
            pvCopy.viewType = PipelineViewType.STATIC
            pvCopy.isProject = false
            pvCopy.createUser = "other"
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pvCopy
            try {
                self.bulkRemove("true", "test", br)
            } catch (e: Exception) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_VIEW_GROUP_NO_PERMISSION
                )
            }
        }

        @Test
        @DisplayName("view不为空 ,静态组, 不是项目管理员 , 是项目流水线组")
        fun test_3() {
            val pvCopy = pv.copy()
            pvCopy.viewType = PipelineViewType.STATIC
            pvCopy.isProject = true
            pvCopy.createUser = "other"
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pvCopy
            try {
                self.bulkRemove("false", "test", br)
            } catch (e: Exception) {
                Assertions.assertThrows(ErrorCodeException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as ErrorCodeException).errorCode,
                    ProcessMessageCode.ERROR_VIEW_GROUP_NO_PERMISSION
                )
            }
        }

        @Test
        @DisplayName("view不为空 ,静态组, 是项目管理员 , 是项目流水线组")
        fun test_4() {
            val pvCopy = pv.copy()
            pvCopy.viewType = PipelineViewType.STATIC
            pvCopy.isProject = true
            pvCopy.createUser = "other"
            every { pipelineViewDao.get(anyDslContext(), any(), any()) } returns pvCopy
            every { pipelineViewGroupDao.batchRemove(anyDslContext(), any(), any(), any()) } returns Unit
            self.bulkRemove("true", "test", br).let {
                Assertions.assertEquals(it, true)
            }
        }
    }

    @Nested
    inner class HasPermission {
        @BeforeEach
        fun beforeEach() {
            every { clientTokenService.getSystemToken(any()) } returns ""
        }

        @Test
        @DisplayName("返回值测试1")
        fun test_1() {
            every {
                client.mockGet(ServiceProjectAuthResource::class).checkManager(any(), any(), any())
            } returns Result(true)
            self.hasPermission("test", "test").let {
                Assertions.assertEquals(true, it)
            }
        }

        @Test
        @DisplayName("返回值测试2")
        fun test_2() {
            every {
                client.mockGet(ServiceProjectAuthResource::class).checkManager(any(), any(), any())
            } returns Result(false)
            self.hasPermission("test", "test").let {
                Assertions.assertEquals(false, it)
            }
        }
    }

    @Nested
    inner class ListView {
        @Test
        @DisplayName("是项目流水线组")
        fun test_1() {
            every { pipelineViewDao.list(anyDslContext(), any(), any(), any(), any()) } returns emptyList()
            every { pipelineViewGroupDao.countByViewId(anyDslContext(), any(), any()) } returns emptyMap()
            every {
                self["sortViews2Summary"](
                    any() as String,
                    any() as String,
                    any() as List<TPipelineViewRecord>,
                    any() as Map<Long, Int>
                )
            } returns mutableListOf<PipelineNewViewSummary>()
            self.listView("test", "test", false, PipelineViewType.DYNAMIC).let {
                Assertions.assertEquals(it.size, 0)
            }
        }

        @Test
        @DisplayName("不是项目流水线组")
        fun test_2() {
            every { pipelineViewDao.list(anyDslContext(), any(), any(), any(), any()) } returns emptyList()
            every { pipelineViewGroupDao.countByViewId(anyDslContext(), any(), any()) } returns emptyMap()
            every {
                self["sortViews2Summary"](
                    any() as String,
                    any() as String,
                    any() as List<TPipelineViewRecord>,
                    any() as Map<Long, Int>
                )
            } returns mutableListOf<PipelineNewViewSummary>()
            every { self["getClassifiedPipelineIds"](any() as String) } returns emptyList<String>()
            every { pipelineInfoDao.countExcludePipelineIds(anyDslContext(), any(), any(), any()) } returns 0
            self.listView("test", "test", true, PipelineViewType.DYNAMIC).let {
                Assertions.assertEquals(it.size, 1)
                Assertions.assertEquals(it[0].id, PIPELINE_VIEW_UNCLASSIFIED)
            }
        }
    }

    @Nested
    inner class SortViews2Summary {
        @Test
        @DisplayName("正常排序")
        fun test_1() {
            val pvCopy1 = pv.copy()
            pvCopy1.id = 1
            pvCopy1.name = "test1"

            val pvCopy2 = pv.copy()
            pvCopy2.id = 2
            pvCopy2.name = "test2"

            val pvt = TPipelineViewTopRecord()
            pvt.viewId = pvCopy2.id

            every { pipelineViewTopDao.list(anyDslContext(), any(), any()) } returns listOf(pvt)
            self.invokePrivate<MutableList<PipelineNewViewSummary>>(
                "sortViews2Summary", "test", "test", listOf(pvCopy1, pvCopy2), emptyMap<Long, Int>()
            ).let {
                Assertions.assertEquals(it!!.size, 2)
                Assertions.assertEquals(it[0].name, pvCopy2.name)
            }
        }
    }

    @Nested
    inner class PipelineCount {
        @Test
        @DisplayName("viewGroup为空")
        fun test_1() {
            every { pipelineViewGroupDao.listByViewId(anyDslContext(), any(), any()) } returns emptyList()
            self.pipelineCount("test", "test", "test").let {
                Assertions.assertEquals(it, PipelineViewPipelineCount.DEFAULT)
            }
        }

        @Test
        @DisplayName("viewGroup不为空")
        fun test_2() {
            every { pipelineViewGroupDao.listByViewId(anyDslContext(), any(), any()) } returns listOf(pvg)
            every {
                pipelineInfoDao.listInfoByPipelineIds(
                    anyDslContext(),
                    any(),
                    any(),
                    any()
                )
            } returns dslContext.mockResult(
                T_PIPELINE_INFO, pi
            )
            self.pipelineCount("test", "test", "test").let {
                Assertions.assertEquals(it.deleteCount, 0)
                Assertions.assertEquals(it.normalCount, 1)
            }
        }
    }
}
