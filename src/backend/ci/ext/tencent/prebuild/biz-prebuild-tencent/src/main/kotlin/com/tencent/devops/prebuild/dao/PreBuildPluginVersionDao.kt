package com.tencent.devops.prebuild.dao

import com.tencent.devops.model.prebuild.tables.TPrebuildPluginVersion
import com.tencent.devops.model.prebuild.tables.records.TPrebuildPluginVersionRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

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
}