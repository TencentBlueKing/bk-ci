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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.project.dao.TableShardingConfigDao
import com.tencent.devops.project.pojo.TableShardingConfig
import com.tencent.devops.project.service.TableShardingConfigService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TableShardingConfigServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val tableShardingConfigDao: TableShardingConfigDao
) : TableShardingConfigService {

    override fun addTableShardingConfig(userId: String, tableShardingConfig: TableShardingConfig): Boolean {
        val clusterName = tableShardingConfig.clusterName
        val moduleCode = tableShardingConfig.moduleCode
        val tableName = tableShardingConfig.tableName
        val nameCount = tableShardingConfigDao.countByName(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            tableName = tableName
        )
        if (nameCount > 0) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf("[$clusterName-$moduleCode]$tableName")
            )
        }
        tableShardingConfigDao.add(dslContext, userId, tableShardingConfig)
        return true
    }

    override fun deleteTableShardingConfig(userId: String, id: String): Boolean {
        tableShardingConfigDao.delete(dslContext, id)
        return true
    }

    override fun updateTableShardingConfig(
        userId: String,
        id: String,
        tableShardingConfig: TableShardingConfig
    ): Boolean {
        val clusterName = tableShardingConfig.clusterName
        val moduleCode = tableShardingConfig.moduleCode
        val tableName = tableShardingConfig.tableName
        val nameCount = tableShardingConfigDao.countByName(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            tableName = tableName
        )
        if (nameCount > 0) {
            val obj = tableShardingConfigDao.getById(dslContext, id)
            if (null != obj && moduleCode.name != obj.moduleCode && tableName != obj.tableName) {
                // 抛出错误提示
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf("[$clusterName-$moduleCode]$tableName")
                )
            }
        }
        tableShardingConfigDao.update(
            dslContext = dslContext,
            id = id,
            userId = userId,
            tableShardingConfig = tableShardingConfig
        )
        return true
    }

    override fun getTableShardingConfigById(id: String): TableShardingConfig? {
        val record = tableShardingConfigDao.getById(dslContext, id)
        return if (record != null) {
            TableShardingConfig(
                clusterName = record.clusterName,
                moduleCode = SystemModuleEnum.valueOf(record.moduleCode),
                tableName = record.tableName,
                shardingNum = record.shardingNum
            )
        } else {
            null
        }
    }

    override fun getTableShardingConfigByName(
        clusterName: String,
        moduleCode: SystemModuleEnum,
        tableName: String
    ): TableShardingConfig? {
        val record = tableShardingConfigDao.getByName(
            dslContext = dslContext,
            clusterName = clusterName,
            moduleCode = moduleCode,
            tableName = tableName
        )
        return if (record != null) {
            TableShardingConfig(
                clusterName = record.clusterName,
                moduleCode = SystemModuleEnum.valueOf(record.moduleCode),
                tableName = record.tableName,
                shardingNum = record.shardingNum
            )
        } else {
            null
        }
    }

    override fun listByModule(
        dslContext: DSLContext,
        clusterName: String,
        moduleCode: SystemModuleEnum
    ): List<TableShardingConfig>? {
        var tableShardingConfigs: MutableList<TableShardingConfig>? = null
        val records = tableShardingConfigDao.listByModule(dslContext, clusterName, moduleCode)
        records?.forEach { record ->
            if (tableShardingConfigs == null) {
                tableShardingConfigs = mutableListOf()
            }
            tableShardingConfigs?.add(
                TableShardingConfig(
                    clusterName = record.clusterName,
                    moduleCode = SystemModuleEnum.valueOf(record.moduleCode),
                    tableName = record.tableName,
                    shardingNum = record.shardingNum
                )
            )
        }
        return tableShardingConfigs
    }
}
