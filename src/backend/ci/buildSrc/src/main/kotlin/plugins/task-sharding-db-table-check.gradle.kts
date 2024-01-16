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
import java.sql.DatabaseMetaData
import java.sql.DriverManager

val commonScriptUrl = javaClass.getResource("/common.gradle.kts")
apply(from = commonScriptUrl)

val shardingTableRegex = "(.+)_(\\d+)".toRegex()
val getMysqlInfo = extra["getMysqlInfo"] as (String) -> Triple<String, String, String>
val getDatabaseName = extra["getDatabaseName"] as (String) -> String
val shardingDbTableCheckTask = tasks.register("shardingDbTableCheck") {
    doLast {
        val moduleName = getBkModuleName()
        var (mysqlURL, mysqlUser, mysqlPasswd) = getMysqlInfo(moduleName)
        val normalDbUrls = mysqlURL.split(",")
        var archiveDbUrls = System.getenv("${moduleName}ArchiveMysqlURL")?.split(",")
        if (moduleName in listOf("process", "engine") && archiveDbUrls == null) {
            archiveDbUrls = normalDbUrls
        }
        if ((!normalDbUrls.isEmpty() && normalDbUrls.size >1) || archiveDbUrls?.isEmpty() == false) {
            val databaseName = getDatabaseName(moduleName)
            // 各普通DB的表进行比较
            val referNormalDb = if (normalDbUrls.size >1) {
                doCompareDatabasesBus(
                    dbUrls = normalDbUrls,
                    mysqlUser = mysqlUser,
                    mysqlPasswd = mysqlPasswd,
                    databaseName = databaseName
                )
            } else {
                getDatabaseData(
                    url = normalDbUrls[0],
                    user = mysqlUser,
                    password = mysqlPasswd,
                    databaseName = databaseName
                )
            }
            if (archiveDbUrls?.isEmpty() == false) {
                val archiveDatabaseName = databaseName.replace(moduleName, "archive_$moduleName")
                // 各归档DB的表和原表进行比较
                archiveDbUrls.forEach { archiveDbUrl ->
                    val compareArchiveDb = getDatabaseData(
                        url = archiveDbUrl,
                        user = mysqlUser,
                        password = mysqlPasswd,
                        databaseName = archiveDatabaseName
                    )
                    compareDatabases(
                        referenceDb = referNormalDb,
                        compareDb = compareArchiveDb,
                        singleChipCompareFlag = true
                    )
                }
                // 各归档DB的表进行比较
                if (archiveDbUrls.size > 1) {
                    doCompareDatabasesBus(
                        dbUrls = archiveDbUrls,
                        mysqlUser = mysqlUser,
                        mysqlPasswd = mysqlPasswd,
                        databaseName = archiveDatabaseName
                    )
                }
            }
        }
    }
}
tasks.getByName("compileKotlin").dependsOn(shardingDbTableCheckTask.name)

data class ColumnInfo(val name: String, val type: String, val length: Int, val nullable: Boolean)
data class IndexInfo(val name: String, val columnNames: List<String>, val unique: Boolean)
data class TableInfo(val name: String, val columns: List<ColumnInfo>, val indexes: List<IndexInfo>)
data class DatabaseInfo(val url: String, val databaseName: String, val tables: List<TableInfo>)

fun getDatabaseData(
    url: String,
    user: String,
    password: String,
    databaseName: String
): DatabaseInfo {
    val tables = mutableListOf<TableInfo>()
    val connectionUrl = "jdbc:mysql://$url/$databaseName?useSSL=false&nullCatalogMeansCurrent=true"
    DriverManager.getConnection(connectionUrl, user, password).use { connection ->
        val metaData = connection.metaData
        val tableNames = getTableNames(metaData, databaseName)

        for (tableName in tableNames) {
            val columns = getColumnData(metaData, tableName)
            val indexes = getIndexData(metaData, tableName)
            tables.add(TableInfo(tableName, columns, indexes))
        }
    }

    return DatabaseInfo(url, databaseName, tables)
}

fun getTableNames(metaData: DatabaseMetaData, databaseName: String): List<String> {
    val tableNames = mutableListOf<String>()
    val resultSet = metaData.getTables(null, databaseName, "%", arrayOf("TABLE"))
    while (resultSet.next()) {
        tableNames.add(resultSet.getString("TABLE_NAME"))
    }

    return tableNames
}

fun getColumnData(metaData: DatabaseMetaData, tableName: String): List<ColumnInfo> {
    val columns = mutableListOf<ColumnInfo>()

    val resultSet = metaData.getColumns(null, null, tableName, null)
    while (resultSet.next()) {
        columns.add(
            ColumnInfo(
                name = resultSet.getString("COLUMN_NAME"),
                type = resultSet.getString("TYPE_NAME"),
                length = resultSet.getInt("COLUMN_SIZE"),
                nullable = resultSet.getInt("NULLABLE") == 1
            )
        )
    }

    return columns
}

fun getIndexData(metaData: DatabaseMetaData, tableName: String): List<IndexInfo> {
    val indexes = mutableMapOf<String, IndexInfo>()

    val resultSet = metaData.getIndexInfo(null, null, tableName, false, true)
    while (resultSet.next()) {
        val indexName = resultSet.getString("INDEX_NAME")
        val columnName = resultSet.getString("COLUMN_NAME")
        val nonUnique = resultSet.getBoolean("NON_UNIQUE")

        val indexInfo = indexes.getOrPut(indexName) {
            IndexInfo(indexName, mutableListOf(), !nonUnique)
        }

        (indexInfo.columnNames as MutableList).add(columnName)
    }

    return indexes.values.toList()
}

