package com.tencent.devops.dispatch.codecc.listener

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.codecc.utils.PipelineContainerLock
import com.tencent.devops.dispatch.codecc.client.CodeccDevCloudClient
import com.tencent.devops.dispatch.codecc.client.DockerHostClient
import com.tencent.devops.dispatch.codecc.common.Constants.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dispatch.codecc.common.ErrorCodeEnum
import com.tencent.devops.dispatch.codecc.dao.BuildContainerPoolNoDao
import com.tencent.devops.dispatch.codecc.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.codecc.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.codecc.exception.DockerServiceException
import com.tencent.devops.dispatch.codecc.pojo.PipelineTaskStatus
import com.tencent.devops.dispatch.codecc.pojo.codecc.CodeccDispatchMessage
import com.tencent.devops.dispatch.codecc.pojo.devcloud.Action
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ContainerStatus
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ContainerType
import com.tencent.devops.dispatch.codecc.pojo.devcloud.DevCloudContainer
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ENV_KEY_PARAM_CODE
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ENV_KEY_PARAM_VALUE
import com.tencent.devops.dispatch.codecc.pojo.devcloud.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.codecc.pojo.devcloud.Params
import com.tencent.devops.dispatch.codecc.pojo.devcloud.Registry
import com.tencent.devops.dispatch.codecc.pojo.devcloud.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.codecc.pojo.devcloud.TaskStatus
import com.tencent.devops.dispatch.codecc.service.DockerHostBuildService
import com.tencent.devops.dispatch.codecc.utils.CommonUtils
import com.tencent.devops.dispatch.codecc.utils.DockerHostUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Random

