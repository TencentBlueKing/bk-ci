package com.tencent.devops.store.service.common

import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.pojo.common.BusinessConfigRequest
import com.tencent.devops.store.pojo.common.BusinessConfigResponse
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/12/1
 * @Version 1.0
 */
@Service
class BusinessConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val businessConfigDao: BusinessConfigDao
) {

    fun add(businessConfigRequest: BusinessConfigRequest): Boolean {
        businessConfigDao.add(dslContext, businessConfigRequest)
        return true
    }

    fun update(id: String, businessConfigRequest: BusinessConfigRequest): Int {
        return businessConfigDao.update(dslContext, businessConfigRequest)
    }

    fun listAllBusinessConfigs(): List<BusinessConfigResponse>? {
        return businessConfigDao.listAll(dslContext)?.map {
            BusinessConfigResponse(
                it.id,
                it.business,
                it.feature,
                it.businessValue,
                it.configValue,
                it.description
            )
        }?.toList() ?: emptyList()
    }

    fun getBusinessConfigById(id: Int): BusinessConfigResponse? {
        val record = businessConfigDao.get(dslContext, id)
        return if (record != null) {
            BusinessConfigResponse(
                record.id,
                record.business,
                record.feature,
                record.businessValue,
                record.configValue,
                record.description
            )
        } else {
            null
        }
    }

    fun deleteBusinessConfigById(id: Int): Int {
        return businessConfigDao.delete(dslContext, id)
    }
}