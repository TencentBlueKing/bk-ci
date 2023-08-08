package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectImages
import com.tencent.devops.model.remotedev.tables.records.TProjectImagesRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ImageManageDao {

    // 新增模板
    fun queryImageList(
        projectId: String,
        dslContext: DSLContext
    ): Result<TProjectImagesRecord> {
        return with(TProjectImages.T_PROJECT_IMAGES) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }
}
