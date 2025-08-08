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

package com.tencent.devops.common.db.pojo

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.datasource")
data class DataSourceProperties(
    val dataSourceConfigs: List<DataSourceConfig>, // 数据源配置
    val tableRuleConfigs: List<TableRuleConfig>, // 数据库表规则配置
    val bindingTableGroupConfigs: List<BindingTableGroupConfig>? = null, // 绑定表规则配置
    val migratingDataSourceConfigs: List<DataSourceConfig>? = null, // 迁移数据源配置
    val migratingTableRuleConfigs: List<TableRuleConfig>? = null, // 迁移数据库表规则配置
    val migratingBindingTableGroupConfigs: List<BindingTableGroupConfig>? = null, // 迁移绑定表规则配置
    val archiveDataSourceConfigs: List<DataSourceConfig>? = null, // 归档数据源配置
    val archiveTableRuleConfigs: List<TableRuleConfig>? = null, // 归档数据库表规则配置
    val archiveBindingTableGroupConfigs: List<BindingTableGroupConfig>? = null // 归档绑定表规则配置
)
