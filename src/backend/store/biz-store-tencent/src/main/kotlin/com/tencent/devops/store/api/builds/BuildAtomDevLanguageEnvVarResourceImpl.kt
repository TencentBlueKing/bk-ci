package com.tencent.devops.store.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.pojo.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.enums.BuildHostOsEnum
import com.tencent.devops.store.pojo.enums.BuildHostTypeEnum
import com.tencent.devops.store.service.atom.AtomDevLanguageEnvVarService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildAtomDevLanguageEnvVarResourceImpl @Autowired constructor(
    private val atomDevLanguageEnvVarService: AtomDevLanguageEnvVarService
) : BuildAtomDevLanguageEnvVarResource {

    override fun getAtomDevLanguageEnvVars(
        language: String,
        buildHostType: BuildHostTypeEnum,
        buildHostOs: BuildHostOsEnum
    ): Result<List<AtomDevLanguageEnvVar>?> {
        return atomDevLanguageEnvVarService.getAtomDevLanguageEnvVars(language, buildHostType, buildHostOs)
    }
}