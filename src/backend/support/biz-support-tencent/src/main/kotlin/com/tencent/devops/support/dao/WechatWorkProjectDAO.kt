package com.tencent.devops.support.dao

import com.tencent.devops.model.support.tables.TWechatWorkProject
import com.tencent.devops.model.support.tables.records.TWechatWorkProjectRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * freyzheng
 * 2018/9/25
 */
@Repository
class WechatWorkProjectDAO {
    fun getByGroupId(dslContext: DSLContext, groupId: String): TWechatWorkProjectRecord? {
        with(TWechatWorkProject.T_WECHAT_WORK_PROJECT) {
            return dslContext.selectFrom(this)
                    .where(GROUP_ID.eq(groupId))
                    .fetchOne()
        }
    }

    fun setProjectIdforGroupId(dslContext: DSLContext, projectId: String, groupId: String) {
        with(TWechatWorkProject.T_WECHAT_WORK_PROJECT) {
            val result = if (exist(dslContext, groupId)) {
                // 更新旧的
                dslContext
                        .update(this)
                        .set(PROJECT_ID, projectId)
                        .where(GROUP_ID.eq(groupId))
                        .execute()
            } else {
                // 插入新的
                dslContext
                        .insertInto(this, GROUP_ID, PROJECT_ID)
                        .values(groupId, projectId)
                        .execute()
            }
            System.out.println(result)
        }
    }

    fun exist(dslContext: DSLContext, buildId: String) =
            getByGroupId(dslContext, buildId) != null
}