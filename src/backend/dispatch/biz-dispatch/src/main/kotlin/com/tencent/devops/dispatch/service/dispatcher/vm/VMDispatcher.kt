package com.tencent.devops.dispatch.service.dispatcher.vm

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.type.exsi.ESXiDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.dispatch.dao.PipelineBuildDao
import com.tencent.devops.dispatch.pojo.PipelineBuildCreate
import com.tencent.devops.dispatch.pojo.VM
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.dispatch.service.PipelineVMService
import com.tencent.devops.dispatch.service.VMService
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.dispatch.utils.ShutdownVMAfterBuildUtils
import com.tencent.devops.dispatch.utils.VMLock
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class VMDispatcher @Autowired constructor(
        private val client: Client,
        private val dslContext: DSLContext,
        private val vmService: VMService,
        private val pipelineBuildDao: PipelineBuildDao,
        private val pipelineVMService: PipelineVMService,
        private val redisOperation: RedisOperation,
        private val rabbitTemplate: RabbitTemplate,
        private val redisUtils: RedisUtils,
        private val pipelineEventDispatcher: PipelineEventDispatcher,
        private val vmAfterBuildUtils: ShutdownVMAfterBuildUtils
) : Dispatcher {
    override fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) =
        pipelineAgentStartupEvent.dispatchType is ESXiDispatchType

    override fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        if (!pipelineBuildDao.exist(dslContext, pipelineAgentStartupEvent.buildId, pipelineAgentStartupEvent.vmSeqId)) {
            pipelineBuildDao.add(
                dslContext, PipelineBuildCreate(
                    pipelineAgentStartupEvent.projectId,
                    pipelineAgentStartupEvent.pipelineId,
                    pipelineAgentStartupEvent.buildId,
                    pipelineAgentStartupEvent.vmSeqId,
                    -1
                )
            )
        }
        // Get the pre vms
        val preVms = pipelineBuildDao.listByPipelineAndVmSeqId(
            dslContext,
            pipelineAgentStartupEvent.pipelineId,
            pipelineAgentStartupEvent.vmSeqId,
            10
        ).map {
            it.vmId
        }.toList()
        val preferVMNames = pipelineVMService.getVMsByPipelines(
            pipelineAgentStartupEvent.pipelineId,
            pipelineAgentStartupEvent.vmSeqId.toInt()
        )
            ?: pipelineAgentStartupEvent.vmNames
        logger.info("Get the prefer vms ($preferVMNames) for the pipeline(${pipelineAgentStartupEvent.pipelineId})")
        val vm =
            vmService.findVM(pipelineAgentStartupEvent.projectId, preferVMNames, pipelineAgentStartupEvent.os, preVms)
        if (vm == null) {
            logger.info("Fail to find the fix vm for the build($pipelineAgentStartupEvent), retry")
            retry(client, rabbitTemplate, pipelineEventDispatcher, pipelineAgentStartupEvent)
            return
        }

        val redisLock = VMLock(redisOperation, vm.ip)

        try {
            if (redisLock.tryLock()) {
                saveVmInfoToBuildDetail(
                    projectId = pipelineAgentStartupEvent.projectId,
                    pipelineId = pipelineAgentStartupEvent.pipelineId,
                    buildId = pipelineAgentStartupEvent.buildId,
                    vmSeqId = pipelineAgentStartupEvent.vmSeqId,
                    vm = vm
                )

                logger.info("Start to start the vm(${vm.id}|${vm.ip}) for the build(${pipelineAgentStartupEvent.buildId})")
                LogUtils.addLine(
                    rabbitTemplate,
                    pipelineAgentStartupEvent.buildId,
                    "Starting the vm(${getVMShotName(vm.name)}) for current build",
                    "",
                    pipelineAgentStartupEvent.executeCount ?: 1
                )
                if (!vmService.startUpVM(
                        pipelineAgentStartupEvent.projectId,
                        vm.id,
                        "${pipelineAgentStartupEvent.pipelineId}_${pipelineAgentStartupEvent.vmSeqId}"
                    )
                ) {
                    logger.error("Fail to start up vm for the build($pipelineAgentStartupEvent), retry")
                    LogUtils.addLine(
                        rabbitTemplate,
                        pipelineAgentStartupEvent.buildId,
                        "Fail to start up vm(${vm.name}) for current build, retry",
                        "",
                        pipelineAgentStartupEvent.executeCount ?: 1
                    )
                    retry(client, rabbitTemplate, pipelineEventDispatcher, pipelineAgentStartupEvent)
                    AlertUtils.doAlert(AlertLevel.HIGH, "DevOps Alert Notify", "Fail to start the vm ${vm.ip}")
                } else {
                    redisUtils.setRedisBuild(
                        vm.ip, RedisBuild(
                            vm.name,
                            pipelineAgentStartupEvent.projectId,
                            pipelineAgentStartupEvent.pipelineId,
                            pipelineAgentStartupEvent.buildId,
                            pipelineAgentStartupEvent.vmSeqId,
                            pipelineAgentStartupEvent.channelCode,
                            pipelineAgentStartupEvent.zone,
                            pipelineAgentStartupEvent.atoms
                        )
                    )
                    pipelineBuildDao.updatePipelineStatus(
                        dslContext,
                        pipelineAgentStartupEvent.buildId,
                        pipelineAgentStartupEvent.vmSeqId,
                        vm.id,
                        PipelineTaskStatus.RUNNING
                    )
                }
            } else {
                logger.warn("Fail to lock the vm(${vm.ip}")
                retry(client, rabbitTemplate, pipelineEventDispatcher, pipelineAgentStartupEvent)
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun saveVmInfoToBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        vm: VM
    ) {
        client.get(ServiceBuildResource::class).saveBuildVmInfo(
            projectId,
            pipelineId,
            buildId,
            vmSeqId,
            VmInfo(vm.ip, vm.ip)
        )
    }

    override fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        val list = if (pipelineAgentShutdownEvent.buildResult) {
            pipelineBuildDao.getPipelineByBuildIdOrNull(
                dslContext,
                pipelineAgentShutdownEvent.buildId,
                pipelineAgentShutdownEvent.vmSeqId
            )
        } else {
            pipelineBuildDao.getPipelineByBuildIdOrNull(dslContext, pipelineAgentShutdownEvent.buildId, null)
        }
        if (list.isEmpty()) {
            logger.warn("The pipeline record is not exist in dispatch db")
            return
        }
        list.forEach {
            val build = pipelineBuildDao.convert(it)
            // Get the vm
            val vm = vmService.queryVMById(build.vmId)
            val redisLock = VMLock(redisOperation, vm.ip)
            redisLock.lock()
            try {
                val redisBuild = redisUtils.getRedisBuild(vm.ip)
                if (redisBuild == null) {
                    logger.info("The ip(${vm.ip}) is not exist in redis, ignore")
                    return@forEach
                }

                if (redisBuild.buildId != pipelineAgentShutdownEvent.buildId) {
                    logger.warn("The redis build id (${redisBuild.buildId}) is not equal to the build finish message one(${pipelineAgentShutdownEvent.buildId})")
                    return@forEach
                }

                logger.info("Shutting down vm(${vm.name}) for build(${pipelineAgentShutdownEvent.buildId})")
                val status = if (pipelineAgentShutdownEvent.buildResult) {
                    if (vmAfterBuildUtils.isShutdown(pipelineAgentShutdownEvent.pipelineId, vm.ip)) {
                        if (!vmService.shutdownVM(
                                vm.id,
                                "${pipelineAgentShutdownEvent.pipelineId}_${pipelineAgentShutdownEvent.vmSeqId}"
                            )
                        ) {
                            if (!vmService.directShutdownVM(vm.id)) {
                                logger.warn("Fail to shutdown the vm directly")
                            }
                        }
                    } else {
                        logger.info("Don't shutdown the vm after the build")
                    }

                    PipelineTaskStatus.DONE
                } else {
                    if (vmAfterBuildUtils.isShutdown(pipelineAgentShutdownEvent.pipelineId, vm.ip)) {
                        if (!vmService.directShutdownVM(vm.id)) {
                            logger.warn("Fail to shutdown the vm directly")
                        }
                    } else {
                        logger.info("Don't shutdown the vm after the build")
                    }
                    PipelineTaskStatus.FAILURE
                }
                redisUtils.deleteRedisBuild(vm.ip)
                pipelineBuildDao.updatePipelineStatus(dslContext, it.id, status)
            } finally {
                redisLock.unlock()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VMDispatcher::class.java)
    }

//    override fun canDispatch(buildMessage: PipelineBuildMessage) = buildMessage.dispatchType.buildType == BuildType.ESXi
//
//    override fun build(buildMessage: PipelineBuildMessage) {
//        if (!pipelineBuildDao.exist(dslContext, buildMessage.buildId, buildMessage.vmSeqId)) {
//            pipelineBuildDao.add(dslContext, PipelineBuildCreate(buildMessage.projectId,
//                    buildMessage.pipelineId, buildMessage.buildId, buildMessage.vmSeqId, -1))
//        }
//        // Get the pre vms
//        val preVms = pipelineBuildDao.listByPipelineAndVmSeqId(dslContext, buildMessage.pipelineId, buildMessage.vmSeqId, 10).map {
//            it.vmId
//        }.toList()
//        val preferVMNames = pipelineVMService.getVMsByPipelines(buildMessage.pipelineId, buildMessage.vmSeqId.toInt()) ?: buildMessage.vmNames
//        logger.info("Get the prefer vms ($preferVMNames) for the pipeline(${buildMessage.pipelineId})")
//        val vm = vmService.findVM(buildMessage.projectId, preferVMNames, buildMessage.os, preVms)
//        if (vm == null) {
//            logger.info("Fail to find the fix vm for the build($buildMessage), retry")
//            retry(rabbitTemplate, buildMessage)
//            return
//        }
//
//        val redisLock = VMLock(redisTemplate, vm.ip)
//
//        try {
//            if (redisLock.tryLock()) {
//                logger.info("Start to start the vm(${vm.id}|${vm.ip}) for the build(${buildMessage.buildId})")
//                LogUtils.addLine(client, buildMessage.buildId, "Starting the vm(${getVMShotName(vm.name)}) for current build", "", buildMessage.executeCount ?: 1)
//                if (!vmService.startUpVM(buildMessage.projectId, vm.id, "${buildMessage.pipelineId}_${buildMessage.vmSeqId}")) {
//                    logger.error("Fail to start up vm for the build($buildMessage), retry")
//                    LogUtils.addLine(client, buildMessage.buildId, "Fail to start up vm(${vm.name}) for current build, retry", "", buildMessage.executeCount ?: 1)
//                    retry(rabbitTemplate, buildMessage)
//                    AlertUtils.doAlert(AlertLevel.HIGH, "DevOps Alert Notify", "Fail to start the vm ${vm.ip}")
//                } else {
//                    /*
//            val notifyResult = client.get(ServiceBuildResource::class).vmStarted(buildMessage.projectId,
//                    buildMessage.pipelineId, buildMessage.buildId, buildMessage.vmSeqId, vm.name)
//            if (notifyResult.isNotOk() || notifyResult.data == null) {
//                logger.warn("Fail to notify process service that the vm(${vm.name}) start up for the build(${buildMessage.buildId}) because of ${notifyResult.message}")
//            }
//            */
//                    redisUtils.setRedisBuild(
//                        vm.ip, RedisBuild(
//                            vm.name,
//                            buildMessage.projectId,
//                            buildMessage.pipelineId,
//                            buildMessage.buildId,
//                            buildMessage.vmSeqId,
//                            buildMessage.channelCode.name,
//                            buildMessage.zone
//                        )
//                    )
//                    pipelineBuildDao.updatePipelineStatus(dslContext, buildMessage.buildId, buildMessage.vmSeqId, vm.id, PipelineTaskStatus.RUNNING)
//                }
//            } else {
//                logger.warn("Fail to lock the vm(${vm.ip}")
//                retry(rabbitTemplate, buildMessage)
//            }
//        } finally {
//            redisLock.unlock()
//        }
//    }
//
//    override fun finish(buildFinishMessage: PipelineFinishMessage) {
//        val list = if (buildFinishMessage.buildResult) {
//            pipelineBuildDao.getPipelineByBuildIdOrNull(dslContext, buildFinishMessage.buildId, buildFinishMessage.vmSeqId)
//        } else {
//            pipelineBuildDao.getPipelineByBuildIdOrNull(dslContext, buildFinishMessage.buildId, null)
//        }
//        if (list.isEmpty()) {
//            logger.warn("The pipeline record is not exist in dispatch db")
//            return
//        }
//        list.forEach {
//            val build = pipelineBuildDao.convert(it)
//            // Get the vm
//            val vm = vmService.queryVMById(build.vmId)
//            val redisLock = VMLock(redisTemplate, vm.ip)
//            redisLock.lock()
//            try {
//                val redisBuild = redisUtils.getRedisBuild(vm.ip)
//                if (redisBuild == null) {
//                    logger.info("The ip(${vm.ip}) is not exist in redis, ignore")
//                    return@forEach
//                }
//
//                if (redisBuild.buildId != buildFinishMessage.buildId) {
//                    logger.warn("The redis build id (${redisBuild.buildId}) is not equal to the build finish message one(${buildFinishMessage.buildId})")
//                    return@forEach
//                }
//
//                logger.info("Shutting down vm(${vm.name}) for build(${buildFinishMessage.buildId})")
//                val status = if (buildFinishMessage.buildResult) {
//                    if (vmAfterBuildUtils.isShutdown(buildFinishMessage.pipelineId, vm.ip)) {
//                        if (!vmService.shutdownVM(vm.id, "${buildFinishMessage.pipelineId}_${buildFinishMessage.vmSeqId}")) {
//                            if (!vmService.directShutdownVM(vm.id)) {
//                                logger.warn("Fail to shutdown the vm directly")
//                            }
//                        }
//                    } else {
//                        logger.info("Don't shutdown the vm after the build")
//                    }
//
//                    PipelineTaskStatus.DONE
//                } else {
//                    if (vmAfterBuildUtils.isShutdown(buildFinishMessage.pipelineId, vm.ip)) {
//                        if (!vmService.directShutdownVM(vm.id)) {
//                            logger.warn("Fail to shutdown the vm directly")
//                        }
//                    } else {
//                        logger.info("Don't shutdown the vm after the build")
//                    }
//                    PipelineTaskStatus.FAILURE
//                }
//                redisUtils.deleteRedisBuild(vm.ip)
//                pipelineBuildDao.updatePipelineStatus(dslContext, it.id, status)
//            } finally {
//                redisLock.unlock()
//            }
//        }
//    }

    private fun getVMShotName(vmName: String): String {
        if (vmName.isEmpty()) {
            return ""
        }
        val list = vmName.split("-")
        if (list.isEmpty()) {
            return vmName
        }

        if (list.size == 1) {
            return list[0]
        }
        return list[list.size - 2] + "-" + list[list.size - 1]
    }
}
