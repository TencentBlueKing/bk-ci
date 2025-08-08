/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.dialect.IPipelineDialect
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.process.dao.PipelineSettingDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineAsCodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineSettingDao: PipelineSettingDao,
    private val projectCacheService: ProjectCacheService
) {

    fun asCodeEnabled(
        projectId: String,
        pipelineId: String
    ): Boolean? {
        return getPipelineAsCodeSettings(projectId, pipelineId)?.enable
    }

    fun getPipelineAsCodeSettings(projectId: String, pipelineId: String): PipelineAsCodeSettings? {
        val settings = pipelineSettingDao.getPipelineAsCodeSettings(
            dslContext = dslContext, projectId = projectId, pipelineId = pipelineId
        )
        return getPipelineAsCodeSettings(projectId = projectId, asCodeSettings = settings)
    }

    /**
     * 前端或者构建机请求时,构造PipelineAsCodeSettings,保证dialect一定有值
     *
     * 1. 如果asCodeSettings为空,则使用项目方言
     * 2. 如果继承项目方言,则查询项目方言
     */
    fun getPipelineAsCodeSettings(
        projectId: String,
        asCodeSettings: PipelineAsCodeSettings?
    ): PipelineAsCodeSettings? {
        return when {
            asCodeSettings == null -> {
                val projectDialect =
                    projectCacheService.getProjectDialect(projectId) ?: PipelineDialectType.CLASSIC.name
                PipelineAsCodeSettings(inheritedDialect = true, projectDialect = projectDialect)
            }
            asCodeSettings.inheritedDialect != false -> {
                val projectDialect =
                    projectCacheService.getProjectDialect(projectId) ?: PipelineDialectType.CLASSIC.name
                asCodeSettings.copy(projectDialect = projectDialect)
            }
            else ->
                asCodeSettings
        }
    }

    /**
     * 获取项目级方言
     */
    fun getProjectDialect(projectId: String): IPipelineDialect {
        val projectDialect =
            projectCacheService.getProjectDialect(projectId) ?: PipelineDialectType.CLASSIC.name
        return PipelineDialectType.valueOf(projectDialect).dialect
    }

    fun getPipelineDialect(projectId: String, pipelineId: String): IPipelineDialect {
        val asCodeSettings = getPipelineAsCodeSettings(projectId = projectId, pipelineId = pipelineId)
        return getPipelineDialect(projectId = projectId, asCodeSettings = asCodeSettings)
    }

    /**
     * 获取流水线方言,根据流水线设置
     */
    fun getPipelineDialect(projectId: String, asCodeSettings: PipelineAsCodeSettings?): IPipelineDialect {
        return PipelineDialectUtil.getPipelineDialect(
            getPipelineAsCodeSettings(
                projectId = projectId,
                asCodeSettings = asCodeSettings
            )
        )
    }

    /**
     * 获取流水线方言,根据流水线设置或者方言设置
     */
    fun getPipelineDialect(
        projectId: String,
        asCodeSettings: PipelineAsCodeSettings?,
        inheritedDialectSetting: Boolean?,
        pipelineDialectSetting: String?
    ): IPipelineDialect {
        val projectDialect = projectCacheService.getProjectDialect(projectId = projectId)
        return if (asCodeSettings != null) {
            PipelineDialectUtil.getPipelineDialect(
                asCodeSettings.copy(projectDialect = projectDialect)
            )
        } else {
            PipelineDialectUtil.getPipelineDialect(
                inheritedDialect = inheritedDialectSetting,
                projectDialect = projectDialect,
                pipelineDialect = pipelineDialectSetting
            )
        }
    }
}
