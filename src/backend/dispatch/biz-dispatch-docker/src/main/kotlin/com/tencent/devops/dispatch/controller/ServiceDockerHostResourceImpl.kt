package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.ServiceDockerHostResource
import com.tencent.devops.dispatch.pojo.DockerHostZone
import com.tencent.devops.dispatch.service.DockerHostZoneTaskService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceDockerHostResourceImpl @Autowired constructor(
    private val dockerHostZoneTaskService: DockerHostZoneTaskService
) : ServiceDockerHostResource {
    override fun list(page: Int?, pageSize: Int?): Page<DockerHostZone> {
        checkParams(page, pageSize)
        val realPage = page ?: 1
        val realPageSize = pageSize ?: 20
        val dockerHostList = dockerHostZoneTaskService.list(realPage, realPageSize)
        val count = dockerHostZoneTaskService.count()
        return Page(
            page = realPage,
            pageSize = realPageSize,
            count = count.toLong(),
            records = dockerHostList
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceDockerHostResourceImpl::class.java)
    }

    fun checkParams(page: Int?, pageSize: Int?) {
        if (page != null && page < 1) {
            throw ParamBlankException("Invalid page")
        }
        if (pageSize != null && pageSize < 1) {
            throw ParamBlankException("Invalid pageSize")
        }
    }
}
