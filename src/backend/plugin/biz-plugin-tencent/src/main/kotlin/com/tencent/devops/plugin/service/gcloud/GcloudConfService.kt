package com.tencent.devops.plugin.service.gcloud

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.plugin.tables.TPluginGcloudConf
import com.tencent.devops.plugin.dao.GcloudConfDao
import com.tencent.devops.plugin.pojo.GcloudConf
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GcloudConfService @Autowired constructor(
    private val gcloudConfDao: GcloudConfDao,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GcloudConfService::class.java)
    }

    fun createGcloudConf(region: String, address: String, fileAddress: String, userId: String, remark: String?): Int {
        logger.info("craete gcloud conf: region:$region, address: $address, userId: $userId, remark: $remark")
        return gcloudConfDao.insert(dslContext, region, address, fileAddress, userId, remark)
    }

    fun updateGcloudConf(id: Int, region: String, address: String, fileAddress: String, userId: String, remark: String?): Int {
        logger.info("update gcloud conf: id: $id, region:$region, address: $address, userId: $userId, remark: $remark")
        return gcloudConfDao.update(dslContext, id, region, address, fileAddress, userId, remark ?: "")
    }

    fun getGcloudConf(id: Int): GcloudConf? {
        val record = gcloudConfDao.getRecord(dslContext, id)
        if (null != record) {
            with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
                return GcloudConf(
                        record.id.toString(),
                        record.region,
                        record.address,
                        record.addressFile,
                        record.updateTime.timestampmilli(),
                        record.userId,
                        record.remark
                )
            }
        }
        return null
    }

    fun deleteGcloudConf(id: Int): Int {
        logger.info("delete gcloud conf: id: $id")
        return gcloudConfDao.delete(dslContext, id)
    }

    fun getList(page: Int, pageSize: Int): List<GcloudConf> {
        val recordList = gcloudConfDao.getList(dslContext, page, pageSize)
        val result = mutableListOf<GcloudConf>()
        if (recordList != null) {
            with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
                for (item in recordList) {
                    result.add(
                            GcloudConf(
                                    id = item.get(ID).toString(),
                                    region = item.get(REGION),
                                    address = item.get(ADDRESS),
                                    fileAddress = item.get(ADDRESS_FILE),
                                    updateTime = item.get(UPDATE_TIME).timestamp(),
                                    userId = item.get(USER_ID),
                                    remark = item.get(REMARK)
                            )
                    )
                }
            }
        }
        return result
    }

    fun getCount(): Int {
        return gcloudConfDao.getCount(dslContext)
    }
}
