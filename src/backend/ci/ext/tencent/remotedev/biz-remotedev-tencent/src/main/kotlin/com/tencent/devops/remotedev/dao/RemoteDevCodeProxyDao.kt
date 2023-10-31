package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TRemoteCodeProxy
import com.tencent.devops.model.remotedev.tables.records.TRemoteCodeProxyRecord
import com.tencent.devops.remotedev.pojo.gitproxy.CodeProxyConf
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository

@Repository
class RemoteDevCodeProxyDao {
    fun addCodeProxy(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        type: String,
        url: String,
        conf: CodeProxyConf,
        desc: String?,
        creator: String
    ) {
        with(TRemoteCodeProxy.T_REMOTE_CODE_PROXY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                TYPE,
                URL,
                CONF,
                DESC,
                CREATOR
            ).values(
                projectId,
                name,
                type,
                url,
                JSON.json(JsonUtil.toJson(conf, false)),
                desc,
                creator
            ).execute()
        }
    }

    fun countFetchCodeProxy(
        dslContext: DSLContext,
        projectId: String,
        type: String?,
    ): Int {
        with(TRemoteCodeProxy.T_REMOTE_CODE_PROXY) {
            val sql = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (type != null) {
                sql.and(TYPE.eq(type))
            }
            return dslContext.fetchCount(sql)
        }
    }

    fun fetchCodeProxy(
        dslContext: DSLContext,
        projectId: String,
        type: String?,
        limit: SQLLimit
    ): List<TRemoteCodeProxyRecord> {
        with(TRemoteCodeProxy.T_REMOTE_CODE_PROXY) {
            val sql = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
            if (type != null) {
                sql.and(TYPE.eq(type))
            }
            sql.limit(limit.limit).offset(limit.offset)
            return sql.fetch()
        }
    }
}