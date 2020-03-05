package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TStoreProjectRelRecord
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class ExtStoreProjectRelDao {

    /**
     * 获取项目的调试组件
     */
    fun getStoreInstall(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        startTime: Long
    ): Result<TStoreProjectRelRecord>? {
        return with(TStoreProjectRel.T_STORE_PROJECT_REL) {
            val where = dslContext.selectFrom(this).where(
                STORE_TYPE.eq(storeType.type.toByte()).and(STORE_CODE.eq(storeCode).and(TYPE.eq(StoreProjectTypeEnum.COMMON.type.toByte())))
            )
            if (startTime > 0) {
                where.and(
                    CREATE_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            where.orderBy(CREATE_TIME.desc())
                .fetch()
        }

    }
}