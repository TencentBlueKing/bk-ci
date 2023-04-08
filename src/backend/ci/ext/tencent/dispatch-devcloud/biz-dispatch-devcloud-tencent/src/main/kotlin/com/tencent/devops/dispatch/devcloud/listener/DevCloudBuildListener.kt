package com.tencent.devops.dispatch.devcloud.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.I18NConstant.BK_FAILED_START_DEVCLOUD
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.dao.BuildContainerPoolNoDao
import com.tencent.devops.dispatch.devcloud.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.ContainerStatus
import com.tencent.devops.dispatch.devcloud.pojo.ContainerType
import com.tencent.devops.dispatch.devcloud.pojo.Credential
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudContainer
import com.tencent.devops.dispatch.devcloud.pojo.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.devcloud.pojo.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.devcloud.pojo.Params
import com.tencent.devops.dispatch.devcloud.pojo.Pool
import com.tencent.devops.dispatch.devcloud.pojo.Registry
import com.tencent.devops.dispatch.devcloud.pojo.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.utils.DevCloudJobRedisUtils
import com.tencent.devops.dispatch.devcloud.utils.PipelineContainerLock
import com.tencent.devops.dispatch.devcloud.utils.RedisUtils
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudBuildRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Random

/**
 * deng
 * 2019-03-26
 */
