package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TProjectImages
import com.tencent.devops.model.remotedev.tables.records.TProjectImagesRecord
import com.tencent.devops.remotedev.pojo.image.ImageStatus
import com.tencent.devops.remotedev.pojo.image.WorkspaceImageInfo
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ImageManageDao {

    /**
     * 查询项目下镜像模板信息
     */
    fun queryImageList(
        projectId: String,
        dslContext: DSLContext
    ): Result<TProjectImagesRecord> {
        return with(TProjectImages.T_PROJECT_IMAGES) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(STATUS.notEqual(ImageStatus.DELETED.ordinal))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    /**
     * 新增项目镜像
     */
    fun createWorkspaceImage(
        projectId: String,
        imageId: String,
        imageName: String,
        userId: String,
        imageStatus: ImageStatus,
        dslContext: DSLContext
    ) {
        with(TProjectImages.T_PROJECT_IMAGES) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                IMAGE_ID,
                IMAGE_NAME,
                CREATOR,
                STATUS
            ).values(
                projectId,
                imageId,
                imageName,
                userId,
                imageStatus.ordinal
            ).execute()
        }
    }

    /**
     * 更新项目镜像
     */
    fun updateWorkspaceImage(
        projectId: String,
        workspaceImageInfo: WorkspaceImageInfo,
        imageStatus: ImageStatus,
        dslContext: DSLContext
    ) {
        with(TProjectImages.T_PROJECT_IMAGES) {
            dslContext.update(this)
                .set(IMAGE_COS_FILE, workspaceImageInfo.imageCosFile)
                .set(SOURCE_CGS_ID, workspaceImageInfo.sourceCgsId)
                .set(SOURCE_CGS_TYPE, workspaceImageInfo.sourceCgsType)
                .set(SOURCE_CGS_ZONE, workspaceImageInfo.sourceCgsZone)
                .set(SIZE, workspaceImageInfo.size)
                .set(STATUS, imageStatus.ordinal)
                .where(PROJECT_ID.eq(projectId))
                .and(IMAGE_ID.eq(workspaceImageInfo.imageId))
                .execute()
        }
    }

    fun deleteWorkspaceImage(
        projectId: String,
        imageId: String,
        dslContext: DSLContext
    ) {
        with(TProjectImages.T_PROJECT_IMAGES) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IMAGE_ID.eq(imageId))
                .execute()
        }
    }

    fun updateWorkspaceImageStatus(
        projectId: String,
        imageId: String,
        imageStatus: ImageStatus,
        dslContext: DSLContext
    ) {
        with(TProjectImages.T_PROJECT_IMAGES) {
            dslContext.update(this)
                .set(STATUS, imageStatus.ordinal)
                .where(PROJECT_ID.eq(projectId))
                .and(IMAGE_ID.eq(imageId))
                .execute()
        }
    }

    fun updateImageName(
        dslContext: DSLContext,
        id: Long,
        imageName: String
    ) {
        with(TProjectImages.T_PROJECT_IMAGES) {
            dslContext.update(this)
                .set(IMAGE_NAME, imageName)
                .where(ID.eq(id))
                .execute()
        }
    }
}
