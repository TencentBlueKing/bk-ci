package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectSoftwares
import com.tencent.devops.model.remotedev.tables.TUserInstalledRecords
import com.tencent.devops.model.remotedev.tables.TUserInstalledSoftwares
import com.tencent.devops.model.remotedev.tables.records.TProjectSoftwaresRecord
import com.tencent.devops.model.remotedev.tables.records.TUserInstalledRecordsRecord
import com.tencent.devops.remotedev.pojo.software.SoftwareInstallStatus
import com.tencent.devops.remotedev.pojo.software.UserSoftware
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.util.mysql.MySQLDSL
import org.springframework.stereotype.Repository

@Repository
class SoftwareManageDao {

    // 新增模板
    fun querySoftwareList(
        projectId: String,
        dslContext: DSLContext
    ): Result<TProjectSoftwaresRecord> {
        return with(TProjectSoftwares.T_PROJECT_SOFTWARES) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    // 安装软件至用户
    fun batchInstallSoftwareToUser(
        dslContext: DSLContext,
        softwareList: List<UserSoftware>
    ) {
        if (softwareList.isEmpty()) {
            return
        }
        dslContext.batch(softwareList.map {
            with(TUserInstalledSoftwares.T_USER_INSTALLED_SOFTWARES) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    CREATOR,
                    SOFTWARE_ID
                ).values(
                    it.projectId,
                    it.user,
                    it.softwareId
                ).onDuplicateKeyUpdate()
                    .set(PROJECT_ID, MySQLDSL.values(PROJECT_ID))
                    .set(CREATOR, MySQLDSL.values(CREATOR))
                    .set(SOFTWARE_ID, MySQLDSL.values(SOFTWARE_ID))
            }
        }).execute()
    }

    // 获取用户的软件安装记录
    fun queryUserSoftwareInstalledRecord(
        dslContext: DSLContext,
        projectId: String,
        user: String?,
        workspaceName: String?,
        status: SoftwareInstallStatus?
    ): Result<TUserInstalledRecordsRecord> {
        with(TUserInstalledRecords.T_USER_INSTALLED_RECORDS) {
            val dsl = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))

            if (!user.isNullOrBlank()) {
                dsl.and(CREATOR.eq(user))
            }
            if (!workspaceName.isNullOrBlank()) {
                dsl.and(WORKSPACE_NAME.eq(workspaceName))
            }
            if (status != null) {
                dsl.and(STATUS.eq(status.ordinal))
            }

            return dsl.fetch()
        }
    }

    fun deleteTest(
        dslContext: DSLContext
    ) {
        with(TUserInstalledSoftwares.T_USER_INSTALLED_SOFTWARES) {
            dslContext.delete(this).execute()
        }
    }
}
