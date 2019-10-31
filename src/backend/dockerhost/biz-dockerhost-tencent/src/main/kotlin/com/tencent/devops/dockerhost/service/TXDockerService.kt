package com.tencent.devops.dockerhost.service

import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.Status
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.pojo.DockerLogsResponse
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Service
class TXDockerService @Autowired constructor(
    private val txDockerHostBuildService: TXDockerHostBuildService,
    private val dockerHostBuildService: DockerHostBuildService
) {

    private val executor = Executors.newFixedThreadPool(10)
    private val buildTask = mutableMapOf<String, Future<Pair<Boolean, String?>>>()

    fun buildImage(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, elementId: String?, dockerBuildParam: DockerBuildParam): Boolean {
        logger.info("projectId: $projectId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId, dockerBuildParam: $dockerBuildParam")

        val future = executor.submit(Callable<Pair<Boolean, String?>> {
            dockerHostBuildService.dockerBuildAndPushImage(projectId, pipelineId, vmSeqId, dockerBuildParam, buildId, elementId)
        })

        buildTask[getKey(vmSeqId, buildId)] = future

        return true
    }

    fun getBuildResult(vmSeqId: String, buildId: String): Pair<Status, String?> {
        logger.info("vmSeqId: $vmSeqId, buildId: $buildId")
        val status = getStatus(vmSeqId, buildId)
        logger.info("status: $status")
        if (status.first == Status.SUCCESS || status.first == Status.FAILURE) {
            logger.info("Delete the build image task: vmSeqId: $vmSeqId, buildId: $buildId, status: $status")
            buildTask.remove(getKey(vmSeqId, buildId))
        }
        return status
    }

    fun dockerRun(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, dockerRunParam: DockerRunParam): DockerRunResponse {
        logger.info("projectId: $projectId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId, dockerRunParam: $dockerRunParam")

        val (containerId, timeStamp) = txDockerHostBuildService.dockerRun(projectId, pipelineId, vmSeqId, buildId, dockerRunParam)
        return DockerRunResponse(containerId, timeStamp)
    }

    fun dockerStop(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, containerId: String) {
        logger.info("projectId: $projectId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId, containerId: $containerId")
        txDockerHostBuildService.dockerStop(projectId, pipelineId, vmSeqId, buildId, containerId)
    }

    fun getDockerRunLogs(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, containerId: String, logStartTimeStamp: Int): DockerLogsResponse {
        val isRunning = dockerHostBuildService.isContainerRunning(containerId)
        val exitCode = when {
            !isRunning -> txDockerHostBuildService.getDockerRunExitCode(containerId)
            else -> null
        }
        val logs = txDockerHostBuildService.getDockerLogs(containerId, logStartTimeStamp)
        return DockerLogsResponse(isRunning, exitCode, logs)
    }

    private fun getStatus(vmSeqId: String, buildId: String): Pair<Status, String?> {
        val future = buildTask[getKey(vmSeqId, buildId)]
        return when {
            future == null -> Pair(Status.NO_EXISTS, null)
            future.isDone -> {
                when {
                    future.get().first -> Pair(Status.SUCCESS, null)
                    else -> Pair(Status.FAILURE, future.get().second)
                }
            }
            else -> Pair(Status.RUNNING, null)
        }
    }

    private fun getKey(vmSeqId: String, buildId: String): String {
        return "$buildId-$vmSeqId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TXDockerService::class.java)
    }
}

//
// fun main(argv: Array<String>) {
//    val buildTask = mutableMapOf<String, Future<Pair<Boolean, String?>>>()
//    val executor = Executors.newFixedThreadPool(10)
//
//    val future1 = executor.submit(Callable<Pair<Boolean, String?>> {
//        Thread.sleep(20*1000)
//        Pair(true, null)
//    })
//
//    buildTask["1"] = future1
//
//    val future2 = executor.submit(Callable<Pair<Boolean, String?>> {
//        Thread.sleep(5*1000)
//        Pair(false, "error msg")
//    })
//
//    buildTask["2"] = future2
//
//    Thread.sleep(5*1000)
//
//    println(buildTask["2"]!!.get().first)
//    println(buildTask["2"]!!.get().second)
//    if(buildTask["1"]!!.isDone) {
//        println(buildTask["1"]!!.get().first)
//        println(buildTask["1"]!!.get().second)
//    }
// }