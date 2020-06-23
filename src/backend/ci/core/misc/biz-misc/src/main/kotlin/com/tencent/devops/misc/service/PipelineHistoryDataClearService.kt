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

abstract class PipelineHistoryDataClearService {

    val projectDbKey = "projectDb" // project数据库
    val processDbKey = "processDb" // process数据库
    protected val repositoryDbKey = "repositoryDb" // repository数据库
    protected val dispatchDbKey = "dispatchDb" // dispatch数据库
    protected val pluginDbKey = "pluginDb" // plugin数据库
    protected val qualityDbKey = "quality" // quality数据库
    protected val artifactoryDbKey = "artifactoryDb" // artifactory数据库
    val projectTableKey = "projectTable"
    val pipelineInfoTableKey = "pipelineInfoTable"
    val pipelineBuildHistoryTableKey = "pipelineBuildHistoryTable"
    private val pipelineBuildDetailTableKey = "pipelineBuildDetailTable"
    private val pipelineBuildTaskTableKey = "pipelineBuildTaskTable"
    private val pipelineBuildVarTableKey = "pipelineBuildVarTable"
    private val pipelineBuildContainerTableKey = "pipelineBuildContainerTable"
    private val pipelineBuildStageTableKey = "pipelineBuildStageTable"
    private val pipelineBuildHisDataClearTableKey = "pipelineBuildHisDataClearTable"
    private val reportTableKey = "reportTable"
    private val repositoryCommitTableKey = "repositoryCommitTable"
    private val dispatchPipelineBuildTableKey = "dispatchPipelineBuildTable"
    private val dispatchPipelineDockerBuildTableKey = "dispatchPipelineDockerBuildTable"
    private val dispatchThirdpartyAgentBuildTableKey = "dispatchThirdpartyAgentBuildTable"
    private val pluginCodeccTableKey = "pluginCodeccTable"
    private val qualityHisDetailMetadataTableKey = "qualityHisDetailMetadataTable"
    private val qualityHisOriginMetadataTableKey = "qualityHisOriginMetadataTable"

    abstract fun getDataBaseInfo(): Map<String, String>

    fun getTableInfo(): Map<String, String> {
        var tableInfoMap = mapOf(
            pipelineInfoTableKey to "T_PIPELINE_INFO",
            pipelineBuildHistoryTableKey to "T_PIPELINE_BUILD_HISTORY",
            pipelineBuildDetailTableKey to "T_PIPELINE_BUILD_DETAIL",
            pipelineBuildTaskTableKey to "T_PIPELINE_BUILD_TASK",
            pipelineBuildVarTableKey to "T_PIPELINE_BUILD_VAR",
            pipelineBuildContainerTableKey to "T_PIPELINE_BUILD_CONTAINER",
            pipelineBuildStageTableKey to "T_PIPELINE_BUILD_STAGE",
            pipelineBuildHisDataClearTableKey to "T_PIPELINE_BUILD_HIS_DATA_CLEAR",
            reportTableKey to "T_REPORT",
            repositoryCommitTableKey to "T_REPOSITORY_COMMIT",
            dispatchPipelineBuildTableKey to "T_DISPATCH_PIPELINE_BUILD",
            dispatchPipelineDockerBuildTableKey to "T_DISPATCH_PIPELINE_DOCKER_BUILD",
            dispatchThirdpartyAgentBuildTableKey to "T_DISPATCH_THIRDPARTY_AGENT_BUILD",
            pluginCodeccTableKey to "T_PLUGIN_CODECC",
            qualityHisDetailMetadataTableKey to "T_QUALITY_HIS_DETAIL_METADATA",
            qualityHisOriginMetadataTableKey to "T_QUALITY_HIS_ORIGIN_METADATA"
        )
        tableInfoMap = tableInfoMap.plus(getSpecTableInfo())
        return tableInfoMap
    }

    abstract fun getSpecTableInfo(): Map<String, String>

    /**
     * 获取清理sql列表
     */
    fun getClearSqlList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<Query> {
        val batchSqlList = mutableListOf<Query>(
            dslContext.query("DELETE FROM $processDbKey.$pipelineBuildDetailTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $processDbKey.$pipelineBuildTaskTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $processDbKey.$pipelineBuildVarTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $processDbKey.$pipelineBuildContainerTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $processDbKey.$pipelineBuildStageTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $processDbKey.$reportTableKey WHERE PROJECT_ID='$projectId' AND PIPELINE_ID='$pipelineId' AND BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $repositoryDbKey.$repositoryCommitTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $dispatchDbKey.$dispatchPipelineBuildTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $dispatchDbKey.$dispatchPipelineDockerBuildTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $dispatchDbKey.$dispatchThirdpartyAgentBuildTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $pluginDbKey.$pluginCodeccTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $qualityDbKey.$qualityHisDetailMetadataTableKey WHERE BUILD_ID='$buildId'"),
            dslContext.query("DELETE FROM $qualityDbKey.$qualityHisOriginMetadataTableKey WHERE BUILD_ID='$buildId'")
        )
        batchSqlList.addAll(getSpecClearSqlList(dslContext, projectId, pipelineId, buildId))
        // 添加删除记录，用“REPLACE INTO”方式插入实现幂等
        batchSqlList.add(dslContext.query("REPLACE INTO $processDbKey.$pipelineBuildHisDataClearTableKey(BUILD_ID,PIPELINE_ID,PROJECT_ID) VALUES ('$buildId','$pipelineId','$projectId')"))
        batchSqlList.add(dslContext.query("DELETE FROM $processDbKey.$pipelineBuildHistoryTableKey WHERE BUILD_ID='$buildId'"))
        return batchSqlList
    }

    abstract fun getSpecClearSqlList(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<Query>
}