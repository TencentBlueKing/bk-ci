package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.atom.AtomDevLanguageEnvVarDao
import com.tencent.devops.store.pojo.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.enums.BuildHostOsEnum
import com.tencent.devops.store.pojo.enums.BuildHostTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomDevLanguageEnvVarService @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomDevLanguageEnvVarDao: AtomDevLanguageEnvVarDao
) {
    private val logger = LoggerFactory.getLogger(AtomDevLanguageEnvVarService::class.java)

    /**
     * 获取插件开发语言相关的环境变量
     * @param language 开发语言
     * @param buildHostType 适用构建机类型
     * @param buildHostOs 适用构建机操作系统
     * @return 环境变量列表
     */
    fun getAtomDevLanguageEnvVars(
        language: String,
        buildHostType: BuildHostTypeEnum,
        buildHostOs: BuildHostOsEnum
    ): Result<List<AtomDevLanguageEnvVar>?> {
        logger.info("getAtomDevLanguageEnvVars language is :$language,buildHostType is :$buildHostType,buildHostOs is :$buildHostOs")
        val atomDevLanguageEnvVarList = mutableListOf<AtomDevLanguageEnvVar>()
        val buildHostTypeList = mutableListOf(BuildHostTypeEnum.ALL.name)
        if (buildHostType != BuildHostTypeEnum.ALL) {
            buildHostTypeList.add(buildHostType.name)
        }
        val buildHostOsList = mutableListOf(BuildHostOsEnum.ALL.name)
        if (buildHostOs != BuildHostOsEnum.ALL) {
            buildHostOsList.add(buildHostOs.name)
        }
        val atomLabelRecords = atomDevLanguageEnvVarDao
            .getAtomEnvVars(
                dslContext,
                language,
                buildHostTypeList,
                buildHostOsList
            )
        atomLabelRecords?.forEach {
            atomDevLanguageEnvVarList.add(
                AtomDevLanguageEnvVar(
                    envKey = it.envKey,
                    envValue = it.envValue,
                    language = it.language,
                    buildHostOs = it.buildHostOs,
                    buildHostType = it.buildHostType
                )
            )
        }
        logger.info("getAtomDevLanguageEnvVars atomDevLanguageEnvVarList is :$atomDevLanguageEnvVarList")
        return Result(atomDevLanguageEnvVarList)
    }
}