fun compareDatabases(
    referenceDb: DatabaseInfo,
    compareDb: DatabaseInfo,
    singleChipCompareFlag: Boolean = false
) {
    val referenceTables = referenceDb.tables
    val referenceTableNames = referenceTables.map { it.name }.toSet()
    val compareTables = compareDb.tables
    val compareTableNames = compareTables.map { it.name }.toSet()
    if (!singleChipCompareFlag) {
        val missingTableNames = referenceTableNames - compareTableNames
        if (missingTableNames.isNotEmpty()) {
            val finalMissingTableNames = getFinalCheckTableNames(referenceTableNames, missingTableNames)
            if (finalMissingTableNames.isNotEmpty()) {
                println("Missing tables: $finalMissingTableNames")
            }
        }
    }
    val extraTableNames = compareTableNames - referenceTableNames
    var finalExtraTableNames: Set<String> = mutableSetOf()
    if (extraTableNames.isNotEmpty()) {
        finalExtraTableNames = getFinalCheckTableNames(referenceTableNames, extraTableNames)
        if (finalExtraTableNames.isNotEmpty()) {
            println("Extra tables: $finalExtraTableNames")
        }
    }
    val validShardingTableNames = extraTableNames - finalExtraTableNames
    val commonTableNames = referenceTableNames.intersect(compareTableNames).toMutableSet()
    commonTableNames.addAll(validShardingTableNames)
    for (tableName in commonTableNames) {
        val referenceTable =
            referenceTables.first { it.name == tableName || getValidShardingTableFlag(tableName, it.name) }
        val compareTable =
            compareTables.first { it.name == tableName || getValidShardingTableFlag(tableName, it.name) }

        if (referenceTable.columns != compareTable.columns) {
            println("Column structure mismatch in table $tableName:")
            println("Reference columns: ${referenceTable.columns}")
            println("Compared columns: ${compareTable.columns}")
            var missingColumns = referenceTable.columns - compareTable.columns
            var extraColumns = compareTable.columns - referenceTable.columns
            val mismatchColumns = mutableListOf<ColumnInfo>()
            extraColumns.forEach { extraColumn ->
                if (!missingColumns.first{ it.name == extraColumn.name}.nullable) {
                    mismatchColumns.add(extraColumn)
                }
            }
            missingColumns = missingColumns - mismatchColumns
            extraColumns = extraColumns - mismatchColumns
            println("Compared missingColumns: $missingColumns,extraColumns: $extraColumns,mismatchColumns: $mismatchColumns")
        }

        if (referenceTable.indexes != compareTable.indexes) {
            println("Index structure mismatch in table $tableName:")
            println("Reference indexes: ${referenceTable.indexes}")
            println("Compared indexes: ${compareTable.indexes}")
            val missingIndexes = referenceTable.indexes - compareTable.indexes
            val extraIndexes = compareTable.indexes - referenceTable.indexes
            println("Compared missingIndexes: $missingIndexes,extraIndexes: $extraIndexes")
        }
    }
}

fun doCompareDatabasesBus(
    dbUrls: List<String>,
    mysqlUser: String,
    mysqlPasswd: String,
    databaseName: String
): DatabaseInfo {
    val referDbUrl = dbUrls[0]
    val referDb = getDatabaseData(
        url = referDbUrl,
        user = mysqlUser,
        password = mysqlPasswd,
        databaseName = databaseName
    )
    dbUrls.subList(1, dbUrls.size).forEach { dbUrl ->
        val compareDb = getDatabaseData(
            url = dbUrl,
            user = mysqlUser,
            password = mysqlPasswd,
            databaseName = databaseName
        )
        compareDatabases(referDb, compareDb)
    }
    return referDb
}

fun getBkModuleName(): String {
    val propertyName = "i18n.module.name"
    var moduleName = if (project.hasProperty(propertyName)) {
        project.property(propertyName)?.toString()
    } else {
        ""
    }
    if (moduleName.isNullOrBlank()) {
        // 根据项目名称提取微服务名称
        val parts = project.name.split("-")
        val num = if (parts.size > 2) {
            parts.size - 1
        } else {
            parts.size
        }
        val projectNameSb = StringBuilder()
        for (i in 1 until num) {
            if (i != num - 1) {
                projectNameSb.append(parts[i]).append("-")
            } else {
                projectNameSb.append(parts[i])
            }
        }
        moduleName = projectNameSb.toString().let { if (it == "engine") "process" else it }
    }
    return moduleName
}

fun getFinalCheckTableNames(
    referenceTableNames: Set<String>,
    tableNames: Set<String>
): MutableSet<String> {
    val finalCheckTableNames = mutableSetOf<String>()
    tableNames.forEach { tableName ->
        val validShardingTableFlag = getValidShardingTableFlag(tableName, referenceTableNames)
        if (validShardingTableFlag) {
            // 满足分表正则匹配，则校验表是否存在
            println("The table structure of $tableName table needs to be verified")
        } else {
            finalCheckTableNames.add(tableName)
        }
    }
    return finalCheckTableNames
}

fun getValidShardingTableFlag(
    tableName: String,
    referenceTableNames: Set<String>
): Boolean {
    val tableMatchResult = shardingTableRegex.find(tableName)
    val validShardingTableFlag =
        tableMatchResult != null && referenceTableNames.contains(tableMatchResult.groupValues[1])
    return validShardingTableFlag
}

fun getValidShardingTableFlag(
    tableName: String,
    referenceTableName: String
): Boolean {
    val tableMatchResult = shardingTableRegex.find(tableName)
    val validShardingTableFlag =
        tableMatchResult != null && referenceTableName == tableMatchResult.groupValues[1]
    return validShardingTableFlag
}
