package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectSoftwares
import com.tencent.devops.model.remotedev.tables.TSystemSoftwares
import com.tencent.devops.model.remotedev.tables.TUserInstalledRecords
import com.tencent.devops.model.remotedev.tables.TUserInstalledSoftwares
import com.tencent.devops.model.remotedev.tables.records.TProjectSoftwaresRecord
import com.tencent.devops.model.remotedev.tables.records.TUserInstalledRecordsRecord
import com.tencent.devops.remotedev.pojo.software.ProjectSoftware
import com.tencent.devops.remotedev.pojo.software.SoftwareInstallStatus
import com.tencent.devops.remotedev.pojo.software.UserSoftware
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
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
                ).onDuplicateKeyIgnore()
            }
        }).execute()
    }

    // 获取用户安装的软件列表
    fun getUserInstalledSoftwareList(
        dslContext: DSLContext,
        user: String
    ): Result<out Record>? {
        val t1 = TUserInstalledSoftwares.T_USER_INSTALLED_SOFTWARES.`as`("t1")
        val t2 = TProjectSoftwares.T_PROJECT_SOFTWARES.`as`("t2")
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.CREATOR.eq(user))
        return dslContext.select(t2.NAME, t2.VERSION)
            .from(t1).leftJoin(t2).on(t1.SOFTWARE_ID.eq(t2.ID))
            .where(conditions)
            .fetch()
    }

    // 获取系统软件
    fun getSystemSoftwareList(
        dslContext: DSLContext
    ): Result<out Record> {
        with(TSystemSoftwares.T_SYSTEM_SOFTWARES) {
            return dslContext.select(NAME, VERSION).from(this)
                .fetch()
        }
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