@Component
class CodeCCDispatchListener @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dockerHostClient: DockerHostClient,
    private val dockerHostUtils: DockerHostUtils,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val dockerHostBuildService: DockerHostBuildService,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val codeccDevCloudClient: CodeccDevCloudClient,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val buildLogPrinter: BuildLogPrinter,
    private val dslContext: DSLContext
) : BuildListener {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeCCDispatchListener::class.java)
    }

    @Value("\${devCloud.cpu}")
    val cpu: Int = 8

    @Value("\${devCloud.memory}")
    val memory: String = "16384M"

    @Value("\${devCloud.disk}")
    val disk: String = "20G"

    @Value("\${devCloud.volume}")
    val volume: Int = 100

    @Value("\${devCloud.regionId}")
    val regionId: String = ""

    @Value("\${devCloud.clusterType}")
    val clusterType: String = ""

    @Value("\${devCloud.closeSourceClusterType}")
    val closeSourceClusterType: String = ""

    @Value("\${devCloud.entrypoint}")
    val entrypoint: String = "devcloud_init.sh"

    @Value("\${registry.host}")
    val registryHost: String = ""

    @Value("\${registry.userName}")
    val registryUserName: String = ""

    @Value("\${registry.password}")
    val registryPassword: String = ""

    private val buildPoolSize = 100 // 单个流水线可同时执行的任务数量

    override fun getShutdownQueue(): String {
        return ".codecc.scan"
    }

    override fun getStartupDemoteQueue(): String {
        return ".codecc.scan.demote"
    }

    override fun getStartupQueue(): String {
        return ".codecc.scan"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.OTHER
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("On shutdown - ($event|$)")
        val buildHistory = devCloudBuildHisDao.get(dslContext, event.buildId)
        // 判断是否有devcloud构建记录，不存在则默认为本地构建集群
        if (buildHistory != null) {
            // redisUtils.deleteDockerBuild(buildHistory.id, buildHistory.secretKey)

            val containerNameList = getContainerNameList(event)
            containerNameList.filter { it.second != null }.forEach {
                try {
                    logger.info("stop dev cloud container,vmSeqId: ${it.first}, containerName:${it.second}")
                    val taskId = codeccDevCloudClient.operateContainer(
                        codeccTaskId = buildHistory.codeccTaskId,
                        staffName = event.userId,
                        name = it.second!!,
                        action = Action.STOP
                    )
                    val opResult = codeccDevCloudClient.waitTaskFinish(buildHistory.codeccTaskId, event.buildId, taskId)
                    if (opResult.first == TaskStatus.SUCCEEDED) {
                        logger.info("stop dev cloud vm success.")
                    } else {
                        // TODO 告警通知
                        logger.info("stop dev cloud vm failed, msg: ${opResult.second}")
                    }
                } catch (e: Exception) {
                    logger.error("stop dev cloud vm failed. containerName: ${it.second}", e)
                }
            }

            val containerPoolList = buildContainerPoolNoDao.getDevCloudBuildLastPoolNo(dslContext, event.buildId, event.vmSeqId)
            containerPoolList.filter { it.second != null }.forEach {
                logger.info("Update status in db,vmSeqId: ${it.first}, containerName:${it.second}")
                devCloudBuildDao.updateStatus(dslContext, event.pipelineId, it.first, it.second!!.toInt(), ContainerStatus.IDLE.status)
            }
            buildContainerPoolNoDao.deleteDevCloudBuildLastContainerPoolNo(dslContext, event.buildId, event.vmSeqId)
        }

        dockerHostBuildService.finishDockerBuild(event)
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("CodeCC dispatcher startUp dispatchMessage: $dispatchMessage")

        val codeccDispatchMessage = JsonUtil.to(dispatchMessage.dispatchMessage, CodeccDispatchMessage::class.java)

        // 判断是否为devcloud构建
        if (codeccDispatchMessage.codeccTaskId == -101L || codeccDispatchMessage.codeccTaskId == -3L) {
            printLogs(dispatchMessage, "准备构建机...")
            createOrStartContainer(dispatchMessage)
            return
        }

        var poolNo = 0
        try {
            // 先判断是否OP已配置专机，若配置了专机，看当前ip是否在专机列表中，若在 选择当前IP并检查负载，若不在从专机列表中选择一个容量最小的
            val specialIpSet = pipelineDockerHostDao.getHostIps(dslContext, dispatchMessage.projectId).toSet()
            logger.info("${dispatchMessage.projectId}| specialIpSet: $specialIpSet")

            val taskHistory = pipelineDockerTaskSimpleDao.getByPipelineIdAndVMSeq(
                dslContext = dslContext,
                pipelineId = dispatchMessage.pipelineId,
                vmSeq = dispatchMessage.vmSeqId
            )

            var driftIpInfo = ""
            val dockerPair: Pair<String, Int>
            poolNo = dockerHostUtils.getIdlePoolNo(dispatchMessage.pipelineId, dispatchMessage.vmSeqId)
            if (taskHistory != null) {
                val dockerIpInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, taskHistory.dockerIp)
                if (dockerIpInfo == null) {
                    dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                        dispatchMessage = dispatchMessage,
                        specialIpSet = specialIpSet
                    )
                } else {
                    driftIpInfo = JsonUtil.toJson(dockerIpInfo.intoMap())

                    dockerPair = if (specialIpSet.isNotEmpty() && specialIpSet.toString() != "[]") {
                        // 该项目工程配置了专机
                        if (specialIpSet.contains(taskHistory.dockerIp) && dockerIpInfo.enable) {
                            // 上一次构建IP在专机列表中，直接重用
                            Pair(taskHistory.dockerIp, dockerIpInfo.dockerHostPort)
                        } else {
                            // 不在专机列表中，重新依据专机列表去选择负载最小的
                            driftIpInfo = "专机漂移"
                            dockerHostUtils.getAvailableDockerIpWithSpecialIps(dispatchMessage, specialIpSet)
                        }
                    } else {
                        // 没有配置专机，根据当前IP负载选择IP
                        val triple = dockerHostUtils.checkAndSetIP(dispatchMessage, specialIpSet, dockerIpInfo, poolNo)
                        if (triple.third.isNotEmpty()) {
                            driftIpInfo = triple.third
                        }
                        Pair(triple.first, triple.second)
                    }
                }
            } else {
                // 第一次构建，根据负载条件选择可用IP
                dockerPair = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                    dispatchMessage = dispatchMessage,
                    specialIpSet = specialIpSet
                )
                pipelineDockerTaskSimpleDao.create(
                    dslContext = dslContext,
                    pipelineId = dispatchMessage.pipelineId,
                    vmSeq = dispatchMessage.vmSeqId,
                    idcIp = dockerPair.first
                )
            }

            // 选择IP后，增加缓存计数，限流用
            // redisOperation.increment("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}${dockerPair.first}", 1)

            dockerHostClient.startBuild(dispatchMessage, dockerPair.first, dockerPair.second, poolNo, driftIpInfo)
        } catch (e: Exception) {
            val errMsg = if (e is DockerServiceException) {
                logger.warn("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] Start build Docker VM failed. ${e.message}")
                e.message!!
            } else {
                logger.error(
                    "[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] Start build Docker VM failed.",
                    e
                )
                "Start build Docker VM failed."
            }

            // 更新构建记录状态
            val result = pipelineDockerBuildDao.updateStatus(
                dslContext,
                dispatchMessage.buildId,
                dispatchMessage.vmSeqId.toInt(),
                PipelineTaskStatus.FAILURE
            )

            if (!result) {
                pipelineDockerBuildDao.startBuild(
                    dslContext = dslContext,
                    projectId = dispatchMessage.projectId,
                    pipelineId = dispatchMessage.pipelineId,
                    buildId = dispatchMessage.buildId,
                    vmSeqId = dispatchMessage.vmSeqId.toInt(),
                    secretKey = dispatchMessage.secretKey,
                    status = PipelineTaskStatus.FAILURE,
                    zone = Zone.SHENZHEN.name,
                    dockerIp = "",
                    poolNo = poolNo
                )
            }


            onFailure(
                errorType = ErrorType.SYSTEM,
                errorCode = 2127001,
                formatErrorMessage = "",
                message = errMsg
            )
        }
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        onStartup(dispatchMessage)
    }

    private fun createOrStartContainer(dispatchMessage: DispatchMessage) {
        try {
            val codeccInfo = JsonUtil.to(dispatchMessage.dispatchMessage, CodeccDispatchMessage::class.java)

            // 记录构建历史
            devCloudBuildHisDao.create(
                dslContext = dslContext,
                pipelineId = dispatchMessage.pipelineId,
                buildId = dispatchMessage.buildId,
                vmSeqId = dispatchMessage.vmSeqId,
                secretKey = dispatchMessage.secretKey,
                codeccTaskId = codeccInfo.codeccTaskId
            )

            val (lastIdleContainer, poolNo, lastImage) = getIdleContainer(
                dispatchMessage.userId,
                dispatchMessage.pipelineId,
                dispatchMessage.vmSeqId,
                codeccInfo.codeccTaskId
            )
            if (null == lastIdleContainer || lastImage != dispatchMessage.dispatchMessage) { // 用户第一次构建，或者用户更换了镜像，则重新创建容器
                logger.info("create new container, poolNo: $poolNo")
                createNewContainer(dispatchMessage, codeccInfo, poolNo)
            } else { // 否则，使用已有容器，start起来即可
                logger.info("start idle container, containerName: $lastIdleContainer")
                startContainer(poolNo, codeccInfo, lastIdleContainer, dispatchMessage)
            }
        } catch (e: Exception) {
            logger.error(e.toString())
            onFailure(ErrorType.SYSTEM, ErrorCodeEnum.SYSTEM_ERROR.errorCode, ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage, "创建构建机失败，错误信息:${e.message}")
        }
    }

    private fun createNewContainer(
        dispatchMessage: DispatchMessage,
        codeccInfo: CodeccDispatchMessage,
        poolNo: Int
    ) {
        // 解析镜像
        val image: String
        val imageHost: String
        val userName: String
        val password: String
        val codeList = mutableListOf<String>()
        val valueList = mutableListOf<String>()
        for (closedSourceParam in codeccInfo.closedSourceParams!!) {
            codeList.add(closedSourceParam.paramCode)
            valueList.add(closedSourceParam.paramValue)
        }
        logger.info("buildId: ${dispatchMessage.buildId} | output of codeList is $codeList and valueList is $valueList")
        if (codeccInfo.image != null) {
            val (host, name, tag) = CommonUtils.parseImage(codeccInfo.image!!)
            image = "$name:$tag"
            imageHost = host
            userName = codeccInfo.userName ?: ""
            password = codeccInfo.password ?: ""

        } else {
            image = "ci/tlinux3_ci:2.3.0"
            imageHost = "mirrors.tencent.com"
            userName = ""
            password = ""
        }

        val envMap = mutableMapOf(
            ENV_KEY_PROJECT_ID to dispatchMessage.projectId,
            ENV_KEY_AGENT_ID to dispatchMessage.id,
            ENV_KEY_AGENT_SECRET_KEY to dispatchMessage.secretKey,
            ENV_KEY_GATEWAY to dispatchMessage.gateway,
            "TERM" to "xterm-256color",
            SLAVE_ENVIRONMENT to "DevCloud",
            ENV_JOB_BUILD_TYPE to BuildType.PUBLIC_DEVCLOUD.name
        )
        if (codeccInfo.codeccTaskId == -3L) {
            envMap[ENV_KEY_PARAM_CODE] = codeList.toString()
            envMap[ENV_KEY_PARAM_VALUE] = valueList.toString()
        }
        val params = Params(
            envMap.toMap(), listOf("/bin/sh", entrypoint)
        )
        logger.info("buildId: ${dispatchMessage.buildId} | poolNo : $poolNo  | hashId : ${dispatchMessage.id}" +
                "the codecc build envparam is $params")

        val devCloudTaskId = codeccDevCloudClient.createContainer(
            codeccInfo.codeccTaskId,
            dispatchMessage.userId,
            DevCloudContainer(
                regionId,
                if (codeccInfo.codeccTaskId == -3L) closeSourceClusterType else clusterType,
                "brief",
                dispatchMessage.buildId,
                ContainerType.DEV.getValue(),
                image,
                Registry(
                    imageHost,
                    userName,
                    password
                ),
                cpu,
                memory,
                disk,
                1,
                emptyList(),
                generatePwd(),
                params
            )
        )
        logger.info("createContainer, taskId:($devCloudTaskId)")
        printLogs(dispatchMessage, "下发创建构建机请求成功，等待机器启动...")
        val createResult = codeccDevCloudClient.waitTaskFinish(
            codeccTaskId = codeccInfo.codeccTaskId,
            userId = dispatchMessage.userId,
            taskId = devCloudTaskId
        )
        if (createResult.first == TaskStatus.SUCCEEDED) {
            // 得到本次任务的实例的信息
            val containerName = createResult.second
            // 启动成功
            logger.info("start dev cloud vm success, wait for agent startup...")
            printLogs(dispatchMessage, "构建机启动成功，等待Agent启动...")

            buildContainerPoolNoDao.setDevCloudBuildLastContainer(dslContext, dispatchMessage.buildId, dispatchMessage.vmSeqId, containerName, poolNo.toString())
            devCloudBuildDao.createOrUpdate(
                dslContext,
                dispatchMessage.pipelineId,
                dispatchMessage.vmSeqId,
                poolNo,
                dispatchMessage.projectId,
                containerName,
                dispatchMessage.dispatchMessage,
                ContainerStatus.BUSY.status,
                dispatchMessage.userId)
            devCloudBuildHisDao.updateContainerName(dslContext, dispatchMessage.buildId, dispatchMessage.vmSeqId, createResult.second)
        } else {
            // 创建失败，记录日志，告警，通知
            logger.error("create dev cloud vm failed, msg: ${createResult.second}")
            devCloudBuildDao.updateStatus(
                dslContext = dslContext,
                pipelineId = dispatchMessage.pipelineId,
                vmSeqId = dispatchMessage.vmSeqId,
                poolNo = poolNo,
                status = ContainerStatus.IDLE.status
            )
            devCloudBuildHisDao.updateContainerName(dslContext, dispatchMessage.buildId, dispatchMessage.vmSeqId, createResult.second)
            onFailure(ErrorType.THIRD_PARTY, ErrorCodeEnum.CREATE_VM_ERROR.errorCode, ErrorCodeEnum.CREATE_VM_ERROR.formatErrorMessage, "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 构建机创建失败:${createResult.second}")

        }
    }

    private fun startContainer(
        poolNo: Int,
        codeccInfo: CodeccDispatchMessage,
        containerName: String,
        dispatchMessage: DispatchMessage
    ) {
        val devCloudTaskId = codeccDevCloudClient.operateContainer(
            codeccInfo.codeccTaskId,
            dispatchMessage.userId,
            containerName,
            Action.START,
            Params(
                mapOf(
                    ENV_KEY_PROJECT_ID to dispatchMessage.projectId,
                    ENV_KEY_AGENT_ID to dispatchMessage.id,
                    ENV_KEY_AGENT_SECRET_KEY to dispatchMessage.secretKey,
                    ENV_KEY_GATEWAY to dispatchMessage.gateway,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "DevCloud",
                    ENV_JOB_BUILD_TYPE to BuildType.PUBLIC_DEVCLOUD.name
                ), listOf("/bin/sh", entrypoint)
            )
        )

        logger.info("start container, taskId:($devCloudTaskId)")
        printLogs(dispatchMessage, "下发启动构建机请求成功，等待机器启动...")
        buildContainerPoolNoDao.setDevCloudBuildLastContainer(
            dslContext = dslContext,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            containerName = containerName,
            poolNo = poolNo.toString()
        )
        val startResult = codeccDevCloudClient.waitTaskFinish(codeccInfo.codeccTaskId, dispatchMessage.userId, devCloudTaskId)
        if (startResult.first == TaskStatus.SUCCEEDED) {
            // 得到本次任务的实例的信息
            val instContainerName = startResult.second
/*            val containerInstanceInfo = codeccDevCloudClient.getContainerInstance(dispatchMessage.userId, instContainerName)
            val actionCode = containerInstanceInfo.optInt("actionCode")
            if (actionCode != 200) {
                val actionMessage = containerInstanceInfo.optString("actionMessage")
                logger.error("Get container instance failed, msg: $actionMessage")
                buildContainerPoolNoDao.deleteDevCloudBuildLastContainerPoolNo(dslContext, dispatchMessage.buildId, dispatchMessage.vmSeqId)
                onFailure(ErrorType.THIRD_PARTY, ErrorCodeEnum.START_VM_ERROR.errorCode, ErrorCodeEnum.START_VM_ERROR.formatErrorMessage, "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 构建机启动失败，错误信息：$actionMessage")
            }*/
            // 启动成功
            logger.info("start dev cloud vm success, wait for agent startup...")
            printLogs(dispatchMessage, "构建机启动成功，等待Agent启动...")

            devCloudBuildDao.createOrUpdate(
                dslContext,
                dispatchMessage.pipelineId,
                dispatchMessage.vmSeqId,
                poolNo,
                dispatchMessage.projectId,
                instContainerName,
                dispatchMessage.dispatchMessage,
                ContainerStatus.BUSY.status,
                dispatchMessage.userId)

            devCloudBuildHisDao.updateContainerName(dslContext, dispatchMessage.buildId, dispatchMessage.vmSeqId, instContainerName)
        } else {
            // 创建失败，记录日志，告警，通知
            logger.error("create dev cloud vm failed, msg: ${startResult.second}")
            buildContainerPoolNoDao.deleteDevCloudBuildLastContainerPoolNo(dslContext, dispatchMessage.buildId, dispatchMessage.vmSeqId)
            onFailure(ErrorType.THIRD_PARTY, ErrorCodeEnum.START_VM_ERROR.errorCode, ErrorCodeEnum.START_VM_ERROR.formatErrorMessage, "第三方服务-DEVCLOUD 异常，请联系8006排查，异常信息 - 构建机启动失败，错误信息:${startResult.second}")
        }
    }

    // 有可能出现devcloud返回容器状态running了，但是其实流水线任务早已经执行完了，导致shutdown消息先收到而redis和db还没有设置的情况，因此sleep等待30秒
    private fun getContainerNameList(event: PipelineAgentShutdownEvent): List<Pair<String, String?>> {
        var containerNameList = buildContainerPoolNoDao.getDevCloudBuildLastContainer(dslContext, event.buildId, event.vmSeqId)
        var times = 0
        while (containerNameList.none { it.second != null }) {
            if (times > 5) {
                break
            }
            Thread.sleep(5 * 1000)
            containerNameList = buildContainerPoolNoDao.getDevCloudBuildLastContainer(dslContext, event.buildId, event.vmSeqId)
            times++
        }
        return containerNameList
    }

    private fun getIdleContainer(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        codeccTaskId: Long
    ): Triple<String?, Int, String?> {
        val lock = PipelineContainerLock(redisOperation, pipelineId, vmSeqId)
        try {
            lock.lock()
            for (i in 1..buildPoolSize) {
                logger.info("poolNo is $i")
                val containerInfo = devCloudBuildDao.get(dslContext, pipelineId, vmSeqId, i)
                if (null == containerInfo) {
                    return Triple(null, i, null)
                } else {
                    if (containerInfo.status == ContainerStatus.BUSY.status) {
                        continue
                    }
                    val statusResponse = codeccDevCloudClient.getContainerStatus(codeccTaskId, userId, containerInfo.containerName)
                    val actionCode = statusResponse.optInt("actionCode")
                    if (actionCode == 200) {
                        if ("stopped" == statusResponse.optString("data") || "stop" == statusResponse.optString("data")) {
                            devCloudBuildDao.updateStatus(dslContext, pipelineId, vmSeqId, i, ContainerStatus.BUSY.status)
                            return Triple(containerInfo.containerName, i, containerInfo.images)
                        }
                        if ("exception" == statusResponse.optString("data")) {
                            clearExceptionContainer(codeccTaskId, userId, containerInfo.containerName)
                            devCloudBuildDao.delete(dslContext, pipelineId, vmSeqId, i)
                            return Triple(null, i, null)
                        }
                    }
                    // continue to find idle container
                }
            }
            throw BuildFailureException(ErrorType.SYSTEM, ErrorCodeEnum.NO_IDLE_VM_ERROR.errorCode, ErrorCodeEnum.NO_IDLE_VM_ERROR.formatErrorMessage, "DEVCLOUD构建机启动失败，没有空闲的构建机")
        } finally {
            lock.unlock()
        }
    }

    private fun clearExceptionContainer(codeccTaskId: Long, userId: String, containerName: String) {
        try {
            // 下发删除，不管成功失败
            codeccDevCloudClient.operateContainer(codeccTaskId, userId, containerName, Action.DELETE)
        } catch (e: Exception) {
            logger.error("delete container failed", e)
        }
    }

    private fun generatePwd(): String {
        val secretSeed = arrayOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz", "0123456789", "[()~!@#%&-+=_")

        val random = Random()
        val buf = StringBuffer()
        for (i in 0 until 15) {
            val num = random.nextInt(secretSeed[i / 4].length)
            buf.append(secretSeed[i / 4][num])
        }
        return buf.toString()
    }

    private fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        try {
            log(buildLogPrinter, dispatchMessage.buildId, dispatchMessage.containerHashId, dispatchMessage.vmSeqId, message, dispatchMessage.executeCount)
        } catch (e: Throwable) {
            // 日志有问题就不打日志了，不能影响正常流程
            logger.error("", e)
        }
    }
}
