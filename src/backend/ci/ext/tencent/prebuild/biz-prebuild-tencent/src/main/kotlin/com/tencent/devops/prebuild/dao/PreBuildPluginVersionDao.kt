package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TPrebuildPluginVersion
import com.tencent.devops.model.prebuild.tables.records.TPrebuildPluginVersionRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PreBuildPluginVersionDao {

    fun getVersion(
        pluginType: String,
        dslContext: DSLContext
    ): TPrebuildPluginVersionRecord? {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.selectFrom(this)
                .where(PLUGIN_TYPE.eq(pluginType))
                .fetchAny()
        }
    }

    fun create(
        version: String,
        modifyUser: String,
        desc: String,
        pluginType: String,
        dslContext: DSLContext
    ): Int {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.insertInto(
                this,
                VERSION,
                UPDATE_TIME,
                MODIFY_USER,
                DESC,
                PLUGIN_TYPE
            ).values(
                version,
                LocalDateTime.now(),
                modifyUser,
                desc,
                pluginType
            ).execute()
        }
    }

    fun update(
        version: String,
        modifyUser: String,
        desc: String,
        pluginType: String,
        dslContext: DSLContext
    ): Int {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.update(this)
                .set(VERSION, version)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFY_USER, version)
                .set(DESC, version)
                .where(PLUGIN_TYPE.eq(pluginType))
                .execute()
        }
    }

    fun delete(
        version: String,
        dslContext: DSLContext
    ): Int {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.deleteFrom(this)
                .where(VERSION.eq(version))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext
    ): List<TPrebuildPluginVersionRecord>? {
        with(TPrebuildPluginVersion.T_PREBUILD_PLUGIN_VERSION) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }
}