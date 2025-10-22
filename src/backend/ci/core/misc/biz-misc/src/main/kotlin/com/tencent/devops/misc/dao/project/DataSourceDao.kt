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

package com.tencent.devops.misc.dao.project

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.model.project.tables.TDataSource
import com.tencent.devops.model.project.tables.records.TDataSourceRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class DataSourceDao {

    fun listByModule(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum,
        fullFlag: Boolean? = false,
        dataTag: String? = null
    ): Result<TDataSourceRecord>? {
        return with(TDataSource.T_DATA_SOURCE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(CLUSTER_NAME.eq(clusterName))
            conditions.add(MODULE_CODE.eq(moduleCode.name))
            if (fullFlag != null) {
                conditions.add(FULL_FLAG.eq(fullFlag))
            }
            if (dataTag != null) {
                conditions.add(TAG.eq(dataTag))
            } else {
                conditions.add(TAG.isNull)
            }
            dslContext.selectFrom(this).where(conditions).orderBy(DATA_SOURCE_NAME.asc()).fetch()
        }
    }
}
