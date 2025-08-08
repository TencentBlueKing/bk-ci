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
import utils.DatabaseUtil
import utils.ModuleUtil
import java.sql.DatabaseMetaData
import java.sql.DriverManager

val shardingTableRegex = "(.+)_(\\d+)".toRegex()
val shardingDbTableCheckTask = tasks.register("shardingDbTableCheck") {
    doLast {
        val bkModuleName =
            ModuleUtil.getBkModuleName(project.name, project.findProperty("i18n.module.name")?.toString())
        val moduleNames = ModuleUtil.getBkActualModuleNames(bkModuleName)
        moduleNames.forEach { moduleName ->
            val (mysqlURL, mysqlUser, mysqlPasswd) = DatabaseUtil.getMysqlInfo(
                moduleName = moduleName,
                defaultMysqlURL = project.extra["DB_HOST"]?.toString(),
                defaultMysqlUser = project.extra["DB_USERNAME"]?.toString(),
                defaultMysqlPasswd = project.extra["DB_PASSWORD"]?.toString()
            )
            val normalDbUrls = mysqlURL.split(",")
            var archiveDbUrls = System.getenv("${moduleName}ArchiveMysqlURL")?.split(",")
            if (moduleName in listOf("process", "engine") && archiveDbUrls == null) {
                archiveDbUrls = normalDbUrls
            }
            if ((normalDbUrls.isNotEmpty() && normalDbUrls.size > 1) || archiveDbUrls?.isEmpty() == false) {
                val databaseName = DatabaseUtil.getDatabaseName(moduleName, project.extra["DB_PREFIX"].toString())
                // 各普通DB的表进行比较
                val referNormalDb = doCompareDatabasesBus(
                    dbUrls = normalDbUrls,
                    mysqlUser = mysqlUser,
                    mysqlPasswd = mysqlPasswd,
                    databaseName = databaseName
                )
                if (archiveDbUrls?.isEmpty() == false) {
                    // 获取归档库的数据库名称
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
    Class.forName("com.mysql.cj.jdbc.Driver");
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
    val referenceDbUrl = referenceDb.url
    val referenceDatabaseName = referenceDb.databaseName
    val referenceTables = referenceDb.tables
    val referenceTableNames = referenceTables.map { it.name }.toSet()
    val compareDbUrl = compareDb.url
    val compareDatabaseName = compareDb.databaseName
    val compareTables = compareDb.tables
    val compareTableNames = compareTables.map { it.name }.toSet()
    if (!singleChipCompareFlag) {
        // 如果没有打开单向对比开关，那么需要将对比库与参照库对比看表是否有缺失
        val missingTableNames = referenceTableNames - compareTableNames
        if (missingTableNames.isNotEmpty()) {
            // 排除对比库和参照库的表因分表造成的表名差异
            val finalMissingTableNames = getFinalCheckTableNames(referenceTableNames, missingTableNames)
            if (finalMissingTableNames.isNotEmpty()) {
                throw RuntimeException(
                    "Missing table in database ($compareDbUrl/$compareDatabaseName) relative to " +
                        "database ($referenceDbUrl/$referenceDatabaseName): $finalMissingTableNames."
                )
            }
        }
    }
    // 将对比库与参照库对比看表是否有多出
    val extraTableNames = compareTableNames - referenceTableNames
    var finalExtraTableNames: Set<String> = mutableSetOf()
    if (extraTableNames.isNotEmpty()) {
        finalExtraTableNames = getFinalCheckTableNames(referenceTableNames, extraTableNames)
        if (finalExtraTableNames.isNotEmpty()) {
            throw RuntimeException(
                "Compared with database ($referenceDbUrl/$referenceDatabaseName), the extra tables" +
                    " in database ($compareDbUrl/$compareDatabaseName): $finalExtraTableNames."
            )
        }
    }
    // 找出对比库的分表集合
    val validShardingTableNames = extraTableNames - finalExtraTableNames
    // 找出二个库表名相同的公共表
    val commonTableNames = referenceTableNames.intersect(compareTableNames).toMutableSet()
    // 将对比库的分表集合数据加入公共表集合
    commonTableNames.addAll(validShardingTableNames)
    // 对比公共表在二个库的结构是否有差异
    for (tableName in commonTableNames) {
        val referenceTable =
            referenceTables.first { it.name == tableName || getValidShardingTableFlag(tableName, it.name) }
        val compareTable =
            compareTables.first { it.name == tableName || getValidShardingTableFlag(tableName, it.name) }

        // 比较表的字段是否有差异
        compareColumns(
            referenceTable = referenceTable,
            compareTable = compareTable,
            referenceDbUrl = referenceDbUrl,
            referenceDatabaseName = referenceDatabaseName,
            tableName = tableName,
            compareDbUrl = compareDbUrl,
            compareDatabaseName = compareDatabaseName
        )
        // 比较表的索引是否有差异
        compareIndexes(
            referenceTable = referenceTable,
            compareTable = compareTable,
            referenceDbUrl = referenceDbUrl,
            referenceDatabaseName = referenceDatabaseName,
            tableName = tableName,
            compareDbUrl = compareDbUrl,
            compareDatabaseName = compareDatabaseName
        )
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
    if (dbUrls.size > 1) {
        dbUrls.subList(1, dbUrls.size).forEach { dbUrl ->
            val compareDb = getDatabaseData(
                url = dbUrl,
                user = mysqlUser,
                password = mysqlPasswd,
                databaseName = databaseName
            )
            compareDatabases(referDb, compareDb)
        }
    }
    return referDb
}

fun getFinalCheckTableNames(
    referenceTableNames: Set<String>,
    tableNames: Set<String>
): MutableSet<String> {
    val finalCheckTableNames = mutableSetOf<String>()
    tableNames.forEach { tableName ->
        val validShardingTableFlag = getValidShardingTableFlag(tableName, referenceTableNames)
        if (!validShardingTableFlag) {
            // 表不是分表类型则需要进一步校验
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

fun compareColumns(
    referenceTable: TableInfo,
    compareTable: TableInfo,
    referenceDbUrl: String,
    referenceDatabaseName: String,
    tableName: String,
    compareDbUrl: String,
    compareDatabaseName: String
) {
    if (referenceTable.columns == compareTable.columns) {
        return
    }
    var missingColumns = referenceTable.columns - compareTable.columns.toSet()
    var extraColumns = compareTable.columns - referenceTable.columns.toSet()
    val mismatchColumns = mutableListOf<ColumnInfo>()
    val missingColumnNames = missingColumns.map { it.name }
    extraColumns.forEach { extraColumn ->
        if (missingColumnNames.contains(extraColumn.name)) {
            mismatchColumns.add(extraColumn)
        }
    }
    val mismatchColumnNames = mismatchColumns.map { it.name }
    missingColumns = missingColumns.filter { !mismatchColumnNames.contains(it.name) }
    extraColumns = extraColumns - mismatchColumns.toSet()
    val columnTip = "Compared with the table of database ($referenceDbUrl/$referenceDatabaseName), " +
            "the differences of table ($tableName) of database ($compareDbUrl/$compareDatabaseName) are as " +
            "follows:  missing columns: $missingColumns;  extra columns: $extraColumns;  " +
            "different columns: $mismatchColumns."
    if (missingColumns.isNotEmpty() || extraColumns.isNotEmpty() || mismatchColumns.isNotEmpty()) {
        // 字段有差异则抛出错误提示
        throw RuntimeException(columnTip)
    }
}

fun compareIndexes(
    referenceTable: TableInfo,
    compareTable: TableInfo,
    referenceDbUrl: String,
    referenceDatabaseName: String,
    tableName: String,
    compareDbUrl: String,
    compareDatabaseName: String
) {
    if (referenceTable.indexes == compareTable.indexes) {
        return
    }
    var missingIndexes = referenceTable.indexes - compareTable.indexes.toSet()
    var extraIndexes = compareTable.indexes - referenceTable.indexes.toSet()
    val mismatchIndexes = mutableListOf<IndexInfo>()
    val missingIndexNames = missingIndexes.map { it.name }
    extraIndexes.forEach { extraIndex ->
        if (missingIndexNames.contains(extraIndex.name)) {
            mismatchIndexes.add(extraIndex)
        }
    }
    val mismatchIndexNames = mismatchIndexes.map { it.name }
    missingIndexes = missingIndexes.filter { !mismatchIndexNames.contains(it.name) }
    extraIndexes = extraIndexes - mismatchIndexes.toSet()
    val indexTip = "Compared with the table of database ($referenceDbUrl/$referenceDatabaseName), " +
            "the differences of table ($tableName) of database ($compareDbUrl/$compareDatabaseName) are as " +
            "follows:  missing indexs: $missingIndexes;  extra indexs: $extraIndexes;  " +
            "different indexs: $mismatchIndexes."
    if (missingIndexes.isNotEmpty() || extraIndexes.isNotEmpty() || mismatchIndexes.isNotEmpty()) {
        // 字段有差异则抛出错误提示
        throw RuntimeException(indexTip)
    }
}
