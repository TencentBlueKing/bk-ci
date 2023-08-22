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
                        val possibleKeys = record.get("possible_keys")?.toString()
                        if (possibleKeys.isNullOrBlank() && rows != null) {
                            logger.error("SQL: $realSQL , possible_keys can not be blank. Please add table index")
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
