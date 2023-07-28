package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TProjectSoftwares
import com.tencent.devops.model.remotedev.tables.TUserInstalledSoftwares
import com.tencent.devops.model.remotedev.tables.records.TProjectSoftwaresRecord
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
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
    fun installSoftwareToUser(
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
                )
            }
        }).execute()
    }
    fun batchSave(dslContext: DSLContext, softwareList: List<UserSoftware>) {
        with(TUserInstalledSoftwares.T_USER_INSTALLED_SOFTWARES) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                CREATOR,
                SOFTWARE_ID
            ).also { insert ->
                softwareList.forEach { record ->
                    insert.values(
                        record.projectId,
                        record.user,
                        record.softwareId
                    )
                }
            }.onDuplicateKeyUpdate()
                .set(PROJECT_ID, org.jooq.util.mysql.MySQLDSL.values(PROJECT_ID))
                .set(CREATOR, org.jooq.util.mysql.MySQLDSL.values(CREATOR))
                .set(SOFTWARE_ID, org.jooq.util.mysql.MySQLDSL.values(SOFTWARE_ID))
                .execute()
        }
    }

}
