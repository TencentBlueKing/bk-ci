package com.tencent.bk.codecc.apiquery.resources

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bk.codecc.apiquery.api.ApigwDefectResource
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel
import com.tencent.bk.codecc.apiquery.defect.model.StatisticModel
import com.tencent.bk.codecc.apiquery.pojo.DefectQueryParam
import com.tencent.bk.codecc.apiquery.service.ICheckerService
import com.tencent.bk.codecc.apiquery.service.IDefectQueryWarningService
import com.tencent.bk.codecc.apiquery.service.openapi.ApiBizService
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO
import com.tencent.bk.codecc.apiquery.vo.openapi.CheckerDefectStatVO
import com.tencent.bk.codecc.apiquery.vo.openapi.TaskOverviewDetailRspVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RestResource
import org.apache.commons.collections.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDefectResourceImpl @Autowired constructor(
    private val taskDao: TaskDao,
    private val apiBizService: ApiBizService,
    private val checkerSerivce: ICheckerService
) : ApigwDefectResource{

    private val toolPatterCache: LoadingCache<String, String> =
        CacheBuilder.newBuilder().maximumSize(100).build(object : CacheLoader<String, String>() {
            override fun load(toolName: String): String {
                return loadToolPattern(toolName)
            }
        })

    override fun getDefectDetailList(
        taskQueryReq: TaskQueryReq,
        appCode : String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): CodeCCResult<Page<CommonModel>> {
        if(taskQueryReq.taskIdList.isNullOrEmpty()){
            logger.info("task id list should not be null!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID)
        }
        val pattern = toolPatterCache.get(taskQueryReq.toolName!!)
        val iDefectQueryWarningService = try{
            SpringContextUtil.getBean(
                IDefectQueryWarningService::class.java,
                "${pattern}QueryWarningBizService"
            )
        } catch (e : Exception){
            logger.warn("default service bean, bean name: CommonQueryWarningBizService")
            SpringContextUtil.getBean(
                IDefectQueryWarningService::class.java,
                "CommonQueryWarningBizService"
            )
        }
        val defectQueryParam = DefectQueryParam(
            taskIdList = taskQueryReq.taskIdList!!,
            toolName = taskQueryReq.toolName,
            status = taskQueryReq.status,
            checker = taskQueryReq.checker,
            notChecker = taskQueryReq.notChecker,
            filterFields = taskQueryReq.filterFields
        )
        logger.info("get defect service bean successfully! bean name: ${pattern}QueryWarningBizService")
        val defectDetailPage = iDefectQueryWarningService.queryLintDefectDetail(
            defectQueryParam,
            pageNum,
            pageSize,
            sortField,
            sortType
        )
        return CodeCCResult(defectDetailPage)
    }


    override fun getDefectStatisticList(
        taskQueryReq: TaskQueryReq,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): CodeCCResult<Page<StatisticModel>> {
        if(taskQueryReq.taskIdList.isNullOrEmpty()){
            logger.info("task id list should not be null!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID)
        }
        val pattern = toolPatterCache.get(taskQueryReq.toolName!!)
        val iDefectQueryWarningService = try{
            SpringContextUtil.getBean(
                IDefectQueryWarningService::class.java,
                "${pattern}QueryWarningBizService"
            )
        } catch (e : Exception){
            logger.warn("default service bean, bean name: CommonQueryWarningBizService")
            SpringContextUtil.getBean(
                IDefectQueryWarningService::class.java,
                "CommonQueryWarningBizService"
            )
        }
        logger.info("get defect statistic bean successfully! bean name: ${pattern}QueryWarningBizService")
        val defectStatisticList = iDefectQueryWarningService.queryLintDefectStatistic(
            taskQueryReq.taskIdList!!,
            taskQueryReq.toolName,
            taskQueryReq.startTime,
            taskQueryReq.endTime,
            taskQueryReq.filterFields,
            taskQueryReq.buildId,
            pageNum,
            pageSize,
            sortField,
            sortType
        )
        return CodeCCResult(defectStatisticList)
    }


    override fun queryTaskOverview(
            taskToolInfoReqVO: TaskToolInfoReqVO,
            pageNum: Int?,
            pageSize: Int?,
            sortType: String?
    ): CodeCCResult<TaskOverviewDetailRspVO> {
        // TODO 临时限定接口只允许查询pcg的任务
        if (taskToolInfoReqVO.bgId != 29292 && CollectionUtils.isEmpty(taskToolInfoReqVO.deptIds)) {
            logger.error("queryTaskOverview req can not query: {}", taskToolInfoReqVO)
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("bgId"))
        }
        return CodeCCResult(apiBizService.statisticsTaskOverview(taskToolInfoReqVO, pageNum, pageSize, sortType))
    }

    override fun getDefectStatByChecker(
            taskToolInfoReqVO: TaskToolInfoReqVO,
            appCode: String,
            pageNum: Int?,
            pageSize: Int?
    ): CodeCCResult<Page<CheckerDefectStatVO>> {
        return CodeCCResult(apiBizService.statCheckerDefect(taskToolInfoReqVO, pageNum, pageSize))
    }


    override fun queryChecker(
        checkerSetType: String
    ): CodeCCResult<List<CheckerDetailModel>> {
        if (!checkerSetType.equals("FULL", ignoreCase = true) && !checkerSetType.equals("SIMPLIFIED", ignoreCase = true)) {
            logger.info("checkerSetType is invalid")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("checkerSetType"))
        }
        return CodeCCResult(checkerSerivce.queryCheckerDetail(checkerSetType))
    }

    private fun loadToolPattern(toolName: String): String {
        return taskDao.findToolMetaByName(toolName)?.pattern ?: ""
    }

    companion object {
        const val taskServiceSubfix = "TaskService"

        private val logger = LoggerFactory.getLogger(ApigwDefectResourceImpl::class.java)
    }
}