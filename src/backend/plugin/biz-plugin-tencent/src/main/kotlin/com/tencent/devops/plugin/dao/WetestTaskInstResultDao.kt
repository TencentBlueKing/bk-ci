package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TPluginWetestInstResult
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WetestTaskInstResultDao {

    fun insert(
        dslContext: DSLContext,
        testId: String,
        callbackjson: String
    ): Int {
        with(TPluginWetestInstResult.T_PLUGIN_WETEST_INST_RESULT) {
            return dslContext.insertInto(this,
                    TEST_ID,
                    RESULT,
                    FINISH_TIME)
                    .values(
                            testId,
                            callbackjson,
                            LocalDateTime.now())
                    .onDuplicateKeyUpdate()
                    .set(RESULT, callbackjson)
                    .set(FINISH_TIME, LocalDateTime.now())
                    .execute()
        }
    }
}