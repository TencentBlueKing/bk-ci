package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TGrayTest
import com.tencent.devops.project.pojo.service.GrayTestInfo
import org.jooq.DSLContext
import org.jooq.Record1
import org.springframework.stereotype.Repository

@Repository
class GrayTestDao {

    fun getStatus(dslContext: DSLContext, userId: String, serviceId: Long?): Record1<String>? {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.select(STATUS).from(this)
                .where(SERVICE_ID.eq(serviceId))
                .and(USERNAME.eq(userId))
                .fetchOne()
        }
    }

    fun create(dslContext: DSLContext, userId: String, serviceId: Long, status: String): GrayTestInfo {

        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.insertInto(
                this, SERVICE_ID, USERNAME, STATUS
            ).values(serviceId, userId, status).returning()
                .fetchOne().let {
                    GrayTestInfo(
                        it.id, it.serviceId, it.username, it.status
                    )
                }
        }
    }

    fun update(dslContext: DSLContext, userId: String, serviceId: Long, status: String, id: Long) {
        with(TGrayTest.T_GRAY_TEST) {
            dslContext.update(this)
                .set(SERVICE_ID, serviceId)
                .set(USERNAME, userId)
                .set(STATUS, status)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Long) {

        with(TGrayTest.T_GRAY_TEST) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, id: Long): GrayTestInfo {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne {
                    GrayTestInfo(it.id, it.serviceId, it.username, it.status)
                }
        }
    }

    fun listByUser(dslContext: DSLContext, userId: String): List<GrayTestInfo> {
        with(TGrayTest.T_GRAY_TEST) {
            return dslContext.selectFrom(this)
                .where(USERNAME.eq(userId))
                .fetch {
                    GrayTestInfo(it.id, it.serviceId, it.username, it.status)
                }
        }
    }
}