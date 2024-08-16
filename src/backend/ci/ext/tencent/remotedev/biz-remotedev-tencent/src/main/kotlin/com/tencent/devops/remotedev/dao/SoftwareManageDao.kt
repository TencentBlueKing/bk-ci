package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TSystemInstalledRecords
import com.tencent.devops.model.remotedev.tables.TSystemSoftwares
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import com.tencent.devops.remotedev.pojo.software.SoftwareInfo
import com.tencent.devops.remotedev.pojo.software.SoftwareInstallStatus
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SoftwareManageDao {

    // 获取系统软件
    fun getSystemSoftwareList(
        dslContext: DSLContext
    ): Result<out Record> {
        with(TSystemSoftwares.T_SYSTEM_SOFTWARES) {
            return dslContext.select(NAME, VERSION).from(this).skipCheck()
                .fetch()
        }
    }
    // 添加软件安装记录
    fun updateSystemInstalledRecords(dslContext: DSLContext, softwareList: SoftwareCallbackRes) {
        val taskId = softwareList.taskId
        val statusList = softwareList.softwareStatusInfo
        statusList.forEach { (t, u) ->
            with(TSystemInstalledRecords.T_SYSTEM_INSTALLED_RECORDS) {
                dslContext.update(this)
                    .set(STATUS, SoftwareInstallStatus.valueOf(u ?: "FAILED").ordinal)
                    .where(TASK_ID.eq(taskId))
                    .and(SOFTWARE_NAME.eq(t))
                    .execute()
            }
        }
    }

    fun batchAddSystemInstalledRecords(
        dslContext: DSLContext,
        tadkId: Long,
        workspaceName: String,
        softwareInfoList: List<SoftwareInfo>
    ) {
        dslContext.batch(
            softwareInfoList.map {
            with(TSystemInstalledRecords.T_SYSTEM_INSTALLED_RECORDS) {
                dslContext.insertInto(
                    this,
                    TASK_ID,
                    WORKSPACE_NAME,
                    SOFTWARE_NAME,
                    STATUS,
                    CREATE_TIME
                ).values(
                    tadkId,
                    workspaceName,
                    it.name,
                    SoftwareInstallStatus.RUNNING.ordinal,
                    LocalDateTime.now()
                ).onDuplicateKeyUpdate()
                    .set(STATUS, SoftwareInstallStatus.RUNNING.ordinal)
            }
        }
        ).execute()
    }
}
