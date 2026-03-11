package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TTrustDevice
import com.tencent.devops.model.remotedev.tables.records.TTrustDeviceRecord
import com.tencent.devops.remotedev.pojo.TrustDeviceInfoDetail
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TrustDeviceDao {
    fun addOrUpdateDevice(
        dslContext: DSLContext,
        userId: String,
        deviceId: String,
        token: String,
        detail: TrustDeviceInfoDetail
    ) {
        with(TTrustDevice.T_TRUST_DEVICE) {
            dslContext.insertInto(this, USER_ID, DEVICE_ID, TOKEN, DEVICE_INFO)
                .values(
                    userId,
                    deviceId,
                    token,
                    JSON.json(JsonUtil.toJson(detail, false))
                )
                .onDuplicateKeyUpdate()
                .set(TOKEN, token)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(DEVICE_INFO, JSON.json(JsonUtil.toJson(detail, false)))
                .execute()
        }
    }

    fun fetchAny(dslContext: DSLContext, userId: String, deviceId: String): TTrustDeviceRecord? {
        with(TTrustDevice.T_TRUST_DEVICE) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .and(DEVICE_ID.eq(deviceId))
                .fetchAny()
        }
    }

    fun fetchList(dslContext: DSLContext, userId: String): List<TTrustDeviceRecord> {
        with(TTrustDevice.T_TRUST_DEVICE) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .fetch()
        }
    }

    fun delete(dslContext: DSLContext, userId: String, deviceId: String) {
        with(TTrustDevice.T_TRUST_DEVICE) {
            dslContext.deleteFrom(this)
                .where(USER_ID.eq(userId))
                .and(DEVICE_ID.eq(deviceId))
                .execute()
        }
    }
}