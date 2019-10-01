package com.tencent.devops.project.service.UserProjectService

import com.tencent.devops.project.pojo.service.ServiceType

/**
 * @author eltons,  Date on 2018-12-05.
 */
interface ServiceTypeService {
    fun createServiceType(userId: String, title: String): ServiceType
    fun updateServiceType(userId: String, serviceTypeId: Long, newName: String)
    fun deleteServiceType(serviceTypeId: Long): Boolean
    fun list(): List<ServiceType>
    fun get(id: Long): ServiceType
}