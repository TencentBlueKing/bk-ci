package com.tencent.devops.project.service.UserProjectService

import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.service.OPPServiceVO
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceListVO
import com.tencent.devops.project.pojo.service.ServiceUpdateUrls
import com.tencent.devops.project.pojo.service.ServiceVO

interface UserProjectServiceService {
    fun listOPService(userId: String): Result<List<OPPServiceVO>>
    fun listService(userId: String, projectId: String?): Result<ArrayList<ServiceListVO>>
    fun updateCollected(userId: String, service_id: Long, collector: Boolean): Result<Boolean>
    fun createService(userId: String, serviceCreateInfo: ServiceCreateInfo): Result<OPPServiceVO>
    fun deleteService(userId: String, serviceId: Long): Result<Boolean>
    fun updateService(userId: String, serviceId: Long, serviceCreateInfo: ServiceCreateInfo): Result<Boolean>
    fun getService(userId: String, serviceId: Long): Result<ServiceVO>
    fun syncService(userId: String, services: List<ServiceListVO>)
    fun updateServiceUrls(userId: String, name: String, serviceUpdateUrls: ServiceUpdateUrls): Result<Boolean>
}