package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectSoftwares
import com.tencent.devops.model.remotedev.tables.TUserInstalledRecords
import com.tencent.devops.model.remotedev.tables.TUserInstalledSoftwares
import com.tencent.devops.model.remotedev.tables.records.TProjectSoftwaresRecord
import com.tencent.devops.model.remotedev.tables.records.TUserInstalledRecordsRecord
import com.tencent.devops.remotedev.pojo.software.ProjectSoftware
import com.tencent.devops.remotedev.pojo.software.SoftwareInstallStatus
import com.tencent.devops.remotedev.pojo.software.UserSoftware
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.Condition
import org.jooq.util.mysql.MySQLDSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

    // 导入软件到项目中
    fun importSoftwareToProject(
        dslContext: DSLContext,
        software: ProjectSoftware
    ): Int {
       return with(TProjectSoftwares.T_PROJECT_SOFTWARES) {
            dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    LOGO,
                    NAME,
                    VERSION,
                    SOURCE,
                    STATUS,
                    CLASSIFICATION,
                    INSTALL_METHOD,
                    CREATOR,
                    CREATE_TIME
                ).values(
                    software.projectId,
                    software.logo,
                    software.name,
                    software.version,
                    software.source,
                    software.status,
                    software.classification,
                    software.installMethod,
                    software.creator,
                    LocalDateTime.now()
                ).execute()
        }
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
            val condition = mutableListOf<Condition>()
            condition.add(PROJECT_ID.eq(projectId))

            user?.let {
                condition.add(CREATOR.eq(user))
            }
            workspaceName?.let {
                condition.add(WORKSPACE_NAME.eq(workspaceName))
            }
            status?.let {
                condition.add(STATUS.eq(status.ordinal))
            }

            return dslContext.selectFrom(this)
                .where(condition)
                .fetch()
        }
    }
}
