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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.misc.service

import org.jooq.DSLContext
import org.jooq.Query
import org.springframework.beans.factory.annotation.Autowired

class TxPipelineHistoryDataClearServiceImpl @Autowired constructor() : PipelineHistoryDataClearService() {

    private val pluginJingangTableKey = "pluginJingangTable"
    private val pluginJingangResultTableKey = "pluginJingangResultTable"
    private val artifacetoryInfoTableKey = "artifacetoryInfoTable"

    override fun getDataBaseInfo(): Map<String, String> {
        return mapOf(
                projectDbKey to "devops_project",
                processDbKey to "devops_process",
                repositoryDbKey to "devops_repository",
                dispatchDbKey to "devops_dispatch",
                pluginDbKey to "devops_plugin",
                qualityDbKey to "devops_quality",
                artifactoryDbKey to "devops_artifactory"
        )
    }

    override fun getSpecTableInfo(): Map<String, String> {
        return mapOf(
                projectTableKey to "t_project",
                pluginJingangTableKey to "T_PLUGIN_JINGANG",
                pluginJingangResultTableKey to "T_PLUGIN_JINGANG_RESULT",
                artifacetoryInfoTableKey to "T_TIPELINE_ARTIFACETORY_INFO"
        )
    }

    override fun getSpecClearSqlList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<Query> {
        val dataBaseInfo = getDataBaseInfo()
        val tableInfo = getTableInfo()
        val pluginDbName = dataBaseInfo[pluginDbKey]
        val artifactoryDbName = dataBaseInfo[artifactoryDbKey]
        return listOf(
                dslContext.query("DELETE FROM $pluginDbName.${tableInfo[pluginJingangTableKey]} WHERE BUILD_ID='$buildId'"),
                dslContext.query("DELETE FROM $pluginDbName.${tableInfo[pluginJingangResultTableKey]} WHERE BUILD_ID='$buildId'"),
                dslContext.query("DELETE FROM $artifactoryDbName.${tableInfo[artifacetoryInfoTableKey]} WHERE BUILD_ID='$buildId'")
        )
    }
}
