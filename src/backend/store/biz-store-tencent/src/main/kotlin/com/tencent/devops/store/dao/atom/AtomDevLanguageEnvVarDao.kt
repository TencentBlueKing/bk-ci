package com.tencent.devops.store.dao.atom

import com.tencent.devops.model.atom.tables.TAtomDevLanguageEnvVar
import com.tencent.devops.model.atom.tables.records.TAtomDevLanguageEnvVarRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AtomDevLanguageEnvVarDao {

    fun getAtomEnvVars(
        dslContext: DSLContext,
        language: String,
        buildHostTypeList: List<String>,
        buildHostOsList: List<String>
    ): Result<TAtomDevLanguageEnvVarRecord>? {
        return with(TAtomDevLanguageEnvVar.T_ATOM_DEV_LANGUAGE_ENV_VAR) {
            dslContext.selectFrom(this)
                .where(LANGUAGE.eq(language))
                .and(BUILD_HOST_TYPE.`in`(buildHostTypeList))
                .and(BUILD_HOST_OS.`in`(buildHostOsList))
                .fetch()
        }
    }
}