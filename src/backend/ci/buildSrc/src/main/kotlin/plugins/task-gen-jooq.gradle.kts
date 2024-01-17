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
import nu.studer.gradle.jooq.JooqGenerate

val commonScriptUrl = javaClass.getResource("/common.gradle.kts")
apply(from = commonScriptUrl)

plugins {
    id("nu.studer.jooq")
}

val jooqGenerator by configurations
val api by configurations

dependencies {
    jooqGenerator("mysql:mysql-connector-java:8.0.28")
    api("org.jooq:jooq")
}

val getDatabaseName = extra["getDatabaseName"] as (String) -> String
val getBkModuleName = extra["getBkModuleName"] as () -> String
val getBkActualModuleNames = extra["getBkActualModuleNames"] as (String) -> List<String>
var moduleNames = getBkActualModuleNames(getBkModuleName())

jooq {
    configurations {
        moduleNames.forEach { moduleName ->
            val databaseName = getDatabaseName(moduleName)

            val specialModule = moduleNames.size != 1
            val taskName = if (specialModule) "${moduleName}Genenrate" else "genenrate"

            project.the<SourceSetContainer>()["main"].java {
                srcDir("build/generated-src/jooq/$taskName")
            }

            create(taskName) {
                jooqConfiguration.apply {
                    jdbc.apply {
                        val getMysqlInfo = extra["getMysqlInfo"] as (String) -> Triple<String, String, String>
                        var (mysqlURL, mysqlUser, mysqlPasswd) = getMysqlInfo(moduleName)

                        println("moduleName : $moduleName")
                        println("mysqlURL : $mysqlURL")
                        println("mysqlUser : $mysqlUser")
                        println("mysqlPasswd : ${mysqlPasswd.substring(0, 3)}****")

                        driver = "com.mysql.cj.jdbc.Driver"
                        url = "jdbc:mysql://$mysqlURL/$databaseName?useSSL=false"
                        user = mysqlUser
                        password = mysqlPasswd
                    }
                    generator.apply {
                        name = "org.jooq.codegen.DefaultGenerator"
                        database.apply {
                            name = "org.jooq.meta.mysql.MySQLDatabase"
                            inputSchema = databaseName
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

tasks.getByName<AbstractCompile>("compileKotlin") {
    tasks.matching { it is JooqGenerate }.forEach {
        dependsOn(it.name)
    }
}
