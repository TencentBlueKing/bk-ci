package com.tencent.devops.dockerhost.service

import com.tencent.devops.dockerhost.pojo.DockerBuildParamNew
import com.tencent.devops.dockerhost.pojo.Status
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Service
class BuildDockerService @Autowired constructor(
    private val txDockerHostBuildService: TXDockerHostBuildService
) {

    private val executor = Executors.newFixedThreadPool(10)
    private val buildTask = mutableMapOf<String, Future<Pair<Boolean, String?>>>()

    fun buildImage(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, dockerBuildParam: DockerBuildParamNew): Boolean {
        logger.info("projectId: $projectId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId, dockerBuildParam: $dockerBuildParam")

        val future = executor.submit(Callable<Pair<Boolean, String?>> {
            txDockerHostBuildService.dockerBuildAndPushImageNew(projectId, pipelineId, vmSeqId, dockerBuildParam, buildId)
        })

        buildTask[getKey(vmSeqId, buildId)] = future

        return true
    }

    fun getBuildResult(vmSeqId: String, buildId: String): Pair<Status, String> {
        logger.info("vmSeqId: $vmSeqId, buildId: $buildId")
        val status = getStatus(vmSeqId, buildId)
        logger.info("status: $status")
        if (status.first == Status.SUCCESS || status.first == Status.FAILURE) {
            logger.info("Delete the build image task: vmSeqId: $vmSeqId, buildId: $buildId, status: $status")
            buildTask.remove(getKey(vmSeqId, buildId))
        }
        return status
    }

    private fun getStatus(vmSeqId: String, buildId: String): Pair<Status, String> {
        val future = buildTask[getKey(vmSeqId, buildId)]
        return when {
            future == null -> Pair(Status.NO_EXISTS, "")
            future.isDone -> {
                when {
                    future.get().first -> Pair(Status.SUCCESS, "")
                    else -> Pair(Status.FAILURE, future.get().second ?: "")
                }
            }
            else -> Pair(Status.RUNNING, "")
        }
    }

    private fun getKey(vmSeqId: String, buildId: String): String {
        return "$buildId-$vmSeqId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildDockerService::class.java)
    }
}