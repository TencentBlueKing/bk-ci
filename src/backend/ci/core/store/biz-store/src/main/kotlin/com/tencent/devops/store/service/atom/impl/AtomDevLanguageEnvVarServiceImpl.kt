/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.atom.AtomDevLanguageEnvVarDao
import com.tencent.devops.store.pojo.atom.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.common.enums.BuildHostOsEnum
import com.tencent.devops.store.pojo.common.enums.BuildHostTypeEnum
import com.tencent.devops.store.service.atom.AtomDevLanguageEnvVarService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomDevLanguageEnvVarServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomDevLanguageEnvVarDao: AtomDevLanguageEnvVarDao
) : AtomDevLanguageEnvVarService {
    private val logger = LoggerFactory.getLogger(AtomDevLanguageEnvVarServiceImpl::class.java)

    /**
     * 获取插件开发语言相关的环境变量
     * @param language 开发语言
     * @param buildHostType 适用构建机类型
     * @param buildHostOs 适用构建机操作系统
     * @return 环境变量列表
     */
    override fun getAtomDevLanguageEnvVars(
        language: String,
        buildHostType: BuildHostTypeEnum,
        buildHostOs: BuildHostOsEnum
    ): Result<List<AtomDevLanguageEnvVar>?> {
        val atomDevLanguageEnvVarList = mutableListOf<AtomDevLanguageEnvVar>()
        val buildHostTypeList = mutableListOf(BuildHostTypeEnum.ALL.name)
        if (buildHostType != BuildHostTypeEnum.ALL) {
            buildHostTypeList.add(buildHostType.name)
        }
        val buildHostOsList = mutableListOf(BuildHostOsEnum.ALL.name)
        if (buildHostOs != BuildHostOsEnum.ALL) {
            buildHostOsList.add(buildHostOs.name)
        }
        val atomLabelRecords = atomDevLanguageEnvVarDao.getAtomEnvVars(
            dslContext = dslContext,
            language = language,
            buildHostTypeList = buildHostTypeList,
            buildHostOsList = buildHostOsList
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
        logger.info("getAtomDevLanguageEnvVars language=$language|atomDevLanguageEnvVarList=$atomDevLanguageEnvVarList")
        return Result(atomDevLanguageEnvVarList)
    }
}
