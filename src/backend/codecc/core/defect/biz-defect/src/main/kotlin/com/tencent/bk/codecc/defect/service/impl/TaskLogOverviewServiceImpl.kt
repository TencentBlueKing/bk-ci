package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogOverviewRepository
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogDao
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogOverviewDao
import com.tencent.bk.codecc.defect.dto.WebsocketDTO
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity
import com.tencent.bk.codecc.defect.service.IQueryStatisticBizService
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO
import com.tencent.bk.codecc.defect.vo.TaskLogVO
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.bk.codecc.task.vo.TaskOverviewVO
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.service.BaseDataCacheService
import com.tencent.devops.common.service.BizServiceFactory
import com.tencent.devops.common.service.utils.PageableUtils
import com.tencent.devops.common.web.mq.EXCHANGE_TASKLOG_DEFECT_WEBSOCKET
import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.collections.CollectionUtils
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TaskLogOverviewServiceImpl @Autowired constructor(
        private val taskLogDao: TaskLogDao,
        private val taskLogRepository: TaskLogRepository,
        private val taskLogOverviewDao: TaskLogOverviewDao,
        private val taskLogOverviewRepository: TaskLogOverviewRepository,
        private val redisTemplate: RedisTemplate<String, String>,
        private val baseDataCacheService: BaseDataCacheService,
        private val buildRepository: BuildRepository,
        private val taskLogAndDefectFactory: BizServiceFactory<IQueryStatisticBizService>,
        private val rabbitTemplate: RabbitTemplate,
        private val client: Client
) : TaskLogOverviewService {

    /**
     * 保存本次构建实际应该执行的有哪些工具
     * 通过插件在过滤完开源扫描的编译型工具后上报到后台保存
     *
     * @param taskLogOverviewVO
     */
    override fun saveActualExeTools(taskLogOverviewVO: TaskLogOverviewVO): Boolean {
        logger.info("save task log overview actual tools: ${taskLogOverviewVO.taskId} | ${taskLogOverviewVO.buildId} | ${taskLogOverviewVO.tools.size}")
        var taskLogOverviewEntity: TaskLogOverviewEntity? =
                taskLogOverviewRepository.findFirstByTaskIdAndBuildId(taskLogOverviewVO.taskId, taskLogOverviewVO.buildId)
        if (taskLogOverviewEntity == null) {
            taskLogOverviewEntity = TaskLogOverviewEntity(
                    ObjectId.get().toString(),
                    taskLogOverviewVO.taskId,
                    taskLogOverviewVO.buildId,
                    ComConstants.ScanStatus.PROCESSING.code,
                    System.currentTimeMillis(),
                    mutableListOf()
            )
        }
        val toolSet = taskLogOverviewVO.tools.toSet()
        taskLogOverviewEntity.toolList = toolSet.toList()
        taskLogOverviewRepository.save(taskLogOverviewEntity)
        return true
    }

    /**
     * 获取当前扫描实际需要执行的工具
     * 用户开源扫描过滤编译型工具时的度量分数计算
     *
     * @param taskId
     * @param buildId
     */
    override fun getActualExeTools(taskId: Long, buildId: String): List<String>? {
        return taskLogOverviewRepository.findFirstByTaskIdAndBuildId(taskId, buildId)?.toolList
    }

    /**
     * 计算任务状态，根据taskLog中的工具状态进行判断
     * 每次执行都更新 taskLog list
     * 任务最后成功后 task log list中的工具与 toolList 对齐
     *
     * @param uploadTaskLogStepVO 上报请求体
     */
    override fun calTaskStatus(uploadTaskLogStepVO: UploadTaskLogStepVO) {
        val taskId = uploadTaskLogStepVO.taskId
        val toolName = uploadTaskLogStepVO.toolName
        val buildId = uploadTaskLogStepVO.pipelineBuildId
        val taskLogEntity = taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(
                taskId,
                toolName,
                buildId
        )

        logger.info("cal status, begin to get lock: $taskId $toolName $buildId ${uploadTaskLogStepVO.triggerFrom} ${uploadTaskLogStepVO.isFinish}")
        val redisLock = RedisLock(
                redisTemplate,
                "${RedisKeyConstants.TOOL_FINISH_CONFIRM_LOCK}:$taskId:$buildId",
                10
        )

        try {
            // 取锁，保证 taskLogOverview 线程安全
            while (!redisLock.tryLock()) {
                Thread.sleep(500)
            }
            logger.info("cal status, get lock success: $taskId $toolName $buildId")
            // 不存在说明是第一个上报的工具触发的，记录还不存在，新建一条记录
            var taskLogOverviewEntity: TaskLogOverviewEntity? =
                    taskLogOverviewRepository.findFirstByTaskIdAndBuildId(taskId, buildId)

            if (taskLogOverviewEntity == null) {
                logger.info("task log overview entity is null")
                taskLogOverviewEntity = TaskLogOverviewEntity(
                        ObjectId.get().toString(),
                        taskId,
                        buildId,
                        ComConstants.ScanStatus.PROCESSING.code,
                        System.currentTimeMillis(),
                        mutableListOf()
                )
                val result = client.get(ServiceTaskRestResource::class.java).getTaskToolList(taskId)
                if (result.isOk() && result.data != null) {
                    logger.error("cal status: succ to get tool config: $taskId $toolName $buildId")
                    val toolSet = result.data!!.enableToolList.map { it.toolName }.toSet()
                    taskLogOverviewEntity.toolList = toolSet.toList()
                }
            }

            var taskStatus = taskLogOverviewEntity.status
            logger.info("cal status, curr status is $taskStatus")
            // 短路实现：当前 taskLog 是完成并且成功状态才去确认工具状态
            if (uploadTaskLogStepVO.isFinish && toolConfirm(taskId, buildId, toolName)) {
                logger.info("cal status, task finished: $taskId $toolName $buildId")
                // 任务状态位置为成功
                taskStatus = ComConstants.ScanStatus.SUCCESS.code
            }

            // 工具失败或中断，将任务状态设为失败，这里注意线程安全
            if (taskLogEntity.flag == ComConstants.StepFlag.FAIL.value()
                    || taskLogEntity.flag == ComConstants.StepFlag.ABORT.value()) {
                logger.info("cal task status: $taskId $toolName $buildId is fail")
                taskStatus = ComConstants.ScanStatus.FAIL.code
            }

            taskLogOverviewEntity.status = taskStatus
            taskLogOverviewEntity.taskLogEntityList = if (taskLogOverviewEntity.taskLogEntityList == null) {
                logger.info("task log entity list is null")
                mutableListOf()
            } else {
                taskLogOverviewEntity.taskLogEntityList
            }
            taskLogOverviewEntity.taskLogEntityList.add(taskLogEntity)
            taskLogOverviewEntity.taskLogEntityList = taskLogOverviewEntity.taskLogEntityList.distinctBy(TaskLogEntity::getToolName)
            logger.info("cal status, finally status is $taskStatus: $taskId $toolName $buildId ${System.currentTimeMillis()}")

            logger.info("cal status, set start and end time ${uploadTaskLogStepVO.startTime}: ${taskLogOverviewEntity.startTime}" +
                    " ${uploadTaskLogStepVO.endTime} ${taskLogOverviewEntity.endTime}")

            // 设置分析 开始/结束 时间
            setTime(uploadTaskLogStepVO, taskLogOverviewEntity)
            taskLogOverviewEntity.buildNum = taskLogOverviewEntity.taskLogEntityList
                    .firstOrNull()?.buildNum ?: ""

            logger.info("cal status, save status time is ${System.currentTimeMillis()}")
            taskLogOverviewRepository.save(taskLogOverviewEntity)

            // 任务整体状态为终态时通过 ws 通知页面转换状态并刷新redis缓存
            if (taskStatus != ComConstants.ScanStatus.PROCESSING.code) {
                val taskDetailVO = TaskDetailVO()
                val taskLogVO = TaskLogVO()

                sendWebSocketMsg(taskDetailVO, taskLogVO, taskLogOverviewEntity)
                redisTemplate.delete("${RedisKeyConstants.TOOL_FINISH_CONFIRM}:$taskId:$buildId")
            }
        } catch (e: Throwable) {
            logger.error("cal task status fail: $taskId $toolName $buildId")
            e.stackTrace.forEach { logger.error("$it") }
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 获取任务维度分析记录，只返回构建号，用于概览页面查找本次任务最后一次成功的构建
     *
     * @param taskId
     * @param buildId
     * @param status
     */
    override fun getTaskLogOverview(taskId: Long, buildId: String?, status: Int?): TaskLogOverviewVO? {
        logger.info("get task log overview: $taskId $buildId $status")
        val taskLogOverviewEntity: TaskLogOverviewEntity? = if (buildId.isNullOrBlank()) {
            if (status == null) {
                taskLogOverviewRepository.findFirstByTaskIdOrderByStartTimeDesc(taskId)
            } else {
                // 总览页面
                taskLogOverviewRepository.findFirstByTaskIdAndStatusOrderByStartTimeDesc(taskId, status)
            }
        } else {
            if (status == null) {
                taskLogOverviewRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
            } else {
                taskLogOverviewRepository.findFirstByTaskIdAndBuildIdAndStatus(taskId, buildId, status)
            }
        } ?: return null

        return entity2VO(taskLogOverviewEntity)
    }

    /**
     * 批量获取当前任务构建记录
     * 用于任务分析就页面，需要带上工具分析记录一起展示
     *
     * @param taskId
     * @param page
     * @param pageSize
     */
    override fun getTaskLogOverviewList(taskId: Long, page: Int?, pageSize: Int?): PageImpl<TaskLogOverviewVO> {
        logger.info("get task log overview list: $taskId $page $pageSize")
        val pageAble = PageableUtils.getPageable(page ?: 0, pageSize
                ?: 50, "start_time", Sort.Direction.DESC, "start_time")
        var taskLogOverviewEntityList =
                taskLogOverviewDao.getTaskLogOverviewList(taskId, pageAble)

        if (taskLogOverviewEntityList.isNullOrEmpty()) {
            taskLogOverviewEntityList = convertTaskLog2TaskLogOverview(taskId)
            logger.info("${taskLogOverviewEntityList.map { it.buildNum }}")
            if (taskLogOverviewEntityList.isNotEmpty()) {
                taskLogOverviewRepository.saveAll(taskLogOverviewEntityList)
            }
        }

        val taskLogOverviewVOList = mutableListOf<TaskLogOverviewVO>()
        // 拿到工具展示顺序
        val orderToolIds = baseDataCacheService.toolOrder
        val toolOrderList = orderToolIds.paramValue.split(",")

        taskLogOverviewEntityList.forEach {
            val taskLogOverviewVO = TaskLogOverviewVO()
            BeanUtils.copyProperties(taskLogOverviewVO, it)
            val taskLogVOList = ArrayList<TaskLogVO>(it.taskLogEntityList.size)
            // 根据工具展示顺序排序 TaskLog 信息
            it.taskLogEntityList.sortBy { taskLogEntity -> toolOrderList.indexOf(taskLogEntity.toolName) }
            it.taskLogEntityList.forEach { taskLogEntity ->
                val taskLogVO = TaskLogVO()
                BeanUtils.copyProperties(taskLogVO, taskLogEntity)
                // 工具状态转换，如果工具状态是已完成并成功的话，step值 +1 以适配前端
                if (taskLogEntity.flag == ComConstants.StepFlag.SUCC.value()) {
                    taskLogVO.currStep = taskLogVO.currStep + 1
                }
                taskLogVOList.add(taskLogVO)
            }
            taskLogOverviewVO.taskLogVOList = taskLogVOList
            taskLogOverviewVO.repoInfoStrList = pickUpRepoInfo(it.taskLogEntityList)
            with(taskLogOverviewVO.repoInfoStrList.firstOrNull() ?: "") {
                taskLogOverviewVO.version = try {
                    substring(this.indexOf("版本号：") + 4, this.indexOf("，提交时间"))
                } catch (e: Exception) {
                    logger.error("getTaskLogOverviewList: fail to substring msg $this")
                    ""
                }
            }
            taskLogOverviewVO.elapseTime = if (taskLogOverviewVO.endTime == null || taskLogOverviewVO.endTime == 0L) {
                0L
            } else {
                taskLogOverviewVO.endTime - (taskLogOverviewVO.startTime?:0)
            }
            taskLogOverviewVO.buildUser =
                    buildRepository.findFirstByBuildId(taskLogOverviewVO.buildId)?.buildUser?:"获取失败"
            taskLogOverviewVOList.add(taskLogOverviewVO)
        }
        val totalCount = taskLogOverviewRepository.countByTaskId(taskId)
        return PageImpl(taskLogOverviewVOList, pageAble, totalCount)
    }

    /**
     * 获取分析记录以及对应的分析结果
     * 根据 QueryData 的积类型表示查询条件，预留四种查询场景，有需要可以在 companion 中添加
     *
     * @param taskId
     * @param buildId
     * @param buildNum
     * @param status
     */
    override fun getAnalyzeResult(taskId: Long, buildId: String?, buildNum: String?, status: Int?): TaskLogOverviewVO? {
        logger.info("get analyze result: $taskId $buildId $buildNum $status")
        val queryData =
                QueryData(false, buildId == null, buildNum == null, status == null)

        val taskLogOverviewVO = TaskLogOverviewVO()
        val taskLogOverviewEntity: TaskLogOverviewEntity?
        // 根据条件查询，具体的 queryData 预留值在 companion 中
        when (queryData) {
            queryLatest -> {
                logger.info("query latest: $taskId")
                taskLogOverviewEntity =
                        taskLogOverviewRepository.findFirstByTaskIdOrderByStartTimeDesc(taskId)
                                ?: return null
                setAnalyzeList(taskLogOverviewVO, taskLogOverviewEntity)
                BeanUtils.copyProperties(taskLogOverviewVO, taskLogOverviewEntity)
            }
            queryByBuildId -> {
                logger.info("query by buildId: $taskId $buildId")
                taskLogOverviewEntity =
                        taskLogOverviewRepository.findFirstByTaskIdAndBuildId(taskId, buildId)
                                ?: return null
                setAnalyzeList(taskLogOverviewVO, taskLogOverviewEntity)
                BeanUtils.copyProperties(taskLogOverviewVO, taskLogOverviewEntity)
            }
            queryByBuildNum -> {
                logger.info("query by buildNum: $taskId $buildNum")
                taskLogOverviewEntity =
                        taskLogOverviewRepository.findFirstByTaskIdAndBuildNum(taskId, buildNum)
                                ?: return null
                setAnalyzeList(taskLogOverviewVO, taskLogOverviewEntity)
                BeanUtils.copyProperties(taskLogOverviewVO, taskLogOverviewEntity)
            }
            queryLatestByStatus -> {
                logger.info("query by status: $taskId $status")
                taskLogOverviewEntity =
                        taskLogOverviewRepository.findFirstByTaskIdAndStatusOrderByStartTimeDesc(taskId, status!!)
                                ?: return null
                setAnalyzeList(taskLogOverviewVO, taskLogOverviewEntity)
                BeanUtils.copyProperties(taskLogOverviewVO, taskLogOverviewEntity)
            }
            else -> {
                logger.info("query other: $taskId")
                return null
            }
        }

        return taskLogOverviewVO
    }

    /**
     * 工具确认执行状态
     * 当一个工具在上报完 工具状态 完后触发 AOP 进行 任务状态 计算
     * 通过 redis 的方式实现一个缓存记录实际执行的工具数量，每触发一次 AOP 并且工具状态是已完成时缓存数据 -1
     * 当缓存值为 0 时代表当前触发 AOP 的是最后一个工具线程，并且所有工具执行状态都是成功，此时设置任务状态为成功
     * 注意：这里需要注意线程安全，在方法调用处已经使用了 redis 锁，所以方法内无需再加锁
     *
     * @param taskId
     * @param buildId
     * @param toolName
     */
    private fun toolConfirm(taskId: Long, buildId: String, toolName: String): Boolean {
        var confirmNum = redisTemplate.opsForValue()
                .get("${RedisKeyConstants.TOOL_FINISH_CONFIRM}:$taskId:$buildId")
                ?.toInt()
        logger.info("$toolName $taskId $buildId confirm num is $confirmNum")
        // 当缓存值为空，初始化缓存值为实际执行工具数
        if (confirmNum == null) {
            val actualExeToolsSize = getActualExeTools(taskId, buildId)?.size
                    ?: throw Exception("invalid actual execute tool num")
            confirmNum = actualExeToolsSize - 1
            // 如果只有一个工具，直接返回已完成状态，不写入 Redis
            if (actualExeToolsSize > 1) {
                redisTemplate.opsForValue()
                        .set("${RedisKeyConstants.TOOL_FINISH_CONFIRM}:$taskId:$buildId", confirmNum.toString(), 24, TimeUnit.HOURS)
            }

            return confirmNum == 0
        }

        confirmNum = redisTemplate.opsForValue()
                .increment("${RedisKeyConstants.TOOL_FINISH_CONFIRM}:$taskId:$buildId", -1)
                ?.toInt()

        if (confirmNum == 0) {
            redisTemplate.delete("${RedisKeyConstants.TOOL_FINISH_CONFIRM}:$taskId:$buildId")
        }

        logger.info("$toolName $taskId $buildId after confirm num is $confirmNum")
        return confirmNum == 0
    }

    /**
     * 从工具分析记录抽取代码库信息，不解析字符串
     * 直接返回字符串列表
     *
     * @param taskLogEntityList
     */
    private fun pickUpRepoInfo(taskLogEntityList: MutableList<TaskLogEntity>?): List<String> {
        val repoInfoStr = mutableSetOf<String>()
        taskLogEntityList?.forEach {
            val steps = it.stepArray
            val strList = steps.filter { step ->
                !step.msg.isNullOrBlank() && step.msg.contains("代码库：")
            }.map { step ->
                step.msg
            }.toList()

            repoInfoStr.addAll(strList)
        }

        return repoInfoStr.toList()
    }

    /**
     * 注入扫描分析结果，获取扫描告警数据
     *
     * @param taskLogOverviewVO
     * @param taskLogOverviewEntity
     */
    private fun setAnalyzeList(taskLogOverviewVO: TaskLogOverviewVO, taskLogOverviewEntity: TaskLogOverviewEntity) {
        val taskLogGroupEntities = taskLogOverviewEntity.taskLogEntityList
        val toolLastAnalysisResultVOList = mutableListOf<ToolLastAnalysisResultVO>()
        if (CollectionUtils.isNotEmpty(taskLogGroupEntities)) {
            for (taskLogGroupEntity in taskLogGroupEntities) {
                val toolLastAnalysisResultVO = ToolLastAnalysisResultVO()
                BeanUtils.copyProperties(taskLogGroupEntity, toolLastAnalysisResultVO)
                val queryStatisticBizService: IQueryStatisticBizService = taskLogAndDefectFactory
                        .createBizService(toolLastAnalysisResultVO.toolName,
                                ComConstants.BusinessType.QUERY_STATISTIC.value(), IQueryStatisticBizService::class.java)
                val lastAnalysisResultVO: BaseLastAnalysisResultVO = queryStatisticBizService.processBiz(toolLastAnalysisResultVO, true)
                toolLastAnalysisResultVO.lastAnalysisResultVO = lastAnalysisResultVO
                toolLastAnalysisResultVOList.add(toolLastAnalysisResultVO)
            }
        }
        taskLogOverviewVO.lastAnalysisResultVOList = toolLastAnalysisResultVOList
    }

    /**
     * 发送任务结束的 ws 推送信息，刷新分析记录页面
     * 在 analyzeTask 中每上报一次记录会推送 ws 刷新一次分析记录页面，但是任务最后执行完后 analyzeTask 是无感知的，
     * 需要在这里手动推送一次 ws 保证分析记录页面的状态
     *
     * @param taskDetailVO
     * @param taskLogVO
     * @param taskLogOverviewEntity
     */
    private fun sendWebSocketMsg(taskDetailVO: TaskDetailVO, taskLogVO: TaskLogVO, taskLogOverviewEntity: TaskLogOverviewEntity?) {
        logger.info("send finish status by websocket")
        val taskLogOverviewVO = entity2VO(taskLogOverviewEntity)
        val websocketDTO = WebsocketDTO(taskLogVO, TaskOverviewVO.LastAnalysis(), taskDetailVO, taskLogOverviewVO)
        rabbitTemplate.convertAndSend(EXCHANGE_TASKLOG_DEFECT_WEBSOCKET, "",
                websocketDTO)
    }

    /**
     * 分析记录工具需要按照工具展示顺序排序，顺序与概览页面对齐
     *
     * @param taskLogOverviewEntity
     */
    private fun entity2VO(taskLogOverviewEntity: TaskLogOverviewEntity?): TaskLogOverviewVO {
        val taskLogVOList = mutableListOf<TaskLogVO>()
        taskLogOverviewEntity?.taskLogEntityList?.forEach { taskLogEntity ->
            val taskLogVO = TaskLogVO()
            BeanUtils.copyProperties(taskLogVO, taskLogEntity)
            // 工具状态转换，如果工具状态是已完成并成功的话，step值 +1 以适配前端
            if (taskLogEntity.flag == ComConstants.StepFlag.SUCC.value()) {
                taskLogVO.currStep = taskLogVO.currStep + 1
            }
            taskLogVOList.add(taskLogVO)
        }

        // 拿到工具展示顺序信息排序
        val orderToolIds = baseDataCacheService.toolOrder
        val toolOrderList = orderToolIds.paramValue.split(",")
        taskLogVOList.sortBy { taskLogEntity -> toolOrderList.indexOf(taskLogEntity.toolName) }

        val taskLogOverviewVO = TaskLogOverviewVO()
        BeanUtils.copyProperties(taskLogOverviewVO, taskLogOverviewEntity)
        taskLogOverviewVO.taskLogVOList = taskLogVOList
        taskLogOverviewVO.repoInfoStrList = pickUpRepoInfo(taskLogOverviewEntity?.taskLogEntityList)
        return taskLogOverviewVO
    }

    /**
     * TaskLog 转换 TaskLogOverview 逻辑，用于版本升级后的过度逻辑
     *
     * @param taskId
     */
    private fun convertTaskLog2TaskLogOverview(taskId: Long): List<TaskLogOverviewEntity> {
        val taskLogEntityList = taskLogDao.findLatestBuild(taskId, 10)
        val taskLogOverviewEntityList = mutableListOf<TaskLogOverviewEntity>()
        taskLogEntityList.groupBy { it.buildId }
                .forEach { (buildId, taskLogGroup) ->
                    val taskLogOverviewEntity = TaskLogOverviewEntity()
                    taskLogOverviewEntity.taskId = taskId
                    taskLogOverviewEntity.buildId = buildId
                    taskLogOverviewEntity.buildNum = taskLogGroup.firstOrNull()?.buildNum
                    taskLogOverviewEntity.startTime = taskLogGroup.map { it.startTime }.minOrNull ()
                    taskLogOverviewEntity.endTime = taskLogGroup.map { it.endTime }.maxOrNull ()
                    taskLogOverviewEntity.taskLogEntityList = taskLogGroup
                    taskLogOverviewEntity.status = calTaskStatus(taskLogGroup)
                    taskLogOverviewEntityList.add(taskLogOverviewEntity)
                }

        return taskLogOverviewEntityList.sortedBy { Integer.parseInt(it.buildNum) }.reversed().toList()
    }

    private fun calTaskStatus(taskLogGroup: List<TaskLogEntity>): Int {
        if (taskLogGroup.isEmpty()) {
            return ComConstants.ScanStatus.PROCESSING.code
        }

        var status = ComConstants.ScanStatus.SUCCESS.code
        taskLogGroup.forEach {
            when(it.flag) {
                ComConstants.StepFlag.ABORT.value() -> {
                    return ComConstants.ScanStatus.FAIL.code
                }
                ComConstants.StepFlag.FAIL.value() -> {
                    return ComConstants.ScanStatus.FAIL.code
                }
                ComConstants.StepFlag.PROCESSING.value() -> {
                    status = ComConstants.ScanStatus.PROCESSING.code
                }
            }
        }

        return status
    }

    private fun setTime(uploadTaskLogStepVO: UploadTaskLogStepVO, taskLogOverviewEntity: TaskLogOverviewEntity) {
        if (uploadTaskLogStepVO.startTime == 0L) {
            uploadTaskLogStepVO.startTime = Long.MAX_VALUE
        }
        if (uploadTaskLogStepVO.endTime == 0L) {
            uploadTaskLogStepVO.endTime = Long.MIN_VALUE
        }
        if (uploadTaskLogStepVO.startTime < (taskLogOverviewEntity.startTime ?: Long.MAX_VALUE)) {
            taskLogOverviewEntity.startTime = uploadTaskLogStepVO.startTime
        }
        if (uploadTaskLogStepVO.endTime > taskLogOverviewEntity.endTime ?: Long.MIN_VALUE) {
            taskLogOverviewEntity.endTime = uploadTaskLogStepVO.endTime
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskLogOverviewServiceImpl::class.java)
        // 查询最后一次分析记录
        private val queryLatest = QueryData(
                isTaskIdNull = false,
                isBuildIdNull = true,
                isBuildNumNull = true,
                isStatusNull = true
        )
        // 查询指定扫描状态的最后一次分析记录
        private val queryLatestByStatus = QueryData(
                isTaskIdNull = false,
                isBuildIdNull = true,
                isBuildNumNull = true,
                isStatusNull = false
        )
        // 查询指定构建号分析记录
        private val queryByBuildId = QueryData(
                isTaskIdNull = false,
                isBuildIdNull = false,
                isBuildNumNull = true,
                isStatusNull = true
        )
        // 查询指定构建号分析记录
        private val queryByBuildNum = QueryData(
                isTaskIdNull = false,
                isBuildIdNull = true,
                isBuildNumNull = false,
                isStatusNull = true
        )
    }

    /**
     * 分析记录查询体，利用积类型表示不同的查询条件:
     * queryLatest
     * queryLatestByStatus
     * queryByBuildId
     * queryByBuildNum
     * 其他查询条件可根据需要添加
     */
    data class QueryData(
            val isTaskIdNull: Boolean,
            val isBuildIdNull: Boolean,
            val isBuildNumNull: Boolean,
            val isStatusNull: Boolean
    )

    /**
     * 统计任务分析次数
     */
    override fun statTaskAnalyzeCount(taskIds: Collection<Long>, status: Int?, startTime: Long?, endTime: Long?): Int {
        return Math.toIntExact(taskLogOverviewDao.queryTaskAnalyzeCount(taskIds, status, startTime, endTime))
    }

}