@Component
class DevCloudBuildListener @Autowired constructor(
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val redisUtils: RedisUtils,
    private val dcPerformanceOptionsDao: DcPerformanceOptionsDao,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter,
    private val devCloudJobRedisUtils: DevCloudJobRedisUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : BuildListener {

    @Value("\${registry.host}")
    val registryHost: String? = null

    @Value("\${registry.userName}")
    val registryUser: String? = null

    @Value("\${registry.password}")
    val registryPwd: String? = null

    @Value("\${devCloud.cpu}")
    var cpu: Int = 32

    @Value("\${devCloud.memory}")
    var memory: String = "65535M"

    @Value("\${devCloud.disk}")
    var disk: String = "500G"

    @Value("\${devCloud.entrypoint}")
    val entrypoint: String = "devcloud_init.sh"

    @Value("\${atom.fuse.container.label}")
    val fuseContainerLabel: String? = null

    @Value("\${atom.fuse.atom-code}")
    val fuseAtomCode: String? = null

    private val threadLocalCpu = ThreadLocal<Int>()
    private val threadLocalMemory = ThreadLocal<String>()
    private val threadLocalDisk = ThreadLocal<String>()

    private val buildPoolSize = 100000 // 单个流水线可同时执行的任务数量

    private val overlayFsLabel = "checkout"

    private val shutdownLockBaseKey = "dispatch_devcloud_shutdown_lock_"

    private val devCloudHelpUrl =
        "<a target='_blank' href='https://iwiki.woa.com/pages/viewpage.action?pageId=218952404'>" +
            "【DevCloud容器问题FAQ】</a>"

    override fun getShutdownQueue(): String {
        return ".devcloud.public"
    }

    override fun getStartupDemoteQueue(): String {
        return ".devcloud.public.demote"
    }

    override fun getStartupQueue(): String {
        return ".devcloud.public"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.DOCKER_DEVCLOUD
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        startUp(dispatchMessage)
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        startUp(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        if (event.source == "shutdownAllVMTaskAtom") {
            // 入缓存，保证次流水线正在运行还未结束的job能够正常结束
            // redisUtils.setShutdownCancelMessage(event.buildId, event)

            // 同一个buildId的多个shutdownAllVMTaskAtom事件一定在短时间内到达，300s足够
            val shutdownLock = RedisLock(redisOperation, shutdownLockBaseKey + event.buildId, 300L)
            try {
                if (shutdownLock.tryLock()) {
                    doShutdown(event)
                } else {
                    logger.info("shutdownAllVMTaskAtom of {} already invoked, ignore", event.buildId)
                }
            } catch (e: Exception) {
                logger.info("Fail to shutdown VM", e)
            } finally {
                shutdownLock.unlock()
            }
        } else {
            doShutdown(event)
        }
    }

    private fun startUp(dispatchMessage: DispatchMessage) {
        logger.info("On start up - ($dispatchMessage)")
        printLogs(dispatchMessage, "准备创建腾讯自研云（云devnet资源)构建机...")

        val buildContainerPoolNo = buildContainerPoolNoDao.getDevCloudBuildLastPoolNo(
            dslContext,
            dispatchMessage.buildId,
            dispatchMessage.vmSeqId,
            dispatchMessage.executeCount ?: 1
        )
        logger.info("buildContainerPoolNo: $buildContainerPoolNo")
        if (buildContainerPoolNo.isNotEmpty() && buildContainerPoolNo[0].second != null) {
            retry()
        } else {
            val tryTime = 0
            createOrStartContainer(dispatchMessage, tryTime)
        }
    }

    private fun createOrStartContainer(dispatchMessage: DispatchMessage, tryTime: Int) {
        threadLocalCpu.set(cpu)
        threadLocalMemory.set(memory)
        threadLocalDisk.set(disk)

        try {
            val containerPool = getContainerPool(dispatchMessage)
            printLogs(dispatchMessage, "启动镜像：${containerPool.container}")
            if (!containerPool.performanceConfigId.isNullOrBlank() && containerPool.performanceConfigId != "0") {
                val performanceOption =
                    dcPerformanceOptionsDao.get(dslContext, containerPool.performanceConfigId!!.toLong())
                if (performanceOption != null) {
                    threadLocalCpu.set(performanceOption.cpu)
                    threadLocalMemory.set("${performanceOption.memory}M")
                    threadLocalDisk.set("${performanceOption.disk}G")
                }
            }

            val (lastIdleContainer, poolNo, containerChanged) = getIdleContainer(dispatchMessage)

            // 记录构建历史
            recordBuildHisAndGatewayCheck(poolNo, lastIdleContainer, dispatchMessage)

            // 用户第一次构建，或者用户更换了镜像，或者容器配置有变更，则重新创建容器。否则，使用已有容器，start起来即可
            if (null == lastIdleContainer || containerChanged) {
                logger.info("buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                                "create new container, poolNo: $poolNo")
                createNewContainer(dispatchMessage, containerPool, poolNo)
            } else {
                logger.info("buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                                "start idle container, containerName: $lastIdleContainer")
                startContainer(lastIdleContainer, dispatchMessage, poolNo)
            }
        } catch (e: BuildFailureException) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                    "create devCloud failed. msg:${e.message}. \n$devCloudHelpUrl"
            )
            onFailure(
                e.errorType,
                e.errorCode,
                e.formatErrorMessage,
                (e.message ?: I18nUtil.getCodeLanMessage(
                    messageCode = BK_FAILED_START_DEVCLOUD,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )) +
                    "\n容器构建异常请参考：$devCloudHelpUrl"
            )
        } catch (e: Exception) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                    "create devCloud failed, msg:${e.message}"
            )
            if (e.message.equals("timeout")) {
                onFailure(
                    ErrorCodeEnum.DEVCLOUD_INTERFACE_TIMEOUT.errorType,
                    ErrorCodeEnum.DEVCLOUD_INTERFACE_TIMEOUT.errorCode,
                    ErrorCodeEnum.DEVCLOUD_INTERFACE_TIMEOUT.formatErrorMessage,
                    "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 接口请求超时"
                )
            }
            onFailure(
                ErrorCodeEnum.SYSTEM_ERROR.errorType,
                ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                "创建构建机失败，错误信息:${e.message}. \n容器构建异常请参考：$devCloudHelpUrl"
            )
        }
    }

    private fun recordBuildHisAndGatewayCheck(
        poolNo: Int,
        lastIdleContainer: String?,
        dispatchMessage: DispatchMessage
    ) {
        devCloudBuildHisDao.create(
            dslContext = dslContext,
            projectId = dispatchMessage.projectId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            poolNo = poolNo.toString(),
            secretKey = dispatchMessage.secretKey,
            containerName = lastIdleContainer ?: "",
            cpu = threadLocalCpu.get(),
            memory = threadLocalMemory.get(),
            disk = threadLocalDisk.get(),
            executeCount = dispatchMessage.executeCount ?: 1
        )
    }

    private fun createNewContainer(
        dispatchMessage: DispatchMessage,
        containerPool: Pool,
        poolNo: Int
    ) {
        val (host, name, tag) = CiYamlUtils.parseImage(containerPool.container!!)
        val userName = containerPool.credential!!.user
        val password = containerPool.credential!!.password

        with(dispatchMessage) {
            val containerLabels = mutableMapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "buildId" to buildId,
                "vmSeqId" to vmSeqId
            )

            // 针对fuse插件优化
            if (fuseAtomCode!! in atoms.keys) {
                val (key, value) = fuseContainerLabel!!.split(":")
                containerLabels[key] = value
            }

            // overlayfs代码拉取优化
            if (overlayFsLabel in atoms.keys) {
                containerLabels[overlayFsLabel] = "true"
            }

            val (devCloudTaskId, createName) = dispatchDevCloudClient.createContainer(
                this, DevCloudContainer(
                "brief",
                buildId,
                ContainerType.DEV.getValue(),
                "$name:$tag",
                Registry(host, userName, password),
                threadLocalCpu.get(),
                threadLocalMemory.get(),
                threadLocalDisk.get(),
                1,
                emptyList(),
                generatePwd(),
                Params(
                        env = generateEnvs(this),
                        command = listOf("/bin/sh", entrypoint),
                        labels = containerLabels,
                        ipEnabled = false
                    )
                )
            )
            logger.info("buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo " +
                            "createContainer, taskId:($devCloudTaskId)")
            printLogs(this, "下发创建构建机请求成功，containerName: $createName 等待机器启动...")

            // 缓存创建容器信息，防止服务中断或重启引起的信息丢失
            // redisUtils.setCreatingContainer(createName, dispatchMessage.userId)

            val createResult = dispatchDevCloudClient.waitTaskFinish(
                userId,
                projectId,
                pipelineId,
                devCloudTaskId
            )

            // 创建完成移除缓存信息
            // redisUtils.removeCreatingContainer(createName, userId)

            if (createResult.first == TaskStatus.SUCCEEDED) {
                // 启动成功
                val containerName = createResult.second
                logger.info("buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo " +
                                "start dev cloud vm success, wait for agent startup...")
                printLogs(this, "构建机启动成功，等待Agent启动...")

                devCloudBuildDao.createOrUpdate(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo,
                    projectId = projectId,
                    containerName = containerName,
                    image = this.dispatchMessage,
                    status = ContainerStatus.BUSY.status,
                    userId = userId,
                    cpu = threadLocalCpu.get(),
                    memory = threadLocalMemory.get(),
                    disk = threadLocalDisk.get()
                )

                // 更新历史表中containerName
                devCloudBuildHisDao.updateContainerName(dslContext, buildId, vmSeqId, containerName, executeCount ?: 1)

                // 创建成功的要记录，shutdown时关机，创建失败时不记录，shutdown时不关机
                buildContainerPoolNoDao.setDevCloudBuildLastContainer(
                    dslContext,
                    buildId,
                    vmSeqId,
                    executeCount ?: 1,
                    containerName,
                    poolNo.toString()
                )
            } else {
                // 清除构建异常容器，并重新置构建池为空闲
                clearExceptionContainer(this, createName)
                devCloudBuildDao.updateStatus(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo,
                    status = ContainerStatus.IDLE.status
                )
                devCloudBuildHisDao.updateContainerName(dslContext, buildId, vmSeqId, createName, executeCount ?: 1)
                onFailure(
                    createResult.third.errorType,
                    createResult.third.errorCode,
                    ErrorCodeEnum.CREATE_VM_ERROR.formatErrorMessage,
                    "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 构建机创建失败:${createResult.second}"
                )
            }
        }
    }

    private fun startContainer(
        containerName: String,
        dispatchMessage: DispatchMessage,
        poolNo: Int
    ) {
        with(dispatchMessage) {
            val containerLabels = mutableMapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "buildId" to buildId,
                "vmSeqId" to vmSeqId
            )

            // 针对fuse和checkout插件优化
            if (fuseAtomCode!! in atoms.keys) {
                val (key, value) = fuseContainerLabel!!.split(":")
                containerLabels[key] = value
            }

            // overlayfs代码拉取优化
            if (overlayFsLabel in atoms.keys) {
                containerLabels[overlayFsLabel] = "true"
            }

            val devCloudTaskId = dispatchDevCloudClient.operateContainer(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                name = containerName,
                action = Action.START,
                param = Params(
                    env = generateEnvs(this),
                    command = listOf("/bin/sh", entrypoint),
                    labels = containerLabels,
                    ipEnabled = false
                )
            )

            logger.info("buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo " +
                            "start container, taskId:($devCloudTaskId)")
            printLogs(this, "下发启动构建机请求成功，containerName: $containerName 等待机器启动...")
            buildContainerPoolNoDao.setDevCloudBuildLastContainer(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1,
                containerName = containerName,
                poolNo = poolNo.toString()
            )

            // redisUtils.setStartingContainer(containerName, dispatchMessage.userId)
            val startResult = dispatchDevCloudClient.waitTaskFinish(
                dispatchMessage.userId,
                dispatchMessage.projectId,
                dispatchMessage.pipelineId,
                devCloudTaskId
            )
            // redisUtils.removeStartingContainer(containerName, dispatchMessage.userId)

            if (startResult.first == TaskStatus.SUCCEEDED) {
                // 启动成功
                val instContainerName = startResult.second
                logger.info("buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo " +
                                "start dev cloud vm success, wait for agent startup...")
                printLogs(this, "构建机启动成功，等待Agent启动...")

                devCloudBuildDao.createOrUpdate(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo,
                    projectId = projectId,
                    containerName = instContainerName,
                    image = this.dispatchMessage,
                    status = ContainerStatus.BUSY.status,
                    userId = userId,
                    cpu = threadLocalCpu.get(),
                    memory = threadLocalMemory.get(),
                    disk = threadLocalDisk.get()
                )
            } else {
                // 暂时注释，统一放在shutdown执行
                /*buildContainerPoolNoDao.deleteDevCloudBuildLastContainerPoolNo(
                    dslContext = dslContext,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    executeCount = executeCount
                )*/
                // 查看当前资源池是否在被其他构建重用中
                val nowBuildRecord = devCloudBuildHisDao.getLatestBuildHistory(dslContext, pipelineId, vmSeqId)
                if (nowBuildRecord?.buidldId == buildId) {
                    // 如果最近的一次构建还是当前buildId，重置资源池状态
                    devCloudBuildDao.updateStatus(
                        dslContext = dslContext,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = poolNo,
                        status = ContainerStatus.IDLE.status
                    )
                }

                onFailure(
                    startResult.third.errorType,
                    startResult.third.errorCode,
                    ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                    "第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 - 构建机启动失败，错误信息:${startResult.second}"
                )
            }
        }
    }

    private fun generateEnvs(dispatchMessage: DispatchMessage): Map<String, Any> {
        // 拼接环境变量
        with(dispatchMessage) {
            val envs = mutableMapOf<String, Any>()
            if (customBuildEnv != null) {
                envs.putAll(customBuildEnv!!)
            }
            envs.putAll(mapOf(
                ENV_KEY_PROJECT_ID to projectId,
                ENV_KEY_AGENT_ID to id,
                ENV_KEY_AGENT_SECRET_KEY to secretKey,
                ENV_KEY_GATEWAY to gateway,
                "TERM" to "xterm-256color",
                SLAVE_ENVIRONMENT to "DevCloud",
                ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.PUBLIC_DEVCLOUD.name)
            ))

            return envs
        }
    }

    private fun getContainerPool(dispatchMessage: DispatchMessage): Pool {
        val containerPool: Pool = try {
            objectMapper.readValue(dispatchMessage.dispatchMessage)
        } catch (e: Exception) {
            // 兼容历史数据
            if (dispatchMessage.dispatchMessage.startsWith(registryHost!!)) {
                Pool(dispatchMessage.dispatchMessage, Credential(registryUser!!, registryPwd!!))
            } else {
                Pool(
                    registryHost + "/" + dispatchMessage.dispatchMessage,
                    Credential(registryUser!!, registryPwd!!)
                )
            }
        }

        if (containerPool.third != null && !containerPool.third!!) {
            val containerPoolFixed = if (containerPool.container!!.startsWith(registryHost!!)) {
                Pool(
                    containerPool.container,
                    Credential(registryUser!!, registryPwd!!),
                    containerPool.performanceConfigId,
                    containerPool.third
                )
            } else {
                Pool(
                    registryHost + "/" + containerPool.container,
                    Credential(registryUser!!, registryPwd!!),
                    containerPool.performanceConfigId,
                    containerPool.third
                )
            }

            return containerPoolFixed
        }

        return containerPool
    }

    fun doShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("do shutdown - ($event)")

        // 有可能出现devcloud返回容器状态running了，但是其实流水线任务早已经执行完了，
        // 导致shutdown消息先收到而redis和db还没有设置的情况，因此扔回队列，sleep等待30秒重新触发
        val containerNameList = buildContainerPoolNoDao.getDevCloudBuildLastContainer(
            dslContext = dslContext,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            executeCount = event.executeCount ?: 1
        )

        if (containerNameList.none { it.second != null } && event.retryTime <= 3) {
            logger.info(
                "[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] shutdown no containerName, " +
                        "sleep 10s and retry ${event.retryTime}. "
            )
            event.retryTime += 1
            event.delayMills = 10000
            pipelineEventDispatcher.dispatch(event)

            return
        }

        containerNameList.filter { it.second != null }.forEach {
            try {
                logger.info("[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                                "stop dev cloud container,vmSeqId: ${it.first}, containerName:${it.second}")
                val taskId = dispatchDevCloudClient.operateContainer(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId ?: "",
                    userId = event.userId,
                    name = it.second!!,
                    action = Action.STOP
                )
                val opResult = dispatchDevCloudClient.waitTaskFinish(
                    event.userId,
                    event.projectId,
                    event.pipelineId,
                    taskId
                )
                if (opResult.first == TaskStatus.SUCCEEDED) {
                    logger.info("[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                                    "stop dev cloud vm success.")
                } else {
                    // TODO 告警通知
                    logger.info("[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                                    "stop dev cloud vm failed, msg: ${opResult.second}")
                }
            } catch (e: Exception) {
                logger.error(
                    "[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                        "stop dev cloud vm failed. containerName: ${it.second}",
                    e
                )
            } finally {
                // 清除job创建记录
                devCloudJobRedisUtils.deleteJobCount(event.buildId, it.second!!)
            }
        }

        val containerPoolList = buildContainerPoolNoDao.getDevCloudBuildLastPoolNo(
            dslContext,
            event.buildId,
            event.vmSeqId,
            event.executeCount ?: 1
        )
        containerPoolList.filter { it.second != null }.forEach {
            logger.info("[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                            "update status in db,vmSeqId: ${it.first}, poolNo:${it.second}")
            devCloudBuildDao.updateStatus(
                dslContext,
                event.pipelineId,
                it.first,
                it.second!!.toInt(),
                ContainerStatus.IDLE.status
            )
        }

        logger.info("[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] delete buildContainerPoolNo.")
        buildContainerPoolNoDao.deleteDevCloudBuildLastContainerPoolNo(
            dslContext,
            event.buildId,
            event.vmSeqId,
            event.executeCount ?: 1
        )
    }

    private fun getIdleContainer(dispatchMessage: DispatchMessage): Triple<String?, Int, Boolean> {
        val lock = PipelineContainerLock(redisOperation, dispatchMessage.pipelineId, dispatchMessage.vmSeqId)
        try {
            lock.lock()
            for (i in 1..buildPoolSize) {
                logger.info("poolNo is $i")
                val containerInfo =
                    devCloudBuildDao.get(dslContext, dispatchMessage.pipelineId, dispatchMessage.vmSeqId, i)

                // 当前流水线构建没有构建池记录，新增构建池记录
                if (null == containerInfo) {
                    devCloudBuildDao.createOrUpdate(
                        dslContext = dslContext,
                        pipelineId = dispatchMessage.pipelineId,
                        vmSeqId = dispatchMessage.vmSeqId,
                        poolNo = i,
                        projectId = dispatchMessage.projectId,
                        containerName = "",
                        image = dispatchMessage.dispatchMessage,
                        status = ContainerStatus.BUSY.status,
                        userId = dispatchMessage.userId,
                        cpu = threadLocalCpu.get(),
                        memory = threadLocalMemory.get(),
                        disk = threadLocalDisk.get()
                    )
                    return Triple(null, i, true)
                }

                // 构件序号被占用，接着在构建池内寻找
                if (containerInfo.status == ContainerStatus.BUSY.status) {
                    continue
                }

                // 分配空闲池
                if (containerInfo.containerName.isEmpty()) {
                    devCloudBuildDao.createOrUpdate(
                        dslContext = dslContext,
                        pipelineId = dispatchMessage.pipelineId,
                        vmSeqId = dispatchMessage.vmSeqId,
                        poolNo = i,
                        projectId = dispatchMessage.projectId,
                        containerName = "",
                        image = dispatchMessage.dispatchMessage,
                        status = ContainerStatus.BUSY.status,
                        userId = dispatchMessage.userId,
                        cpu = threadLocalCpu.get(),
                        memory = threadLocalMemory.get(),
                        disk = threadLocalDisk.get()
                    )
                    return Triple(null, i, true)
                }

                val statusResponse = dispatchDevCloudClient.getContainerStatus(
                    dispatchMessage.projectId,
                    dispatchMessage.pipelineId,
                    dispatchMessage.buildId,
                    dispatchMessage.vmSeqId,
                    dispatchMessage.userId,
                    containerInfo.containerName
                )
                val actionCode = statusResponse.optInt("actionCode")
                val containerStatus = statusResponse.optString("data")

                // 接口异常，接着在构建池内寻找
                if (actionCode != 200) {
                    continue
                }

                // 启用stopped容器
                if ("stopped" == containerStatus || "stop" == containerStatus) {
                    devCloudBuildDao.updateStatus(
                        dslContext,
                        dispatchMessage.pipelineId,
                        dispatchMessage.vmSeqId,
                        i,
                        ContainerStatus.BUSY.status
                    )
                    return Triple(containerInfo.containerName, i, checkContainerChanged(containerInfo, dispatchMessage))
                }

                // 删除池内的异常容器，同时重置池子
                if ("exception" == containerStatus) {
                    clearExceptionContainer(dispatchMessage, containerInfo.containerName)
                    devCloudBuildDao.delete(dslContext, dispatchMessage.pipelineId, dispatchMessage.vmSeqId, i)
                    devCloudBuildDao.createOrUpdate(
                        dslContext = dslContext,
                        pipelineId = dispatchMessage.pipelineId,
                        vmSeqId = dispatchMessage.vmSeqId,
                        poolNo = i,
                        projectId = dispatchMessage.projectId,
                        containerName = "",
                        image = dispatchMessage.dispatchMessage,
                        status = ContainerStatus.BUSY.status,
                        userId = dispatchMessage.userId,
                        cpu = threadLocalCpu.get(),
                        memory = threadLocalMemory.get(),
                        disk = threadLocalDisk.get()
                    )
                    return Triple(null, i, true)
                }
                // continue to find idle container
            }

            // 构建池遍历结束也没有可用构建机，报错
            throw BuildFailureException(
                ErrorCodeEnum.NO_IDLE_VM_ERROR.errorType,
                ErrorCodeEnum.NO_IDLE_VM_ERROR.errorCode,
                ErrorCodeEnum.NO_IDLE_VM_ERROR.formatErrorMessage,
                "DEVCLOUD构建机启动失败，没有空闲的构建机"
            )
        } finally {
            lock.unlock()
        }
    }

    private fun checkContainerChanged(
        containerInfo: TDevcloudBuildRecord,
        dispatchMessage: DispatchMessage
    ): Boolean {
        var containerChanged = false
        // 查看构建性能配置是否变更
        if (threadLocalCpu.get() != containerInfo.cpu ||
            threadLocalDisk.get() != containerInfo.disk ||
            threadLocalMemory.get() != containerInfo.memory) {
            containerChanged = true
            logger.info("buildId: ${dispatchMessage.buildId}, vmSeqId: ${dispatchMessage.vmSeqId}" +
                            " performanceConfig changed.")
        }

        // 镜像是否变更
        if (checkImageChanged(containerInfo.images, dispatchMessage)) {
            containerChanged = true
        }

        return containerChanged
    }

    private fun checkImageChanged(images: String, dispatchMessage: DispatchMessage): Boolean {
        // 镜像是否变更
        val containerPool: Pool = try {
            objectMapper.readValue(dispatchMessage.dispatchMessage)
        } catch (e: Exception) {
            // 兼容历史数据
            if (dispatchMessage.dispatchMessage.startsWith(registryHost!!)) {
                Pool(dispatchMessage.dispatchMessage, Credential(registryUser!!, registryPwd!!))
            } else {
                Pool(
                    registryHost + "/" + dispatchMessage.dispatchMessage,
                    Credential(registryUser!!, registryPwd!!)
                )
            }
        }

        val lastContainerPool: Pool? = try {
            objectMapper.readValue(images)
        } catch (e: Exception) {
            null
        }

        // 兼容旧版本，数据库中存储的非pool结构值
        if (lastContainerPool != null) {
            if (lastContainerPool.container != containerPool.container ||
                lastContainerPool.credential != containerPool.credential) {
                logger.info("buildId: ${dispatchMessage.buildId}, vmSeqId: ${dispatchMessage.vmSeqId} image changed. " +
                                "old image: $lastContainerPool, new image: $containerPool")
                return true
            }
        } else {
            if (containerPool.container != images && dispatchMessage.dispatchMessage != images) {
                logger.info("buildId: ${dispatchMessage.buildId}, vmSeqId: ${dispatchMessage.vmSeqId} image changed. " +
                                "old image: $images, new image: ${dispatchMessage.dispatchMessage}")
                return true
            }
        }

        return false
    }

    private fun clearExceptionContainer(dispatchMessage: DispatchMessage, containerName: String) {
        try {
            // 下发删除，不管成功失败
            logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] Delete container, " +
                            "userId: ${dispatchMessage.userId}, containerName: $containerName")
            dispatchDevCloudClient.operateContainer(
                projectId = dispatchMessage.projectId,
                pipelineId = dispatchMessage.pipelineId,
                buildId = dispatchMessage.buildId,
                vmSeqId = dispatchMessage.vmSeqId,
                userId = dispatchMessage.userId,
                name = containerName,
                action = Action.DELETE
            )
        } catch (e: Exception) {
            logger.error("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] delete container failed", e)
        }
    }

    private fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        try {
            log(
                buildLogPrinter,
                dispatchMessage.buildId,
                dispatchMessage.containerHashId,
                dispatchMessage.vmSeqId,
                message,
                dispatchMessage.executeCount
            )
        } catch (e: Throwable) {
            // 日志有问题就不打日志了，不能影响正常流程
            logger.error("", e)
        }
    }

    private fun generatePwd(): String {
        val secretSeed =
            arrayOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz", "0123456789", "[()~!@#%&-+=_")

        val random = Random()
        val buf = StringBuffer()
        for (i in 0 until 15) {
            val num = random.nextInt(secretSeed[i / 4].length)
            buf.append(secretSeed[i / 4][num])
        }
        return buf.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudBuildListener::class.java)
    }
}
