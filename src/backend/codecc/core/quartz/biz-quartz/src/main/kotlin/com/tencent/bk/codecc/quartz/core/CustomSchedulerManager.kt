package com.tencent.bk.codecc.quartz.core

import com.tencent.bk.codecc.quartz.job.IScheduleTask
import com.tencent.bk.codecc.quartz.job.ShardingJob
import com.tencent.bk.codecc.quartz.model.JobCompensateEntity
import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.JobInfoVO
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.bk.codecc.quartz.service.JobManageService
import com.tencent.bk.codecc.quartz.service.ShardingRouterService
import com.tencent.bk.codecc.quartz.strategy.router.EnumRouterStrategy
import com.tencent.bk.codecc.quartz.strategy.sharding.EnumShardingStrategy
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_DELETE_ALL_JOB
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_INIT_ALL_JOB
import groovy.lang.GroovyClassLoader
import org.codehaus.groovy.control.CompilationFailedException
import org.quartz.*
import org.quartz.impl.matchers.EverythingMatcher
import org.quartz.impl.matchers.GroupMatcher
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat

open class CustomSchedulerManager @Autowired constructor(
    private val applicationContext: ApplicationContext,
    private val shardingRouterService: ShardingRouterService,
    private val jobManageService: JobManageService,
    private val scheduler: Scheduler,
    private val shardingListener: ShardingListener,
    private val redisTemplate: RedisTemplate<String, String>,
    private val rabbitTemplate: RabbitTemplate
) {

    private val groovyClassLoader = GroovyClassLoader()

    companion object {
        private val logger = LoggerFactory.getLogger(CustomSchedulerManager::class.java)
        //分片算法
        val shardingStrategy = EnumShardingStrategy.ASCEND
        //路由算法
        val routerStrategy = EnumRouterStrategy.CONSISTENT_HASH
        //job分组名称
        val jobGroup = "bkJobGroup"
        //触发器分组名称
        val triggerGroup = "bkTriggerGroup"
    }

    @Scheduled(cron = "15 0/30 * * * ?")
    fun monitorConsulSharding() {
        //初始化分片及job分配
        synchronized(this) {
            val shardingResult = shardingStrategy.getShardingStrategy().getShardingResult()
            if (null == shardingResult) {
                initialize()
            } else {
                monitorSharding()
            }
        }
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    fun monitorJobCompensate() {
        //定时查询补偿
        queryAndSaveCompensateInfo()
    }

    /**
     * 查询并执行补偿
     */
    private fun queryAndExecuteCompensate() {
        val redisLock = RedisLock(
            redisTemplate,
            "quartz:cluster:compensate:${shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag}",
            20
        )
        if (redisLock.tryLock()) {
            val hashKey =
                "${RedisKeyConstants.BK_JOB_CLUSTER_RECORD}${CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag}"
            val jobCompensateInfo = redisTemplate.opsForHash<String, String>().entries(hashKey)
            if (jobCompensateInfo.isNotEmpty()) {
                jobCompensateInfo.forEach { t, _ ->
                    logger.info("start to compensate uncompleted job! job info is: $t")
                    val jobName = t.substring(0, t.indexOfLast { it == '_' })
                    val jobInstanceEntity = jobManageService.findJobByName(jobName)
                    val scheduleExecuteTime = t.substring(t.indexOfLast { it == '_' } + 1)
                    val executeDate = SimpleDateFormat("yyyyMMddHHmmss").parse(scheduleExecuteTime)
                    //再执行一遍
                    val scheduleTask =
                        SpringContextUtil.getBean(IScheduleTask::class.java, jobInstanceEntity.className.decapitalize())
                    val quartzContext = QuartzJobContext(
                        jobInstanceEntity.jobName, jobInstanceEntity.className.decapitalize(),
                        shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.shardNum,
                        shardingStrategy.getShardingStrategy().getShardingResult()!!.currentNode.nodeNum,
                        executeDate,
                        jobInstanceEntity.jobParam
                    )
                    scheduleTask.executeTask(quartzContext)
                    redisTemplate.opsForHash<String, String>().delete(hashKey, t)
                }
            }
        }
    }

    /**
     * 查询并持久化补偿信息
     */
    private fun queryAndSaveCompensateInfo() {
        if (null == shardingStrategy.getShardingStrategy().getShardingResult()) {
            logger.info("shard info not initialized yet!")
            return
        }
        val redisLock = RedisLock(
            redisTemplate,
            "quartz:cluster:compensate:${shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag}",
            20
        )
        if (redisLock.tryLock()) {
            val hashKey =
                "${RedisKeyConstants.BK_JOB_CLUSTER_RECORD}${shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag}"
            val jobCompensateInfo = redisTemplate.opsForHash<String, String>().entries(hashKey)
            if (null != jobCompensateInfo && jobCompensateInfo.isNotEmpty()) {
                jobCompensateInfo.forEach { t, _ ->
                    logger.info("start to save compensate info! job info is: $t")
                    val jobName = t.substring(0, t.indexOfLast { it == '_' })
                    val scheduleExecuteTime = t.substring(t.indexOfLast { it == '_' } + 1)
                    val executeDate = SimpleDateFormat("yyyyMMddHHmmss").parse(scheduleExecuteTime)
                    logger.info("job name is: $jobName, schedule execute time is: $executeDate")
                    val trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobName, triggerGroup))
                    //只有当后续又触发时，才确保是补偿，防止记录进行过程中的记录
                    if (null != trigger &&
                        null != trigger.previousFireTime &&
                        executeDate < trigger.previousFireTime
                    ) {
                        logger.info("trigger previous fire time is: ${trigger.previousFireTime}")
                        logger.info("start to save compensate info!")
                        val jobCompensateEntity = JobCompensateEntity(
                            jobName, shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag,
                            scheduleExecuteTime, false
                        )
                        jobManageService.saveJobCompensate(jobCompensateEntity)
                        redisTemplate.opsForHash<String, String>().delete(hashKey, t)
                    }
                }
            }
        }
    }

    /**
     * 初始化工作
     */
    private fun initialize() {
        //将接口所有子类注入到容器中
        beanInjection()
        //添加监听器
        scheduler.listenerManager.addTriggerListener(shardingListener, EverythingMatcher.allTriggers())
        //初始化分片
        val shardingResult = shardingRouterService.initSharding(shardingStrategy)
        logger.info("init sharding successfully!")
        val totalJobList = jobManageService.findAllJobs()
        //根据当前分片和job全量信息取出该分片信息并加入到调度中
        val qualifiedJobList = shardingRouterService.initJobInstance(
            shardingResult, totalJobList,
            routerStrategy
        )
        addJobs(qualifiedJobList)
        qualifiedJobList.forEach {
            it.shardTag = shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag
            //暂停状态为1的
            if(it.status == 1){
                parseJob(it)
                Thread.sleep(100L)
            }
        }
        jobManageService.saveJobs(qualifiedJobList)
        logger.info("all job added to scheduler, initialize finish!")
    }

    fun stop() {
        try {
            scheduler.shutdown(true)
        } catch (e: Exception) {
            logger.error("scheduler shut down fail! e: ${e.message}")
        }
    }

    /**
     * 注册新job或者移除job
     */
    fun newJobComeOrRemove(jobInstanceEntity: JobInstanceEntity, operType: OperationType) {
        synchronized(this) {
            when (operType) {
                OperationType.ADD -> {
                    if (shardingRouterService.judgeCurrentShardJob(
                            jobInstanceEntity,
                            shardingStrategy,
                            routerStrategy
                        )
                    ) {
                        addJob(jobInstanceEntity)
                        jobInstanceEntity.shardTag =
                            shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag
                        jobManageService.saveJob(jobInstanceEntity)
                    }
                }
                OperationType.REMOVE -> {
                    if (shardingRouterService.judgeCurrentShardJob(jobInstanceEntity, shardingStrategy, routerStrategy))
                        deleteJob(jobInstanceEntity)
                }
                OperationType.RESCHEDULE -> {
                    if (shardingRouterService.judgeCurrentShardJob(
                            jobInstanceEntity,
                            shardingStrategy,
                            routerStrategy
                        )
                    ) {
                        rescheduleJob(jobInstanceEntity)
                    }
                }
                OperationType.PARSE -> {
                    if (shardingRouterService.judgeCurrentShardJob(
                            jobInstanceEntity,
                            shardingStrategy,
                            routerStrategy
                        )
                    ) {
                        parseJob(jobInstanceEntity)
                    }
                }
                OperationType.RESUME -> {
                    if (shardingRouterService.judgeCurrentShardJob(
                            jobInstanceEntity,
                            shardingStrategy,
                            routerStrategy
                        )
                    ) {
                        resumeJob(jobInstanceEntity)
                    }
                }

            }
            jobManageService.addOrRemoveJobToCache(jobInstanceEntity, operType)
        }
    }

    /**
     * 监控是否重新分片或者节点变化
     */
    private fun monitorSharding() {
        logger.info("start to monitor consul service list")
        //先取老的分片信息
        val oldShardingResult = shardingStrategy.getShardingStrategy().getShardingResult()
        val jobChangeInfo = shardingRouterService.reShardAndReRouter(
            shardingStrategy,
            routerStrategy
        )
        //取新的分片信息
        val newShardingResult = shardingStrategy.getShardingStrategy().getShardingResult()
        if (jobChangeInfo.addJobInstances.isNotEmpty()) {
            addJobs(jobChangeInfo.addJobInstances)
            jobChangeInfo.addJobInstances.forEach {
                //对于本节点新增的job，改变原来其分片数
                it.shardTag = newShardingResult!!.currentShard.tag
                if(it.status == 1){
                    parseJob(it)
                    Thread.sleep(100L)
                }
            }
            jobManageService.saveJobs(jobChangeInfo.addJobInstances)
        }

        if (jobChangeInfo.removeJobInstances.isNotEmpty()) {
            deleteJobs(jobChangeInfo.removeJobInstances)
        }

        for (oldNode in oldShardingResult!!.currentShard.nodeList) {
            //如果有节点下线，则要查询并执行补偿
            if (newShardingResult!!.currentShard.nodeList.find { newNode ->
                    oldNode.host == newNode.host && oldNode.port == newNode.port
                } == null)
                queryAndSaveCompensateInfo()
            break
        }
        logger.info("job monitoring finish!")
    }

    /**
     * 将对应接口的实现类都注入到容器中
     */
    private fun beanInjection() {
        val reflection = Reflections("com.tencent.bk")
        val taskClasses = reflection.getSubTypesOf(IScheduleTask::class.java)
        val defaultListableBeanFactory = applicationContext.autowireCapableBeanFactory as DefaultListableBeanFactory
        taskClasses.forEach {
            val beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(it)
            defaultListableBeanFactory.registerBeanDefinition(
                it.simpleName.decapitalize(),
                beanDefinition.beanDefinition
            )
        }
    }

    /**
     * 批量添加定时任务
     */
    private fun addJobs(jobInstances: List<JobInstanceEntity>) {
        val jobTriggerMap = mutableMapOf<JobDetail, List<Trigger>>()
        jobInstances.forEach {
            with(it) {
                val beanName = className.decapitalize()
                if (!SpringContextUtil.beanExistsWithName(beanName)) {
                    if (!registerJobBean(classUrl, className)) {
                        logger.error("bean not exists with name of $beanName, class url: $className")
                        return@forEach
                    }
                }

                val trigger = TriggerBuilder.newTrigger().withIdentity(
                    triggerName,
                    triggerGroup
                )
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing())
                    .build()
                val jobKey = JobKey.jobKey(jobName, jobGroup)
                val jobParamMap = mapOf(
                    "beanName" to className.decapitalize(),
                    "jobCustomParam" to jobParam
                )
                val jobDetail = JobBuilder.newJob(ShardingJob::class.java).withIdentity(jobKey)
                    .usingJobData(JobDataMap(jobParamMap)).build()
                jobTriggerMap[jobDetail] = listOf(trigger)
            }
        }
        try {
            scheduler.scheduleJobs(jobTriggerMap, true)
        } catch (e: SchedulerException) {
            //如果报错则单个加，确保影响面最低
            logger.error("trigger has problem when scheduling job")
            jobInstances.forEach {
                addJob(it)
            }
        }
    }

    /**
     * 由于要对每个job都验重，所以还是用单个添加的方法
     */
    private fun addJob(jobInstanceEntity: JobInstanceEntity) {
        with(jobInstanceEntity) {
            logger.info("start to add job to scheduler! job info: $jobInstanceEntity")
            //对job进行校验,如果类不存在，则用URLClassLoader进行类的远程加载，并注入bean
            val beanName = className.decapitalize()
            if (!SpringContextUtil.beanExistsWithName(beanName)) {
                if (!registerJobBean(classUrl, className)) {
                    logger.error("bean not exists with name of $beanName, class url: $className")
                    return
                }
            }
            var trigger = TriggerBuilder.newTrigger().withIdentity(
                triggerName,
                triggerGroup
            ).withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing())
                .build()
            val jobKey = JobKey.jobKey(jobName, jobGroup)
            val jobParamMap = mapOf(
                "beanName" to className.decapitalize(),
                "jobCustomParam" to jobParam
            )
            val jobDetail = JobBuilder.newJob(ShardingJob::class.java).withIdentity(jobKey)
                .usingJobData(JobDataMap(jobParamMap)).build()

            //判断原有该job是否存在，如果存在则reschedule
            val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
            val previousTrigger = scheduler.getTrigger(triggerKey)
            if (null == previousTrigger) {
                logger.info("this is brand new trigger!")
                try {
                    scheduler.scheduleJob(jobDetail, trigger)
                } catch (e1: ObjectAlreadyExistsException) {
                    trigger = trigger.triggerBuilder.withIdentity(triggerKey).withSchedule(
                        CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing()
                    ).build()
                    scheduler.rescheduleJob(triggerKey, trigger)
                } catch (e2: Exception) {
                    logger.error("error when adding job to schedule, error: ${e2.message}")
                    scheduler.deleteJob(jobKey)
                }
            } else {
                logger.info("trigger already exists!")
                trigger = trigger.triggerBuilder.withIdentity(triggerKey).withSchedule(
                    CronScheduleBuilder.cronSchedule(cronExpression)
                ).build()
                scheduler.rescheduleJob(triggerKey, trigger)
            }
        }
    }

    /**
     * 重新定时任务
     */
    private fun rescheduleJob(jobInstanceEntity: JobInstanceEntity) {
        with(jobInstanceEntity) {
            val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
            try {
                var trigger = scheduler.getTrigger(triggerKey) as CronTrigger
                if (trigger.cronExpression.equals(cronExpression)) {
                    return
                }

                var triggerNew = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(
                        CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing()
                    )
                    .build()

                scheduler.rescheduleJob(triggerKey, triggerNew)
            } catch (e: Exception) {
                logger.info("reschedule job fail! job name: ${jobInstanceEntity.jobName}")
            }
        }
    }


    /**
     * 暂停定时任务
     */
    private fun parseJob(jobInstanceEntity: JobInstanceEntity) {
        with(jobInstanceEntity) {
            val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
            try {
                scheduler.pauseTrigger(triggerKey)
            } catch (e: Exception) {
                logger.info("parse job fail! job name: ${jobInstanceEntity.jobName}")
            }
        }
    }


    /**
     * 恢复定时任务
     */
    private fun resumeJob(jobInstanceEntity: JobInstanceEntity) {
        with(jobInstanceEntity) {
            val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
            try {
                scheduler.resumeTrigger(triggerKey)
            } catch (e: Exception) {
                logger.info("resume job fail! job name: ${jobInstanceEntity.jobName}")
            }
        }
    }


    /**
     * 删除job
     */
    private fun deleteJob(jobInstanceEntity: JobInstanceEntity) {
        try {
            scheduler.deleteJob(JobKey.jobKey(jobInstanceEntity.jobName, jobGroup))
            logger.info("delete job successfully! job name: ${jobInstanceEntity.jobName}, job group: $jobGroup")
        } catch (e: Exception) {
            logger.info(
                "delete job fail! job name: ${jobInstanceEntity.jobName}, job group: $jobGroup," +
                    "error message: ${e.message}"
            )
        }
    }

    /**
     * 删除job
     */
    private fun deleteJobs(jobInstances: List<JobInstanceEntity>) {
        try {
            val jobKeys = jobInstances.map { JobKey.jobKey(it.jobName, jobGroup) }
            scheduler.deleteJobs(jobKeys)
            logger.info("delete jobs successfully!")
        } catch (e: Exception) {
            logger.info("delete jobs fail!")
        }
    }

    fun getExistingJob(): List<JobInfoVO> {
        val jobNames = scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)).map { it.name }
        return jobManageService.findJobsByName(jobNames).map {
            jobManageService.convert(it)
        }
    }

    fun deleteAllJobs(dataDelete: Int) {
        val shardingResult = shardingStrategy.getShardingStrategy().getShardingResult()
            ?: return
        if (shardingResult.currentShard.shardNum == 1 &&
            shardingResult.currentNode.nodeNum == 1
        ) {
            if (dataDelete == 1) {
                jobManageService.deleteAllJobs()
            }
            rabbitTemplate.convertAndSend(EXCHANGE_GONGFENG_DELETE_ALL_JOB, "", "")
        }
    }

    fun deleteScheduledJobs() {
        synchronized(this) {
            scheduler.clear()
            jobManageService.deleteAllCacheJobs()
        }
    }

    /**
     * 初始化job
     */
    fun manualInitialize() {
        synchronized(this) {
            try {
                logger.info("start manual job initialization")
                val shardingResult = shardingStrategy.getShardingStrategy().getShardingResult()
                val totalJobList = jobManageService.findAllJobs()
                //根据当前分片和job全量信息取出该分片信息并加入到调度中
                val qualifiedJobList = shardingRouterService.initJobInstance(
                    shardingResult!!, totalJobList,
                    routerStrategy
                )
                addJobs(qualifiedJobList)
                //暂停状态为1的
                qualifiedJobList.filter { it.status == 1 }.forEach {
                    parseJob(it)
                    Thread.sleep(100L)
                }
                qualifiedJobList.forEach {
                    it.shardTag = shardingStrategy.getShardingStrategy().getShardingResult()!!.currentShard.tag
                }
                jobManageService.saveJobs(qualifiedJobList)
                logger.info("all job added to scheduler, initialize finish!")
            } catch (e: Exception) {
                logger.error("initialize job information fail!")
            }
        }
    }

    fun initAllJobs() {
        val shardingResult = shardingStrategy.getShardingStrategy().getShardingResult()
            ?: return
        if (shardingResult.currentShard.shardNum == 1 &&
            shardingResult.currentNode.nodeNum == 1
        ) {
            rabbitTemplate.convertAndSend(EXCHANGE_GONGFENG_INIT_ALL_JOB, "", "")
        }
    }

    //动态加载逻辑
    private fun registerJobBean(classUrl: String?, className: String): Boolean {
        logger.info("start to groovy load class, class name: $className")
        if (classUrl.isNullOrBlank()) {
            logger.error("class url is null!")
            return false
        }
        try {
            val url = URL(classUrl)
            val fileText = url.readText()
            if (fileText.isBlank()) {
                logger.info("file content is blank!")
                return false
            }
            val scheduleTaskClass = groovyClassLoader.parseClass(fileText.trim())
            if (!IScheduleTask::class.java.isAssignableFrom(scheduleTaskClass)) {
                logger.error("class ${scheduleTaskClass.simpleName} is not implementation of IScheduleTask!")
                return false
            }
            val defaultListableBeanFactory = applicationContext.autowireCapableBeanFactory as DefaultListableBeanFactory
            val beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(scheduleTaskClass)
            defaultListableBeanFactory.registerBeanDefinition(
                scheduleTaskClass.simpleName.decapitalize(),
                beanDefinition.beanDefinition
            )
            return true
        } catch (e1: MalformedURLException) {
            logger.error("incorrent url form! url: $classUrl")
            return false
        } catch (e2: CompilationFailedException) {
            logger.error("compile file fail!, error message: ${e2.message}")
            return false
        } catch (e3: Exception) {
            logger.error("load custom class fail!, class name : $className, class url: $classUrl, error message: ${e3.message}")
            return false
        }
    }
}
