package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TPluginGcloudConf
import com.tencent.devops.model.plugin.tables.records.TPluginGcloudConfRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Repository
class GcloudConfDao {

    fun insert(
        dslContext: DSLContext,
        region: String,
        address: String,
        fileAddress: String,
        userId: String,
        remark: String?
    ): Int {

        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            val data = dslContext.insertInto(this,
                    REGION,
                    ADDRESS,
                    ADDRESS_FILE,
                    UPDATE_TIME,
                    USER_ID,
                    REMARK)
                    .values(region,
                            address,
                            fileAddress,
                            LocalDateTime.ofInstant(Date(System.currentTimeMillis()).toInstant(), ZoneId.systemDefault()),
                            userId,
                            remark)
                    .returning(ID)
                    .fetchOne()
            return data.id
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Int,
        region: String,
        address: String,
        fileAddress: String,
        userId: String,
        remark: String
    ): Int {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.update(this)
                    .set(REGION, region)
                    .set(ADDRESS, address)
                    .set(ADDRESS_FILE, fileAddress)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(USER_ID, userId)
                    .set(REMARK, remark)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun getList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TPluginGcloudConfRecord>? {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.selectFrom(this)
                    .orderBy(UPDATE_TIME.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }

    fun getCount(dslContext: DSLContext): Int {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.selectCount().from(this)
                    .fetchOne().get(0) as Int
        }
    }

    fun getRecord(
        dslContext: DSLContext,
        id: Int
    ): TPluginGcloudConfRecord? {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Int
    ): Int {
        with(TPluginGcloudConf.T_PLUGIN_GCLOUD_CONF) {
            return dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }
}

// CREATE TABLE `devops_plugin`.`T_PLUGIN_GCLOUD_CONF`(
// `ID` int(11) NOT NULL AUTO_INCREMENT,
// `REGION` varchar(1024) DEFAULT NULL,
// `ADDRESS` varchar(1024) DEFAULT NULL,
// `UPDATE_TIME` datetime DEFAULT NULL,
// `USER_ID` varchar(64) DEFAULT NULL,
// `REMARK` varchar(1024) DEFAULT NULL,
// PRIMARY KEY (`ID`)
// ) ENGINE=InnoDB DEFAULT CHARSET='utf8' COLLATE='utf8_general_ci';