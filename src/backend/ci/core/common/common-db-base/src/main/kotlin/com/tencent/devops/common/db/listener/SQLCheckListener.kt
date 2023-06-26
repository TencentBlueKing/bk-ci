package com.tencent.devops.common.db.listener

import org.jooq.ExecuteContext
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListener
import org.slf4j.LoggerFactory


class SQLCheckListener : DefaultExecuteListener() {
    override fun renderEnd(ctx: ExecuteContext) {
        val sql = ctx.sql()!!
        if (sql.contains("EXPLAIN")) {
            return
        }
        check(sql, ctx)
    }

    @SuppressWarnings("NestedBlockDepth", "TooGenericExceptionCaught")
    fun check(sql: String, ctx: ExecuteContext? = null) {
        val checkRegex = "^(?i:(UPDATE|DELETE|SELECT).*)$".toRegex()
//        val noWhereRegex = "(?!.* WHERE ).*".toRegex()
        val noWhereRegex = "(?i:(?!.* WHERE ).*)".toRegex()
        if (sql.matches(checkRegex)) {
            if (sql.matches(noWhereRegex)) {
//                throw DataAccessException("This SQL : $sql must use WHERE")
                logger.warn("This SQL : $sql must use WHERE")
            }
            if (ctx != null) {
                try {
                    val explain = DSL.using(ctx.configuration()).fetch("EXPLAIN $sql")
                    for (record in explain) {
                        val selectType: String = record.getValue("select_type", String::class.java)
                        val type: String = record.getValue("type", String::class.java)
                        val key: String = record.getValue("key", String::class.java)
                        logger.info("SQL: $sql , selectType: $selectType, type: $type, key: $key")
                    }
                } catch (e: Exception) {
                    logger.warn("explain error", e)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SQLCheckListener::class.java)
    }
}
