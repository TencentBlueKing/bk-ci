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

package com.tencent.devops.misc.service.project

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.misc.dao.project.DataSourceDao
import com.tencent.devops.project.pojo.DataSource
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataSourceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dataSourceDao: DataSourceDao
) {

    fun listByModule(
        clusterName: String,
        moduleCode: SystemModuleEnum,
        fullFlag: Boolean? = false,
        dataTag: String? = null
    ): List<DataSource>? {
        val dataSourceRecords = dataSourceDao.listByModule(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            fullFlag = false,
            dataTag = dataTag
        )
        return if (dataSourceRecords == null) {
            null
        } else {
            val dataSourceList = mutableListOf<DataSource>()
            dataSourceRecords.forEach { dataSourceRecord ->
                dataSourceList.add(
                    DataSource(
                        clusterName = clusterName,
                        moduleCode = moduleCode,
                        dataSourceName = dataSourceRecord.dataSourceName,
                        fullFlag = dataSourceRecord.fullFlag,
                        dsUrl = dataSourceRecord.dsUrl
                    )
                )
            }
            dataSourceList
        }
    }
}
