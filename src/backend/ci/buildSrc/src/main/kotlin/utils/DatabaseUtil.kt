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

package utils

object DatabaseUtil {

    fun getMysqlInfo(
        moduleName: String,
        defaultMysqlURL: String?,
        defaultMysqlUser: String?,
        defaultMysqlPasswd: String?
    ): Triple<String, String, String> {
        var mysqlURL = getMysqlParamValue(moduleName, "mysqlURL")
        var mysqlUser = getMysqlParamValue(moduleName, "mysqlUser")
        var mysqlPasswd = getMysqlParamValue(moduleName, "mysqlPasswd")
        if (mysqlURL == null || mysqlUser == null || mysqlPasswd == null) {
            println("use default properties.")
            mysqlURL = defaultMysqlURL ?: ""
            mysqlUser = defaultMysqlUser ?: ""
            mysqlPasswd = defaultMysqlPasswd ?: ""
        }
        return Triple(mysqlURL, mysqlUser, mysqlPasswd)
    }

    fun getDatabaseName(
        moduleName: String,
        defaultMysqlPrefixName: String
    ): String {
        val mysqlPrefix: String? = System.getProperty("mysqlPrefix") ?: System.getenv("mysqlPrefix")
        return if (mysqlPrefix != null && mysqlPrefix != "") {
            println("jooq build env : $mysqlPrefix")
            mysqlPrefix + moduleName
        } else {
            "$defaultMysqlPrefixName$moduleName"
        }
    }

    private fun getMysqlParamValue(
        moduleName: String,
        paramName: String
    ): String? {
        val firstCharacter = paramName[0]
        val moduleParamName = "${moduleName}${paramName.replaceFirst(firstCharacter, firstCharacter.toUpperCase())}"
        var paramValue = System.getenv(moduleParamName) ?: System.getProperty(moduleParamName)
        if (paramValue == null) {
            paramValue = System.getProperty(paramName) ?: System.getenv(paramName)
        }
        return paramValue
    }
}
