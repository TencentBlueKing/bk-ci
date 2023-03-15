package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceTemplate
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceTemplateRecord
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class WorkspaceTemplateDao {

    // 新增模板
    fun createWorkspaceTemplate(
        userId: String,
        workspaceTemplate: WorkspaceTemplate,
        dslContext: DSLContext
    ) {
        with(TWorkspaceTemplate.T_WORKSPACE_TEMPLATE) {
            dslContext.insertInto(
                this,
                NAME,
                IMAGE,
                SOURCE,
                LOGO,
                URL,
                DESCRIPTION,
                CREATOR
            ).values(
                workspaceTemplate.name,
                workspaceTemplate.image,
                workspaceTemplate.source,
                workspaceTemplate.logo,
                workspaceTemplate.url,
                workspaceTemplate.description,
                userId
            ).execute()
        }
    }

    // 修改模板
    fun updateWorkspaceTemplate(
        wsTemplateId: Long,
        workspaceTemplate: WorkspaceTemplate,
        dslContext: DSLContext
    ) {
        with(TWorkspaceTemplate.T_WORKSPACE_TEMPLATE) {
            dslContext.update(this)
                .set(NAME, workspaceTemplate.name)
                .set(IMAGE, workspaceTemplate.image)
                .set(SOURCE, workspaceTemplate.source)
                .set(LOGO, workspaceTemplate.logo)
                .set(URL, workspaceTemplate.url)
                .set(DESCRIPTION, workspaceTemplate.description)
                .where(ID.eq(wsTemplateId))
                .execute()
        }
    }

    // 删除模板
    fun deleteWorkspaceTemplate(
        wsTemplateId: Long,
        dslContext: DSLContext
    ) {
        with(TWorkspaceTemplate.T_WORKSPACE_TEMPLATE) {
            dslContext.delete(this)
                .where(ID.eq(wsTemplateId))
                .limit(1)
                .execute()
        }
    }

    // 获取模板列表
    fun queryWorkspaceTemplate(
        wsTemplateId: Long?,
        dslContext: DSLContext
    ): Result<TWorkspaceTemplateRecord> {
        return with(TWorkspaceTemplate.T_WORKSPACE_TEMPLATE) {
            val dsl = dslContext.selectFrom(this)
            if (wsTemplateId != null) {
                dsl.where(ID.eq(wsTemplateId))
            }

            dsl.limit(10).fetch()
        }
    }
}
