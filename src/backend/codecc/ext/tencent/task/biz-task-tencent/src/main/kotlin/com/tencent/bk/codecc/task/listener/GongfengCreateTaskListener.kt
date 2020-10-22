package com.tencent.bk.codecc.task.listener

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.task.component.GongfengProjectChecker
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongodbrepository.GongfengFailPageRepository
import com.tencent.bk.codecc.task.dao.mongodbrepository.GongfengStatProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongodbtemplate.GongfengStatProjDao
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao
import com.tencent.bk.codecc.task.model.GongfengActiveProjEntity
import com.tencent.bk.codecc.task.model.GongfengFailPageEntity
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.GongfengStatProjEntity
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.ActiveProjParseModel
import com.tencent.devops.common.auth.pojo.GongfengBranchModel
import com.tencent.bk.codecc.task.pojo.GongfengProjPageModel
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel
import com.tencent.bk.codecc.task.pojo.GongfengStatPageModel
import com.tencent.bk.codecc.task.pojo.OwnerInfo
import com.tencent.bk.codecc.task.pojo.ProjectCommitInfo
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.OpenSourcePipelineService
import com.tencent.bk.codecc.task.service.TaskRegisterService
import com.tencent.bk.codecc.task.service.TaskService
import com.tencent.bk.codecc.task.service.impl.OpenSourceTaskRegisterServiceImpl
import com.tencent.bk.codecc.task.tof.TofClientApi
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.codecc.CodeCCDispatchType
import com.tencent.devops.common.util.OkhttpUtils
import com.tencent.devops.common.web.mq.EXCHANGE_EXTERNAL_JOB
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Component
class GongfengCreateTaskListener @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val pipelineService: OpenSourcePipelineService,
    @Qualifier("pipelineTaskRegisterService") private val taskRegisterService: TaskRegisterService,
    private val rabbitTemplate: RabbitTemplate,
    private val taskService: TaskService,
    private val gongfengFailPageRepository: GongfengFailPageRepository,
    private val taskRepository: TaskRepository,
    private val tofClientApi: TofClientApi,
    private val client: Client,
    private val gongfengStatProjDao: GongfengStatProjDao,
    private val gongfengStatProjRepository: GongfengStatProjRepository,
    private val baseDataRepository: BaseDataRepository,
    private val gongfengProjectChecker: GongfengProjectChecker,
    private val openSourceTaskRegisterService: OpenSourceTaskRegisterServiceImpl,
    private val taskDao: TaskDao
) {

    @Value("\${git.path:#{null}}")
    private val gitCodePath: String? = null

    @Value("\${codecc.privatetoken:#{null}}")
    private val gitPrivateToken: String? = null

    @Value("\${codecc.classurl:#{null}}")
    private val publicClassUrl: String? = null

    @Value("\${devopsGateway.idchost:#{null}}")
    private val devopsUrl: String? = null

    @Value("\${codecc.public.account:#{null}}")
    private val codeccPublicAccount: String? = null

    @Value("\${codecc.public.password:#{null}}")
    private val codeccPublicPassword: String? = null

    @Value("\${devops.imageName:#{null}}")
    private val imageName: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengCreateTaskListener::class.java)

        //用于线程阻塞时记录信息
        private val pageThreadLocal = ThreadLocal<GongfengFailPageEntity>()
    }

    /**
     * 用于缓存开源扫描时间周期
     */
    private val periodInfoCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, Int>(
            object : CacheLoader<String, Int>() {
                override fun load(paramType: String): Int {
                    return getOpenSourceCheckPeriod(paramType)
                }
            }
        )

    //新建线程池，当线程池满了时，阻塞主线程
    private val taskExecutor = ThreadPoolExecutor(0, 20, 60L, TimeUnit.MILLISECONDS,
        SynchronousQueue<Runnable>(), RejectedExecutionHandler { r, executor ->
            try {
                //生产者阻塞30秒，无反应就退出
                logger.info("blocking queue of thread pool is full, waiting to release space")
                val offerResult = executor.queue.offer(r, 30, TimeUnit.SECONDS)
                if (!offerResult) {
                    logger.error("current thread is blocked, ready to quit!")
                    val failPageEntity = pageThreadLocal.get()
                    if (null != failPageEntity) {
                        gongfengFailPageRepository.save(failPageEntity)
                    }
                }
            } catch (e: InterruptedException) {
                logger.error("execute new task fail!")
                val failPageEntity = pageThreadLocal.get()
                gongfengFailPageRepository.save(failPageEntity)
            }
        })

    fun executeCreateTask(gongfengPageModel: GongfengProjPageModel) {
        val pageNum = gongfengPageModel.pageNum
        val gongfengModelList = gongfengPageModel.publicProjList
        val projIdList = gongfengModelList.map { it.id }
        try {
            logger.info("start to handle with new project")
            val allPublicProjects = gongfengPublicProjService.findAllProjects()
            //拉取工蜂项目信息，新建只带CLOC工具的流水线，扫描CLOC工具信息并上报
            //拉出工蜂信息，放进线程池阻塞队列，进行执行，确保不会一下大批量运行
            pageThreadLocal.set(GongfengFailPageEntity(pageNum, projIdList, "create task", "page level info", null))
            //过滤出不在原有清单中的公共项目
            gongfengModelList.forEach {
                val gongfengDupProjEntity =
                    allPublicProjects.find { gongfengPublicProjEntity -> gongfengPublicProjEntity.id == it.id }
                if (null == gongfengDupProjEntity) {
                    taskExecutor.execute {
                        try {
                            handleWithGongfengPublicProject(it, pageNum ?: 0)
                        } catch (e: Exception) {
                            logger.error("handle with gong feng public project fail!, err: ${e.message}")
                            val gongfengHandleFailEntity = GongfengFailPageEntity(
                                pageNum,
                                listOf(it.id),
                                "create task",
                                e.message,
                                e.stackTrace.contentToString()
                            )
                            gongfengFailPageRepository.save(gongfengHandleFailEntity)
                        }
                    }
                    Thread.sleep(600)
                } else {
                    logger.info("gongfeng public project info exists!, id: ${gongfengDupProjEntity.id}")
                    val gongfengPublicProjectEntity =
                        objectMapper.readValue(
                            objectMapper.writeValueAsString(it),
                            GongfengPublicProjEntity::class.java
                        )
                    gongfengPublicProjectEntity.id = gongfengDupProjEntity.id
                    gongfengPublicProjectEntity.entityId = gongfengDupProjEntity.entityId
                    gongfengPublicProjectEntity.synchronizeTime = System.currentTimeMillis()
                    gongfengPublicProjService.saveProject(gongfengPublicProjectEntity)
                }
            }
        } catch (e: Exception) {
            logger.error("execute task fail!, e: $e")
        }
    }

    /**
     * 更新工蜂统计信息
     */
    fun updateGongfengStatInfo(gongfengPageModel: GongfengStatPageModel) {
        try {
            val gongfengStatVOList = gongfengPageModel.statsProjList
            val bgId = gongfengPageModel.bgId
            val pageNum = gongfengPageModel.pageNum
            val gongfengStatEntityList = gongfengStatVOList.map {
                val gongfengStatEntity = GongfengStatProjEntity()
                BeanUtils.copyProperties(it, gongfengStatEntity)
                gongfengStatEntity.owners = it.owners
                gongfengStatEntity.id = it.id
                logger.info("gongfeng id: ${gongfengStatEntity.id}, public visibility: ${gongfengStatEntity.publicVisibility}, owners: ${gongfengStatEntity.owners}")
                gongfengStatEntity
            }
            logger.info(">>>>>>>>>bg id $bgId fetch page $pageNum")
            val start = System.currentTimeMillis()
            gongfengStatProjDao.upsertGongfengStatProjList(gongfengStatEntityList)
            logger.info("time >>> ${System.currentTimeMillis() - start}")
        } catch (e: Exception) {
            logger.error("update gongfeng stat info fail!", e)
        }
    }

    fun executeTriggerPipeline(triggerPipelineModel: TriggerPipelineModel) {
        try {
            logger.info("start to trigger pipeline process! pipeline id: ${triggerPipelineModel.pipelineId}, user name : ${triggerPipelineModel.owner}")
//            return
            //获取任务信息，对比版本号
            var taskInfo = if (triggerPipelineModel.taskId > 0) {
                taskRepository.findByTaskId(triggerPipelineModel.taskId)
            } else {
                taskRepository.findByPipelineId(triggerPipelineModel.pipelineId)
            }
            if (null == taskInfo) {
                taskInfo = setValueToInvalidTask(triggerPipelineModel)
            }
            //后续删除
            /*if(taskInfo.gongfengProjectId > 15000){
                logger.info("project id less than 15000, pass")
                return
            }*/

            //查询工蜂相应信息
            val gongfengPublicProjEntity = gongfengPublicProjService.findProjectById(triggerPipelineModel.gongfengId)
            val gongfengStatProjEntity = gongfengStatProjRepository.findFirstById(triggerPipelineModel.gongfengId)

            val checkResult = gongfengProjectChecker.check(
                TaskConstants.GongfengCheckType.SCHEDULE_TASK,
                gongfengPublicProjEntity,
                gongfengStatProjEntity,
                taskInfo
            )
            if (null != checkResult) {
                logger.info("check result invalid!")
                if (null == taskInfo) {
                    //如果不符合规范，且还未创建codecc任务，则直接创建一个失效任务
                    logger.info("this project is invalid project, need to register disabled task, id: ${gongfengPublicProjEntity.id}")
                    openSourceTaskRegisterService.registerDisabledTask(
                        gongfengPublicProjEntity.id, triggerPipelineModel.pipelineId,
                        triggerPipelineModel.projectId, checkResult
                    )
                }
                return
            }

            //查询分支信息，比较提交信息进行扫描
            var branchInfo: GongfengBranchModel? = null
            val inputBranch = gongfengPublicProjEntity.defaultBranch ?: "master"
            try {
                val url =
                    "$gitCodePath/api/v3/projects/${triggerPipelineModel.gongfengId}/repository/branches/$inputBranch"
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
                branchInfo = objectMapper.readValue(result, GongfengBranchModel::class.java)
            } catch (e: Exception) {
                logger.info("no permission to the master branch, gongfeng id: ${triggerPipelineModel.gongfengId}")
            }
            Thread.sleep(6000L)
            pipelineService.updateExistsCommonPipeline(
                gongfengPublicProjEntity,
                triggerPipelineModel.projectId,
                triggerPipelineModel.taskId,
                triggerPipelineModel.pipelineId,
                gongfengStatProjEntity.owners,
                ComConstants.CodeCCDispatchRoute.OPENSOURCE
            )
            with(triggerPipelineModel) {
                val buildIdResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
                    owner, projectId,
                    pipelineId, mapOf("scheduledTriggerPipeline" to "true"), ChannelCode.GONGFENGSCAN
                )
                logger.info("trigger pipeline successfully! build id: ${buildIdResult.data?.id}")
            }
            if (null != taskInfo) {
                if (null != branchInfo) {
                    taskDao.updateNameCnAndCommitId(
                        if (taskInfo.nameCn.startsWith("CODEPIPELINE")) {
                            gongfengPublicProjEntity.name
                        } else {
                            null
                        },
                        branchInfo.commit.id,
                        System.currentTimeMillis(),
                        taskInfo.taskId
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(
                "trigger pipeline fail for gongfeng project scan! pipeline id: ${triggerPipelineModel.pipelineId}",
                e
            )
            gongfengFailPageRepository.save(
                GongfengFailPageEntity(
                    null, listOf(triggerPipelineModel.gongfengId),
                    "trigger pipeline", e.message, e.stackTrace!!.contentToString()
                )
            )
        }
    }

    fun executeTriggerPipelineForManual(triggerPipelineModel: TriggerPipelineModel) {
        try {
            logger.info("start to trigger pipeline process! pipeline id: ${triggerPipelineModel.pipelineId}, user name : ${triggerPipelineModel.owner}")
//            return
            //获取任务信息，对比版本号
            val taskInfo = if (triggerPipelineModel.taskId > 0) {
                taskRepository.findByTaskId(triggerPipelineModel.taskId)
            } else {
                taskRepository.findByPipelineId(triggerPipelineModel.pipelineId)
            }
            if (null == taskInfo) {
                logger.info("task info empty!")
                return
            }
            //后续删除
            /*if(taskInfo.gongfengProjectId > 15000){
                logger.info("project id less than 15000, pass")
                return
            }*/
            //查询分支信息，比较提交信息进行扫描
            var branchInfo: GongfengBranchModel? = null
            try {
                val url = "$gitCodePath/api/v3/projects/${taskInfo.gongfengProjectId}/repository/branches/master"
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
                branchInfo = objectMapper.readValue(result, GongfengBranchModel::class.java)
            } catch (e: Exception) {
                logger.info("no permission to the master branch, gongfeng id: ${taskInfo.gongfengProjectId}")
            }
            //查询工蜂相应信息
            val gongfengPublicProjEntity = gongfengPublicProjService.findProjectById(taskInfo.gongfengProjectId)
            val gongfengStatProjEntity = gongfengStatProjRepository.findFirstById(taskInfo.gongfengProjectId)
            updateExistsCommonPipeline(
                gongfengPublicProjEntity,
                taskInfo.projectId,
                taskInfo.taskId,
                taskInfo.pipelineId,
                gongfengStatProjEntity.owners
            )
            with(triggerPipelineModel) {
                val buildIdResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
                    owner, projectId,
                    pipelineId, mapOf(), ChannelCode.GONGFENGSCAN
                )
                logger.info("trigger pipeline successfully! build id: ${buildIdResult.data?.id}")
            }
            if (!taskInfo.gongfengFlag && null != branchInfo) {
                taskInfo.gongfengCommitId = branchInfo.commit.id
                taskInfo.updatedDate = System.currentTimeMillis()
                taskService.saveTaskInfo(taskInfo)
            }
        } catch (e: Exception) {
            logger.error(
                "trigger pipeline fail for gongfeng project scan! pipeline id: ${triggerPipelineModel.pipelineId}",
                e
            )
            gongfengFailPageRepository.save(
                GongfengFailPageEntity(
                    null, listOf(triggerPipelineModel.gongfengId),
                    "trigger pipeline", e.message, e.stackTrace!!.contentToString()
                )
            )
        }
    }

    private fun updateExistsCommonPipeline(
        gongfengPublicProjEntity: GongfengPublicProjEntity,
        projectId: String, taskId: Long, pipelineId: String, owner: String
    ): Boolean {
        /**
         * 第一个stage的内容
         */
        val elementFirst = ManualTriggerElement(
            name = "手动触发",
            id = null,
            status = null,
            canElementSkip = false,
            useLatestParameters = false
        )

        val containerFirst = TriggerContainer(
            id = null,
            name = "demo",
            elements = arrayListOf(elementFirst),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = emptyList(),
            templateParams = null,
            buildNo = null,
            canRetry = null,
            containerId = null
        )

        val stageFirst = Stage(
            containers = arrayListOf(containerFirst),
            id = null
        )

        /**
         * 第二个stage
         */
        //用于判断是否需要过滤的插件
        val filterElement: Element = MarketBuildAtomElement(
            name = "动态判断是否需要扫描",
            atomCode = "CodeCCFilter",
            data = mapOf(
                "input" to mapOf(
                    "gongfengProjectId" to ""
                ),
                "output" to mapOf()
            )
        )

        val gitElement: Element = MarketBuildAtomElement(
            name = "拉取代码",
            atomCode = "gitCodeRepoCommon",
            version = "2.*",
            data = mapOf(
                "input" to
                    mapOf(
                        "username" to codeccPublicAccount,
                        "password" to codeccPublicPassword,
                        "refName" to "master",
                        "commitId" to "",
                        "enableAutoCrlf" to false,
                        "enableGitClean" to true,
                        "enableSubmodule" to true,
                        "enableSubmoduleRemote" to false,
                        "enableVirtualMergeBranch" to false,
                        "excludePath" to "",
                        "fetchDepth" to "",
                        "includePath" to "",
                        "localPath" to "",
                        "paramMode" to "SIMPLE",
                        "pullType" to "BRANCH",
                        "repositoryUrl" to gongfengPublicProjEntity.httpUrlToRepo,
                        "strategy" to "REVERT_UPDATE",
                        "tagName" to ""
                    ),
                "output" to mapOf()
            )
        )


        gitElement.additionalOptions = ElementAdditionalOptions(
            enable = true,
            continueWhenFailed = true,
            retryWhenFailed = false,
            retryCount = 1,
            timeout = 900L,
            runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
            otherTask = "",
            customVariables = listOf(
                NameAndValue(
                    key = "BK_CI_CODECC_SCAN_FILTER_PASS",
                    value = "true"
                )
            ),
            customCondition = ""
        )

        val codeccElement: Element =
            MarketBuildAtomElement(
                name = "CodeCC代码检查(New)",
                atomCode = "CodeccCheckAtomDebug",
                version = "4.*",
                data = mapOf(
                    "input" to mapOf(
                        "languages" to listOf<String>(),
                        "tools" to listOf("CLOC"),
                        "asynchronous" to "false",
                        "openScanPrj" to true
                    ),
                    "output" to mapOf()
                )
            )

        codeccElement.additionalOptions = ElementAdditionalOptions(
            enable = true,
            continueWhenFailed = true,
            retryWhenFailed = false,
            retryCount = 1,
            timeout = 900L,
            runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
            otherTask = "",
            customVariables = listOf(
                NameAndValue(
                    key = "BK_CI_GIT_REPO_BRANCH",
                    value = (gongfengPublicProjEntity.defaultBranch ?: "master")
                ),
                NameAndValue(
                    key = "BK_CI_CODECC_SCAN_FILTER_PASS",
                    value = "true"
                )
            ),
            customCondition = ""
        )

        val judgeElement = LinuxScriptElement(
            name = "判断是否推送",
            scriptType = BuildScriptType.SHELL,
            script = "# 您可以通过setEnv函数设置插件间传递的参数\n# setEnv \"FILENAME\" \"package.zip\"\n# 然后在后续的插件的表单中使用\${FILENAME}引用这个变量\n\n# 您可以在质量红线中创建自定义指标，然后通过setGateValue函数设置指标值\n# setGateValue \"CodeCoverage\" \$myValue\n# 然后在质量红线选择相应指标和阈值。若不满足，流水线在执行时将会被卡住\n\n# cd \${WORKSPACE} 可进入当前工作空间目录\necho \$BK_CI_GIT_REPO_BRANCH\necho \$BK_CI_CODECC_TASK_STATUS\nif [ \$BK_CI_GIT_REPO_BRANCH ] && [ \$BK_CI_GIT_REPO_BRANCH = 'master' ]\nthen\n    if [ ! \$BK_CI_CODECC_TASK_STATUS ]\n    then\n        setEnv BK_CI_CODECC_MESSAGE_PUSH 'true'\n    fi\nfi\necho \$BK_CI_CODECC_MESSAGE_PUSH\n",
            continueNoneZero = false,
            enableArchiveFile = false,
            archiveFile = ""
        )

        val notifyElement = MarketBuildLessAtomElement(
            name = "企业微信机器人推送",
            atomCode = "WechatWorkRobot",
            data = mapOf(
                "input" to mapOf(
                    "inputWebhook" to "e01e5f25-4362-4c6f-b163-18b4c529163b",
                    "inputChatid" to "",
                    "inputMsgtype" to "text",
                    "inputTextContent" to "【开源扫描构建失败提醒】\n蓝盾项目id:\${BK_CI_PROJECT_NAME}\n流水线id: \${BK_CI_PIPELINE_ID}\n构建id: \${BK_CI_BUILD_ID}",
                    "inputTextMentionedList" to "",
                    "inputMarkdownContent" to "",
                    "inputImageBase64" to "",
                    "inputImageMd5" to "",
                    "inputNewsTitle" to "",
                    "inputNewsDescription" to "",
                    "inputNewsUrl" to "",
                    "inputNewsPicurl" to "",
                    "inputRetry" to 1

                ),
                "output" to mapOf()
            )
        )

        with(notifyElement) {
            executeCount = 1
            canRetry = false
            additionalOptions = ElementAdditionalOptions(
                enable = true,
                continueWhenFailed = false,
                retryWhenFailed = false,
                retryCount = 1,
                timeout = 900,
                runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                otherTask = "",
                customVariables = listOf(
                    NameAndValue(
                        key = "BK_CI_CODECC_MESSAGE_PUSH",
                        value = "true"
                    )
                ),
                customCondition = ""
            )
        }

        val containerSecond = VMBuildContainer(
            id = null,
            name = "demo",
            elements = listOf(filterElement, gitElement, codeccElement, judgeElement, notifyElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.valueOf("LINUX"),
            vmNames = emptySet(),
            maxQueueMinutes = null,
            maxRunningMinutes = 80,
            buildEnv = null,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = imageName,
            dispatchType = CodeCCDispatchType(
                codeccTaskId = ComConstants.CodeCCDispatchRoute.OPENSOURCE.flag()
            ),
            canRetry = null,
            enableExternal = null,
            containerId = null,
            jobControlOption = JobControlOption(
                enable = true,
                timeout = 600,
                runCondition = JobRunCondition.STAGE_RUNNING,
                customVariables = null,
                customCondition = null
            ),
            mutexGroup = null,
            tstackAgentId = null
        )
        val stageSecond = Stage(
            containers = arrayListOf(containerSecond),
            id = null
        )

        logger.info(
            "assemble pipeline parameter successfully! gongfeng " +
                "project id: ${gongfengPublicProjEntity.id}"
        )
        /**
         * 总流水线拼装
         */
        val pipelineModel = Model(
            name = "CODEPIPELINE_${gongfengPublicProjEntity.id}",
            desc = gongfengPublicProjEntity.description,
            stages = arrayListOf(stageFirst, stageSecond),
            labels = emptyList(),
            instanceFromTemplate = null,
            pipelineCreator = null,
            srcTemplateId = null
        )

        val pipelineCreateResult = client.getDevopsService(ServicePipelineResource::class.java).edit(
            userId = owner.split(",")[0],
            projectId = projectId,
            pipelineId = pipelineId,
            pipeline = pipelineModel,
            channelCode = ChannelCode.GONGFENGSCAN
        )
        return if (pipelineCreateResult.isOk()) pipelineCreateResult.data
            ?: throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR) else throw CodeCCException(
            CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR
        )
    }

    /**
     * 为首次触发的失效项目补齐数据
     */
    private fun setValueToInvalidTask(triggerPipelineModel: TriggerPipelineModel) : TaskInfoEntity?{
        val taskInfoList = taskRepository.findByGongfengProjectIdIsAndCreateFromIs(
            triggerPipelineModel.gongfengId,
            ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
        )
        //防止原来因为首次触发失效的任务没有项目和流水线字段，导致会重复创建项目
        if(!taskInfoList.isNullOrEmpty()){
            val taskInfo = taskInfoList.findLast { (it.projectId.isNullOrBlank() || !it.projectId.startsWith("CUSTOMPROJ_")) &&
                (!it.nameCn.isNullOrBlank() && it.nameCn == "CODEPIPELINE_${triggerPipelineModel.gongfengId}") && it.status == TaskConstants.TaskStatus.DISABLE.value() }
            if(null != taskInfo && taskInfo.taskId > 0L){
                if(taskInfo.projectId.isNullOrBlank() || taskInfo.pipelineId.isNullOrBlank()){
                    taskDao.updateProjectIdAndPipelineId(
                        if(taskInfo.projectId.isNullOrBlank()){
                            triggerPipelineModel.projectId
                        } else {
                            null
                        },
                        if(taskInfo.pipelineId.isNullOrBlank()){
                            triggerPipelineModel.pipelineId
                        } else {
                            null
                        },
                        taskInfo.taskId
                    )
                }
                return taskInfo
            }
        }
        return null
    }


    /**
     * 判断近一个月变更的代码量
     */
    private fun judgeActiveProject(triggerPipelineModel: TriggerPipelineModel): Int {
        return try {
            val monthAgoDate = Calendar.getInstance()
            monthAgoDate.time = Date()
            monthAgoDate.add(Calendar.YEAR, -20)
            val beginDate = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssZ",
                Locale.ENGLISH
            ).format(monthAgoDate.time).replace("+", "%2B")
            val endDate = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssZ",
                Locale.ENGLISH
            ).format(Date()).replace("+", "%2B")
            val url =
                "$gitCodePath/api/v3/projects/${triggerPipelineModel.gongfengId}/tloc/user/diff?begin_date=$beginDate&end_date=$endDate"
            val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
            val projectCommitList: List<ProjectCommitInfo> =
                objectMapper.readValue(result, object : TypeReference<List<ProjectCommitInfo>>() {})
            if (projectCommitList.isNullOrEmpty()) {
                0
            } else {
                projectCommitList.map { it.totalWork }.reduce { acc, i ->
                    acc + i
                }
            }
        } catch (e: Exception) {
            logger.error("get total work fail! gongfeng id: ${triggerPipelineModel.gongfengId}", e)
            0
        }
    }

    fun executeActiveProjectTask(activeProjParseModel: ActiveProjParseModel) {
        try {
            if (gongfengPublicProjService.judgeActiveProjExists(activeProjParseModel.id)) {
                logger.info("active project already exists!")
                return
            }
            val activeProjEntity = GongfengActiveProjEntity()
            BeanUtils.copyProperties(activeProjParseModel, activeProjEntity)
            activeProjEntity.bkProject = activeProjParseModel.ownersOrg.indexOf("蓝鲸产品中心") >= 0
            gongfengPublicProjService.saveActiveProject(activeProjEntity)
            logger.info("save active project info successfully!")
        } catch (e: Exception) {
            logger.error("handle with gong feng public project fail!, err: ${e.message}")
            val gongfengHandleFailEntity = GongfengFailPageEntity(
                null,
                listOf(activeProjParseModel.id),
                "create active task",
                e.message,
                e.stackTrace.contentToString()
            )
            gongfengFailPageRepository.save(gongfengHandleFailEntity)
        }
    }

    /*fun executeActiveProjectTask(activeProjParseModel: ActiveProjParseModel) {
        try {
            //如果已存在，则不执行
            if(gongfengPublicProjService.judgeActiveProjExists(activeProjParseModel.id))
                return
            val activeProjEntity = GongfengActiveProjEntity()
            BeanUtils.copyProperties(activeProjParseModel, activeProjEntity)
            //1. 挑出蓝鲸的
            activeProjEntity.bkProject = activeProjParseModel.ownersOrg.indexOf("蓝鲸产品中心") >= 0
            //2. 开源去重，只操作闭源的
            if (null == gongfengPublicProjService.findProjectById(activeProjEntity.id)) {
                logger.info("closed source project! project id: ${activeProjParseModel.id}")
                handleWithActiveProject(activeProjParseModel)
            }
            else
            {
                logger.info("open source project! project id: ${activeProjParseModel.id}")
            }
            //3. todo 拿取他是开源的还是闭源的
            gongfengPublicProjService.saveActiveProject(activeProjEntity)
        } catch (e: Exception) {
            logger.error("handle with gong feng public project fail!, err: ${e.message}")
            val gongfengHandleFailEntity = GongfengFailPageEntity(
                null,
                listOf(activeProjParseModel.id),
                "create active task",
                e.message,
                e.stackTrace.contentToString()
            )
            gongfengFailPageRepository.save(gongfengHandleFailEntity)
        }
    }*/

    private fun handleWithActiveProject(activeProjParseModel: ActiveProjParseModel) {
        logger.info("start to handle with active project, gongfeng id: ${activeProjParseModel.id}")
        //注册蓝盾项目
        val projectId = pipelineService.createActiveProjDevopsProject(activeProjParseModel)
        logger.info("create project successfully! project id: $projectId")

        //获取员工组织信息
        val staffInfo = tofClientApi.getStaffInfoByUserName(activeProjParseModel.creator)
        val organizationInfo = tofClientApi.getOrganizationInfoByGroupId(staffInfo.data?.GroupId ?: -1)

        //注册任务
        val taskDetailVO = TaskDetailVO()
        taskDetailVO.projectId = "CODE_${activeProjParseModel.id}"
        taskDetailVO.nameCn = "工蜂活跃项目${activeProjParseModel.id}"
        taskDetailVO.pipelineId = ""
        taskDetailVO.pipelineName = "CODEPIPELINE_${activeProjParseModel.id}"
        taskDetailVO.gongfengFlag = true
        taskDetailVO.gongfengProjectId = activeProjParseModel.id

        taskDetailVO.devopsTools = objectMapper.writeValueAsString(listOf("CLOC"))
        taskDetailVO.bgId = organizationInfo?.bgId ?: -1
        taskDetailVO.deptId = organizationInfo?.deptId ?: -1
        taskDetailVO.centerId = organizationInfo?.centerId ?: -1
        taskDetailVO.groupId = staffInfo.data?.GroupId ?: -1

        //按流水线注册形式注册
        val taskResult = taskRegisterService.registerTask(taskDetailVO, activeProjParseModel.creator)
        val taskId = taskResult.taskId
        val taskEnName = taskResult.nameEn
        //创建蓝盾流水线
        val pipelineId =
            pipelineService.createGongfengActivePipeline(activeProjParseModel, projectId, taskEnName, taskId)
        logger.info("create active project pipeline successfully! pipeline id: $pipelineId")
        //更新流水线信息
        val taskInfo = taskService.getTaskById(taskId)
        taskInfo.pipelineId = pipelineId
        taskService.saveTaskInfo(taskInfo)
        //添加定时任务
        addGongfengJob(activeProjParseModel.id, activeProjParseModel.creator, projectId, pipelineId)
        logger.info("register task for gongfeng successfully!")
    }

    /**
     * 对工蜂开源项目进行处理
     */
    private fun handleWithGongfengPublicProject(newProject: GongfengPublicProjModel, pageNum: Int) {
        logger.info("start to handle with new project, project id: ${newProject.id}")

        // 验证目前的owner在职、非机器人
        if (null != newProject.owner) {
            if (newProject.owner!!.state != "active" || !validateOwnerName(newProject.owner!!.userName)) {
                newProject.owner = null
            }
        }
        // 上一步验证失败，则获取项目的其他在职、非机器人owner
        if (null == newProject.owner) {
            setProjectOwnerByProject(newProject)
        }

        if (null == newProject.owner) {
            logger.info("no owner info for project: ${newProject.id}")
            gongfengFailPageRepository.save(
                GongfengFailPageEntity(
                    null, listOf(newProject.id),
                    "create task", "no owner info for project", null
                )
            )
            openSourceTaskRegisterService.registerDisabledTask(
                newProject.id, null, null,
                ComConstants.OpenSourceDisableReason.OWNERPROBLEM.code
            )
            return
        }

        val projectId: String
        val pipelineId: String

        try {
            //获取项目管理员
            val adminUser = newProject.owner!!.userName
            logger.info("project user name: $adminUser")
            //注册蓝盾项目
            projectId = pipelineService.createGongfengDevopsProject(newProject)
            logger.info("create project successfully! project id: $projectId")

            //按流水线注册形式注册
//        val taskResult = taskRegisterService.registerTask(taskDetailVO, adminUser)
            //创建蓝盾流水线
            pipelineId = pipelineService.createGongfengDevopsPipeline(newProject, projectId)
            logger.info("create pipeline successfully! pipeline id: $pipelineId")
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("create task fail! gongfeng project id: ${newProject.id}")
            val gongfengHandleFailEntity = GongfengFailPageEntity(
                pageNum,
                listOf(newProject.id),
                "create task",
                e.message,
                e.stackTrace.contentToString()
            )
            gongfengFailPageRepository.save(gongfengHandleFailEntity)
            openSourceTaskRegisterService.registerDisabledTask(
                newProject.id, null, null,
                ComConstants.OpenSourceDisableReason.OWNERPROBLEM.code
            )
            return
        }

        //添加定时任务
        addGongfengJob(newProject.id, newProject.owner!!.userName, projectId, pipelineId)
        val gongfengPublicProjectEntity =
            objectMapper.readValue(objectMapper.writeValueAsString(newProject), GongfengPublicProjEntity::class.java)
        gongfengPublicProjectEntity.synchronizeTime = System.currentTimeMillis()
        gongfengPublicProjService.saveProject(gongfengPublicProjectEntity)
        logger.info("register task for gongfeng successfully!")

        logger.info("send msg to kafka cluster successfully!")
    }

    /**
     * 添加工蜂定时任务
     */
    private fun addGongfengJob(
        id: Int, userName: String, projectId: String,
        pipelineId: String
    ) {
        val jobExternalDto = JobExternalDto(
            jobName = null,
            className = "TriggerPipelineScheduleTask",
            classUrl = "${publicClassUrl}TriggerPipelineScheduleTask.java",
            cronExpression = getGongfengTriggerCronExpression(id),
            jobCustomParam = mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "taskId" to 0,
                "gongfengId" to id,
                "owner" to userName
            ),
            operType = OperationType.ADD
        )
        rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto)
    }

    /**
     *  验证rtx用户名
     */
    private fun validateOwnerName(userName: String): Boolean {
        val staffInfo = tofClientApi.getStaffInfoByUserName(userName)
        if (staffInfo.code != "00") {
            logger.info("tof client api error, code : ${staffInfo.code}, message:${staffInfo.message}, username: $userName")
        }
        return userName == staffInfo.data?.LoginName
    }

    /**
     * 根据项目成员清单设置owner信息
     */
    private fun setProjectOwnerByProject(newProject: GongfengPublicProjModel) {
        var page = 1
        var dataSize: Int
        do {
            val url = "$gitCodePath/api/v3/projects/${newProject.id}/members/all?page=$page&per_page=1000"
            try {
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
                val ownerList: List<OwnerInfo> =
                    objectMapper.readValue(result, object : TypeReference<List<OwnerInfo>>() {})
                logger.info("get project members,project id: ${newProject.id},members size: ${ownerList.size}")
                if (ownerList.isNullOrEmpty()) {
                    return
                }
                dataSize = ownerList.size
                for(ownerInfo in ownerList){
                    if (ownerInfo.accessLevel == 50 && ownerInfo.state == "active" && validateOwnerName(ownerInfo.userName)) {
                        logger.info("validated owner, username: ${ownerInfo.userName}, project id: ${newProject.id}")
                        newProject.owner = ownerInfo
                        return
                    }
                    logger.info("invalidated owner, username: ${ownerInfo.userName}, project id: ${newProject.id}")
                }
                page++
            } catch (e: Exception) {
                logger.error("get project member info fail! gongfeng id: ${newProject.id}")
                return
            }
        } while (dataSize >= 1000)
    }

    //将定时时间全天平均，以10分钟为间隔
    private fun getGongfengTriggerCronExpression(gongfengId: Int): String {
        val period = periodInfoCache.get("PERIOD")
        val startTime = periodInfoCache.get("STARTTIME")
        val remainder = gongfengId % (period * 6)
        val minuteNum = (remainder % 6) * 10
        val hourNum = (startTime + ((remainder / 6) % period)) % 24
        return "0 $minuteNum $hourNum * * ?"
    }

    /**
     * 获取开源扫描时间周期和时间起点
     */
    private fun getOpenSourceCheckPeriod(paramCode: String): Int {
        val baseDataEntity =
            baseDataRepository.findAllByParamTypeAndParamCode(ComConstants.KEY_OPENSOURCE_PERIOD, paramCode)
        //如果是周期的默认值是24，起点的默认值是0
        return if (baseDataEntity.isNullOrEmpty()) {
            if (paramCode == "PERIOD") 24 else 0
        } else {
            try {
                baseDataEntity[0].paramValue.toInt()
            } catch (e: Exception) {
                if (paramCode == "PERIOD") 24 else 0
            }
        }
    }
}