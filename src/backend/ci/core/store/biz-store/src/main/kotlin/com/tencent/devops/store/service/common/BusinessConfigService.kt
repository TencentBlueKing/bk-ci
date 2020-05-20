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
        if (businessConfigDao.existFeatureConfig(dslContext, businessConfigRequest.business, businessConfigRequest.feature, businessConfigRequest.businessValue)) {
            return false
        } else {
            businessConfigDao.add(dslContext, businessConfigRequest)
        }
        return true
    }

    fun update(id: Int, businessConfigRequest: BusinessConfigRequest): Int {
        if (id < 0) {
            return -1
        }
        val record = businessConfigDao.get(dslContext, businessConfigRequest.business.name, businessConfigRequest.feature, businessConfigRequest.businessValue)
        if (record != null && record.id != id) {
            return -1
        } else {
            return businessConfigDao.updateById(dslContext, id, businessConfigRequest)
        }
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