package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpDockerHostZoneResource
import com.tencent.devops.dispatch.pojo.DockerHostZoneWithPage
import com.tencent.devops.dispatch.service.DockerHostZoneTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpDockerhostZoneResourceImpl @Autowired constructor(private val dockerHostZoneTaskService: DockerHostZoneTaskService) : OpDockerHostZoneResource {
    override fun create(hostIp: String, zone: Zone, remark: String?): Result<Boolean> {
        dockerHostZoneTaskService.create(hostIp, zone.toString(), remark)
        return Result(true)
    }

    override fun delete(hostIp: String): Result<Boolean> {
        dockerHostZoneTaskService.delete(hostIp)
        return Result(true)
    }

    override fun list(page: Int, pageSize: Int): Result<DockerHostZoneWithPage> {
        return Result(DockerHostZoneWithPage(dockerHostZoneTaskService.count(), dockerHostZoneTaskService.list(page, pageSize)))
    }

    override fun enable(hostIp: String, enable: Boolean): Result<Boolean> {
        dockerHostZoneTaskService.enable(hostIp, enable)
        return Result(true)
    }
}
