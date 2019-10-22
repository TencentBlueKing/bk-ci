package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.service.ServiceType
import com.tencent.devops.project.pojo.service.ServiceTypeModify

interface ServiceTypeService {
    fun createServiceType(userId: String, title: String, weight: Int): ServiceType
    fun updateServiceType(userId: String, serviceTypeId: Long, serviceTypeModify: ServiceTypeModify)
    fun deleteServiceType(serviceTypeId: Long): Boolean
    fun list(): List<ServiceType>
    fun get(id: Long): ServiceType
}