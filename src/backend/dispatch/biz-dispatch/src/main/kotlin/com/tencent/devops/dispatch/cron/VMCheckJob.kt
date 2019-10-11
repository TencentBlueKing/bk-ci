package com.tencent.devops.dispatch.cron

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.service.VMService
import com.tencent.devops.dispatch.service.vm.QueryVMs
import com.tencent.devops.dispatch.utils.ShutdownVMAfterBuildUtils
import com.tencent.devops.dispatch.utils.VMLock
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.process.api.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * deng
 * 17/01/2018
 */
@Component
class VMCheckJob @Autowired constructor(
    private val client: Client,
    private val redisUtils: RedisUtils,
    private val redisOperation: RedisOperation,
    private val queryVMs: QueryVMs,
    private val vmService: VMService,
    private val vmAfterBuildUtils: ShutdownVMAfterBuildUtils,
    private val gray: Gray
) {

    // 20 minutes
    @Scheduled(initialDelay = 2000*60, fixedDelay = 2000*60)
    fun heartbeatCheck() {
        logger.info("Start to check the idle VM")
        if (gray.isGray()) {
            logger.info("The dispatch is gray dispatch, ignore")
            return
        }
        try {
            val allPowerOnVM = queryVMs.queryAllPowerOnVM()
            if (allPowerOnVM.isEmpty()) {
                return
            }
            allPowerOnVM.forEach {
                val vm = vmService.queryVMByName(it.name) ?: return@forEach
                if (vm.inMaintain) {
                    return@forEach
                }
                val redisLock = VMLock(redisOperation, vm.ip)
                try {
                    if (!redisLock.tryLock()) {
                        logger.info("It fail to lock the redis for the vm ${vm.ip}")
                        return@forEach
                    }
                    val redisBuild = redisUtils.getRedisBuild(vm.ip)
                    if (redisBuild == null) {
                        val vmShutdown = vmAfterBuildUtils.isShutdown(vm.ip)
                        if (vmShutdown.first) {
                            logger.warn("The vm(${vm.ip}) is not exist in the redis, try to stop it")
                            vmService.directShutdownVM(vm.id)
                        } else {
                            logger.warn("The vm(${vm.ip}) is not shutdown after the build(${vmShutdown.second})")
                        }
                    } else {
                        // Check if the pipeline if running
                        try {
                            val channelCode = if (redisBuild.channelCode.isNullOrBlank()) {
                                ChannelCode.BS
                            } else {
                                ChannelCode.valueOf(redisBuild.channelCode!!)
                            }
                            val result = client.get(ServicePipelineResource::class).isPipelineRunning(redisBuild.projectId,
                                    redisBuild.buildId, channelCode)
                            if (result.isNotOk() || result.data == null) {
                                logger.warn("Fail to check if the build(${redisBuild.buildId}) is running because of ${result.message} for the vm(${vm.ip})")
                                return@forEach
                            }
                            if (!result.data!!) {
                                logger.warn("The build(${redisBuild.buildId}) of pipeline(${redisBuild.pipelineId}) and project(${redisBuild.projectId}) is not running, try to shutdown the vm")
                                redisUtils.deleteRedisBuild(vm.ip)
                                vmService.directShutdownVM(vm.id)
                            }
                        } catch (e: Exception) {
                            logger.warn("Fail to check if the build(${redisBuild.buildId}) running", e)
                        }
                    }
                } finally {
                    redisLock.unlock()
                }
            }
        } catch (t: Throwable) {
            logger.warn("Fail to check the idle vm", t)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VMCheckJob::class.java)
    }
}