package com.tencent.devops.common.db.listener

import com.tencent.devops.common.db.utils.SKIP_CHECK
import org.jooq.ExecuteContext
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListener
import org.slf4j.LoggerFactory

class SQLCheckListener : DefaultExecuteListener() {
    override fun renderEnd(ctx: ExecuteContext) {
        val sql = ctx.sql()!!
        if (sql.contains(SKIP_CHECK) || sql.contains("EXPLAIN")) {
            return
        }
        check(sql, ctx)
    }

    @SuppressWarnings("NestedBlockDepth", "ComplexMethod")
    fun check(sql: String, ctx: ExecuteContext? = null): Boolean {
        val checkRegex = "^(?i:(UPDATE|DELETE|SELECT).*)$".toRegex()
        val noWhereRegex = "(?i:(?!.* WHERE ).*)".toRegex()
        if (sql.matches(checkRegex)) {
            if (sql.matches(noWhereRegex)) {
                logger.error("This SQL : $sql must use WHERE")
                return false
            }
            if (ctx?.query() != null) {
                try {
                    val realSQL = ctx.query()!!.getSQL(ParamType.INLINED)
                    val explain =
                        DSL.using(ctx.configuration()).fetch("EXPLAIN $realSQL")
                    for (record in explain) {
                        val rows = record.get("rows")?.toString()
                        val type = record.get("type")?.toString()
                        val key = record.get("key")?.toString()
                        if (type == null && key == null && rows == null) {
                            continue
                        }
                        if (type == null || type.uppercase() == "ALL") {
                            logger.error("SQL: $realSQL , type: $type is not allowed")
                            return false
                        }
                        if (key.isNullOrBlank()) {
                            logger.error("SQL: $realSQL , key can not be blank. Please add table index")
                            return false
                        }
                        if (rows == null || rows.toInt() > 1000000) {
                            logger.error("SQL: $realSQL , too many rows. Please optimization table index")
                            return false
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("explain error", e)
                }
            }
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SQLCheckListener::class.java)
    }
}
