package com.tencent.devops.common.db.listener

import org.jooq.ExecuteContext
import org.jooq.conf.ParamType
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
        val noWhereRegex = "(?i:(?!.* WHERE ).*)".toRegex()
        if (sql.matches(checkRegex)) {
            if (sql.matches(noWhereRegex)) {
                logger.error("This SQL : $sql must use WHERE")
            }
            if (ctx?.query() != null) {
                try {
                    val realSQL = ctx.query()!!.getSQL(ParamType.INLINED)
                    val explain =
                        DSL.using(ctx.configuration()).fetch("EXPLAIN $realSQL")
                    for (record in explain) {
                        val rows: String = record.getValue("rows", String::class.java)
                        val type: String = record.getValue("type", String::class.java)
                        val key: String = record.getValue("key", String::class.java)
                        if (type.uppercase() == "ALL") {
                            logger.error("SQL: $realSQL , type: $type is not allowed")
                        }
                        if (key.isBlank()) {
                            logger.error("SQL: $realSQL , key can not be blank. Please add table index")
                        }
                        if (rows.toInt() > 1000000) {
                            logger.error("SQL: $realSQL , too many rows. Please optimization table index")
                        }
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
