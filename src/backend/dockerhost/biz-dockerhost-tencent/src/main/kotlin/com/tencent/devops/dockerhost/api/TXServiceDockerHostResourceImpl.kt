package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.pojo.DockerLogsResponse
import com.tencent.devops.dockerhost.service.TXDockerService
import com.tencent.devops.dockerhost.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletRequest

@RestResource
class TXServiceDockerHostResourceImpl @Autowired constructor(
    private val dockerService: TXDockerService
) : TXServiceDockerHostResource {

    override fun dockerRun(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, dockerRunParam: DockerRunParam, request: HttpServletRequest): Result<DockerRunResponse> {
        checkReq(request)
        logger.info("Enter ServiceDockerHostResourceImpl.dockerRun...")
        return Result(dockerService.dockerRun(projectId, pipelineId, vmSeqId, buildId, dockerRunParam))
    }

    override fun getDockerRunLogs(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, containerId: String, logStartTimeStamp: Int, request: HttpServletRequest): Result<DockerLogsResponse> {
        checkReq(request)
        logger.info("Enter ServiceDockerHostResourceImpl.dockerRun...")
        return Result(dockerService.getDockerRunLogs(projectId, pipelineId, vmSeqId, buildId, containerId, logStartTimeStamp))
    }

    override fun dockerStop(projectId: String, pipelineId: String, vmSeqId: String, buildId: String, containerId: String, request: HttpServletRequest): Result<Boolean> {
        checkReq(request)
        logger.info("Enter ServiceDockerHostResourceImpl.dockerStop...")
        dockerService.dockerStop(projectId, pipelineId, vmSeqId, buildId, containerId)
        return Result(true)
    }

    private fun checkReq(request: HttpServletRequest) {
        var ip = request.getHeader("x-forwarded-for")
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        if (ip != null && (CommonUtils.getInnerIP() == ip || ip.startsWith("172.32"))) { // 只允许从本机调用
            logger.info("Request from $ip")
        } else {
            logger.info("Request from $ip")
            logger.info("Local ip :${CommonUtils.getInnerIP()}")
            throw PermissionForbiddenException("不允许的操作！")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TXServiceDockerHostResourceImpl::class.java)
    }
}