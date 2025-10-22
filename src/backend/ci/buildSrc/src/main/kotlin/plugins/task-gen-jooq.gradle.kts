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
import nu.studer.gradle.jooq.JooqGenerate
import utils.DatabaseUtil
import utils.ModuleUtil

plugins {
    id("nu.studer.jooq")
}

val jooqGenerator by configurations
val api by configurations

dependencies {
    jooqGenerator("com.mysql:mysql-connector-j")
    api("org.jooq:jooq")
}

val bkModuleName = ModuleUtil.getBkModuleName(project.name, project.findProperty("i18n.module.name")?.toString())
val moduleNames = ModuleUtil.getBkActualModuleNames(bkModuleName)

jooq {
    configurations {
        moduleNames.forEach { moduleName ->
            val databaseName = DatabaseUtil.getDatabaseName(moduleName, project.extra["DB_PREFIX"].toString())

            val specialModule = moduleNames.size != 1
            val taskName = if (specialModule) "${moduleName}Genenrate" else "genenrate"

            project.the<SourceSetContainer>()["main"].java {
                srcDir("build/generated-src/jooq/$taskName")
            }

            create(taskName) {
                jooqConfiguration.apply {
                    jdbc.apply {
                        var (mysqlURL, mysqlUser, mysqlPasswd) = DatabaseUtil.getMysqlInfo(
                            moduleName = moduleName,
                            defaultMysqlURL = project.extra["DB_HOST"]?.toString(),
                            defaultMysqlUser = project.extra["DB_USERNAME"]?.toString(),
                            defaultMysqlPasswd = project.extra["DB_PASSWORD"]?.toString()
                        )

                        println("moduleName : $moduleName")
                        println("mysqlURL : $mysqlURL")
                        println("mysqlUser : $mysqlUser")
                        println("mysqlPasswd : ${mysqlPasswd.substring(0, 3)}****")
                        val connectionMysqlURL = mysqlURL.split(",")[0]
                        driver = "com.mysql.cj.jdbc.Driver"
                        url = "jdbc:mysql://$connectionMysqlURL/$databaseName?useSSL=false"
                        user = mysqlUser
                        password = mysqlPasswd
                    }
                    generator.apply {
                        name = "org.jooq.codegen.DefaultGenerator"
                        database.apply {
                            name = "org.jooq.meta.mysql.MySQLDatabase"
                            inputSchema = databaseName
                            isUnsignedTypes = false
                            withIncludeRoutines(false) // 兼容"denied to user for table 'proc'"错误
                        }

                        strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"

                        generate.apply {
                            isRelations = false
                            isDeprecated = false
                            isFluentSetters = true
                            isGeneratedAnnotation = false
                            isJavaTimeTypes = true
                        }

                        target.apply {
                            packageName = "com.tencent.devops.model.${moduleName.replace("_", ".")}"
                        }
                    }
                }
            }
        }
    }
}

tasks.getByName("compileKotlin") {
    tasks.matching { it is JooqGenerate }.forEach {
        dependsOn(it.name)
    }
}
