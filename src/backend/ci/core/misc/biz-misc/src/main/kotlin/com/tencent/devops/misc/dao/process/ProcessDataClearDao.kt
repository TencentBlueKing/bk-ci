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

package com.tencent.devops.misc.dao.process

import com.tencent.devops.model.process.tables.TBuildStartupParam
import com.tencent.devops.model.process.tables.TPipelineBuildContainer
import com.tencent.devops.model.process.tables.TPipelineBuildDetail
import com.tencent.devops.model.process.tables.TPipelineBuildHistory
import com.tencent.devops.model.process.tables.TPipelineBuildStage
import com.tencent.devops.model.process.tables.TPipelineBuildTask
import com.tencent.devops.model.process.tables.TPipelineBuildVar
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProcessDataClearDao {

    fun deleteBuildTaskByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            dslContext.deleteFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .execute()
        }
    }

    fun deleteBuildVarByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildVar.T_PIPELINE_BUILD_VAR) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildContainerByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildContainer.T_PIPELINE_BUILD_CONTAINER) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildStageByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildStage.T_PIPELINE_BUILD_STAGE) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildDetailByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteReportByBuildId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ) {
        with(TPipelineBuildStage.T_PIPELINE_BUILD_STAGE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildStartupParamByBuildId(dslContext: DSLContext, buildId: String) {
        with(TBuildStartupParam.T_BUILD_STARTUP_PARAM) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun deleteBuildHistoryByBuildId(dslContext: DSLContext, buildId: String) {
        with(TPipelineBuildHistory.T_PIPELINE_BUILD_HISTORY) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }
}
