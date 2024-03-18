package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class RemoteDevDao {
    fun fetchProjectEnableRemoteDev(
        dslContext: DSLContext,
        englishName: String?
    ): List<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(CHANNEL.eq(ProjectChannelCode.BS.name))
                .and(PROPERTIES.like("%\"remotedev\":true%")).fetch()
        }
    }
}
