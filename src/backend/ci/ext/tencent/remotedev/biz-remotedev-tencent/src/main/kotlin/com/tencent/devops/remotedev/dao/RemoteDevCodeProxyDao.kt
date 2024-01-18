package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TRemoteCodeProxy
import com.tencent.devops.model.remotedev.tables.records.TRemoteCodeProxyRecord
import com.tencent.devops.remotedev.pojo.gitproxy.CodeProxyConf
import com.tencent.devops.remotedev.pojo.gitproxy.RefreshCodeProxyData
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
        creator: String,
        enableLfs: Boolean
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
                CREATOR,
                ENABLE_LFS
            ).values(
                projectId,
                name,
                type,
                url,
                JSON.json(JsonUtil.toJson(conf, false)),
                desc,
                creator,
                enableLfs
            ).execute()
        }
    }

    fun countFetchCodeProxy(
        dslContext: DSLContext,
        projectId: String,
        type: String?
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

    fun fetchSingleCodeProxy(
        dslContext: DSLContext,
        projectId: String,
        repoName: String
    ): TRemoteCodeProxyRecord? {
        with(TRemoteCodeProxy.T_REMOTE_CODE_PROXY) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(NAME.eq(repoName))
                .orderBy(ID.desc())
                .fetchAny()
        }
    }

    fun deleteCodeProxy(
        dslContext: DSLContext,
        projectId: String,
        repoName: String
    ) {
        with(TRemoteCodeProxy.T_REMOTE_CODE_PROXY) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(NAME.eq(repoName))
                .execute()
        }
    }

    fun batchAddProxy(
        dslContext: DSLContext,
        data: List<RefreshCodeProxyData>
    ) {
        dslContext.batch(
            data.map {
                with(TRemoteCodeProxy.T_REMOTE_CODE_PROXY) {
                    dslContext.insertInto(
                        this,
                        PROJECT_ID,
                        NAME,
                        TYPE,
                        URL,
                        CONF,
                        DESC,
                        CREATOR,
                        ENABLE_LFS
                    ).values(
                        it.projectId,
                        it.name,
                        it.type,
                        it.url,
                        JSON.json(JsonUtil.toJson(it.conf, false)),
                        it.desc,
                        it.creator,
                        it.enableLfs
                    )
                }
            }
        ).execute()
    }

    fun checkExistRepo(
        dslContext: DSLContext,
        projectId: String,
        repoName: String
    ): Boolean {
        with(TRemoteCodeProxy.T_REMOTE_CODE_PROXY) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId)).and(NAME.eq(name))
                .fetchOne(0, Int::class.java)!! > 0
        }
    }
}
