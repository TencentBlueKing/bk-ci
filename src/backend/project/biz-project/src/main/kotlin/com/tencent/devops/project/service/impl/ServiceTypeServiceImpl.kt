package com.tencent.devops.project.service.impl

import com.tencent.devops.project.dao.ServiceTypeDao
import com.tencent.devops.project.pojo.service.ServiceType
import com.tencent.devops.project.pojo.service.ServiceTypeModify
import com.tencent.devops.project.service.ServiceTypeService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceTypeServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceTypeDao: ServiceTypeDao
) : ServiceTypeService {
    override fun get(id: Long): ServiceType {
        return serviceTypeDao.get(dslContext, id)
    }

    override fun createServiceType(userId: String, title: String, weight: Int): ServiceType {
        return serviceTypeDao.create(dslContext, userId, title, weight)
    }

    override fun updateServiceType(userId: String, serviceTypeId: Long, serviceTypeModify: ServiceTypeModify) {
        serviceTypeDao.update(dslContext, userId, serviceTypeId, serviceTypeModify)
    }

    override fun deleteServiceType(serviceTypeId: Long): Boolean {
        return serviceTypeDao.delete(dslContext, serviceTypeId)
    }

    override fun list(): List<ServiceType> {
        return serviceTypeDao.list(dslContext)
    }
}