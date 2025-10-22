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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.project.dao.DataSourceDao
import com.tencent.devops.project.dao.ShardingRoutingRuleDao
import com.tencent.devops.project.pojo.DataBasePiecewiseInfo
import com.tencent.devops.project.pojo.DataSource
import com.tencent.devops.project.service.DataSourceService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataSourceServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val shardingRoutingRuleDao: ShardingRoutingRuleDao,
    private val dataSourceDao: DataSourceDao
) : DataSourceService {

    override fun addDataSource(userId: String, dataSource: DataSource): Boolean {
        val clusterName = dataSource.clusterName
        val dataSourceName = dataSource.dataSourceName
        val moduleCode = dataSource.moduleCode.name
        val nameCount = dataSourceDao.countByName(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            dataSourceName = dataSourceName
        )
        if (nameCount > 0) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf("[$clusterName-$moduleCode]$dataSourceName")
            )
        }
        dataSourceDao.add(dslContext, userId, dataSource)
        return true
    }

    override fun deleteDataSource(userId: String, id: String): Boolean {
        dataSourceDao.delete(dslContext, id)
        return true
    }

    override fun updateDataSource(userId: String, id: String, dataSource: DataSource): Boolean {
        val clusterName = dataSource.clusterName
        val dataSourceName = dataSource.dataSourceName
        val moduleCode = dataSource.moduleCode.name
        val nameCount = dataSourceDao.countByName(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            dataSourceName = dataSourceName
        )
        if (nameCount > 0) {
            // 判断更新的名称是否属于自已
            val obj = dataSourceDao.getById(dslContext, id)
            if (null != obj && moduleCode != obj.moduleCode && dataSourceName != obj.dataSourceName) {
                // 抛出错误提示
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf("[$clusterName-$moduleCode]$dataSourceName")
                )
            }
        }
        dataSourceDao.update(dslContext, id, dataSource)
        return true
    }

    override fun getDataSourceById(id: String): DataSource? {
        val record = dataSourceDao.getById(dslContext, id)
        return if (record != null) {
            DataSource(
                clusterName = record.clusterName,
                moduleCode = SystemModuleEnum.valueOf(record.moduleCode),
                dataSourceName = record.dataSourceName,
                fullFlag = record.fullFlag,
                dsUrl = record.dsUrl
            )
        } else {
            null
        }
    }

    override fun getDataBasePiecewiseById(
        projectId: String,
        moduleCode: SystemModuleEnum,
        clusterName: String,
        ruleType: ShardingRuleTypeEnum,
        tableName: String?
    ): DataBasePiecewiseInfo? {
        val routingRuleRecord = shardingRoutingRuleDao.get(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            type = ruleType,
            routingName = projectId,
            tableName = tableName
        )
        if (routingRuleRecord != null) {
            val dataSource = dataSourceDao.getDataBasePiecewiseById(
                dslContext = dslContext,
                moduleCode = moduleCode,
                clusterName = clusterName,
                routingRule = routingRuleRecord.routingRule
            ) ?: return null
            return DataBasePiecewiseInfo(
                projectId = projectId,
                moduleCode = moduleCode,
                clusterName = dataSource.clusterName,
                dataSourceName = dataSource.dataSourceName,
                routingRule = routingRuleRecord.routingRule,
                dsUrl = dataSource.dsUrl
            )
        }
        return null
    }
}
