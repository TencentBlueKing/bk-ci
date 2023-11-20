package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TProjectSoftwares
import com.tencent.devops.model.remotedev.tables.TSystemInstalledRecords
import com.tencent.devops.model.remotedev.tables.TSystemSoftwares
import com.tencent.devops.model.remotedev.tables.TUserInstalledRecords
import com.tencent.devops.model.remotedev.tables.TUserInstalledSoftwares
import com.tencent.devops.model.remotedev.tables.records.TProjectSoftwaresRecord
import com.tencent.devops.model.remotedev.tables.records.TUserInstalledRecordsRecord
import com.tencent.devops.remotedev.pojo.software.ProjectSoftware
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import com.tencent.devops.remotedev.pojo.software.SoftwareInfo
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
        user: String,
        projectId: String?
    ): Result<out Record>? {
        val t1 = TUserInstalledSoftwares.T_USER_INSTALLED_SOFTWARES.`as`("t1")
        val t2 = TProjectSoftwares.T_PROJECT_SOFTWARES.`as`("t2")
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.CREATOR.eq(user))
        projectId?.let { conditions.add(t2.PROJECT_ID.eq(projectId)) }
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
            return dslContext.select(NAME, VERSION).from(this).skipCheck()
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
        dslContext.batch(softwareInfoList.map {
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
        }).execute()
    }

    fun batchAddUserInstalledRecords(
        dslContext: DSLContext,
        projectId: String,
        creator: String,
        tadkId: Long,
        workspaceName: String,
        softwareInfoList: List<SoftwareInfo>
    ) {
        dslContext.batch(softwareInfoList.map {
            with(TUserInstalledRecords.T_USER_INSTALLED_RECORDS) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    CREATOR,
                    TASK_ID,
                    WORKSPACE_NAME,
                    SOFTWARE_NAME,
                    STATUS,
                    CREATE_TIME
                ).values(
                    projectId,
                    creator,
                    tadkId,
                    workspaceName,
                    it.name,
                    SoftwareInstallStatus.RUNNING.ordinal,
                    LocalDateTime.now()
                ).onDuplicateKeyUpdate()
                    .set(STATUS, SoftwareInstallStatus.RUNNING.ordinal)
            }
        }).execute()
    }

    fun updateUserInstalledRecords(dslContext: DSLContext, softwareList: SoftwareCallbackRes) {
        val taskId = softwareList.taskId
        val statusList = softwareList.softwareStatusInfo
        statusList.forEach { (t, u) ->
            with(TUserInstalledRecords.T_USER_INSTALLED_RECORDS) {
                dslContext.update(this)
                    .set(STATUS, SoftwareInstallStatus.valueOf(u ?: "FAILED").ordinal)
                    .where(TASK_ID.eq(taskId))
                    .and(SOFTWARE_NAME.eq(t))
                    .execute()
            }
        }
    }
}
