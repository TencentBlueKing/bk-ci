/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.monitor.export

class CreateDatabaseQueryBuilder(databaseName: String) {
    private val databaseName: String
    private val retentionPolicyClauses = arrayOfNulls<String>(4)

    init {
        require(databaseName.isNotBlank()) { "The database name cannot be null or empty" }
        this.databaseName = databaseName
    }

    fun setRetentionDuration(retentionDuration: String?): CreateDatabaseQueryBuilder {
        if (!retentionDuration.isNullOrBlank()) {
            retentionPolicyClauses[0] = DURATION_CLAUSE_TEMPLATE.format(retentionDuration)
        }
        return this
    }

    fun setRetentionReplicationFactor(retentionReplicationFactor: Int?): CreateDatabaseQueryBuilder {
        if (retentionReplicationFactor != null) {
            retentionPolicyClauses[1] = REPLICATION_FACTOR_CLAUSE_TEMPLATE.format(retentionReplicationFactor)
        }
        return this
    }

    fun setRetentionShardDuration(retentionShardDuration: String?): CreateDatabaseQueryBuilder {
        if (!retentionShardDuration.isNullOrBlank()) {
            retentionPolicyClauses[2] = SHARD_DURATION_CLAUSE_TEMPLATE.format(retentionShardDuration)
        }
        return this
    }

    fun setRetentionPolicyName(retentionPolicyName: String?): CreateDatabaseQueryBuilder {
        if (!retentionPolicyName.isNullOrBlank()) {
            retentionPolicyClauses[3] = NAME_CLAUSE_TEMPLATE.format(retentionPolicyName)
        }
        return this
    }

    fun build(): String {
        val queryStringBuilder = StringBuilder(QUERY_MANDATORY_TEMPLATE.format(databaseName))
        if (hasAnyRetentionPolicy()) {
            queryStringBuilder.append(RETENTION_POLICY_INTRODUCTION)
            queryStringBuilder.append(retentionPolicyClauses.filterNotNull().joinToString(""))
        }
        return queryStringBuilder.toString()
    }

    private fun hasAnyRetentionPolicy(): Boolean {
        return retentionPolicyClauses.any { !it.isNullOrBlank() }
    }

    companion object {
        private const val QUERY_MANDATORY_TEMPLATE = "CREATE DATABASE \"%s\""
        private const val RETENTION_POLICY_INTRODUCTION = " WITH"
        private const val DURATION_CLAUSE_TEMPLATE = " DURATION %s"
        private const val REPLICATION_FACTOR_CLAUSE_TEMPLATE = " REPLICATION %d"
        private const val SHARD_DURATION_CLAUSE_TEMPLATE = " SHARD DURATION %s"
        private const val NAME_CLAUSE_TEMPLATE = " NAME %s"
    }
}
