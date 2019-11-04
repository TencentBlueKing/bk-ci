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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ticket.util

import org.dom4j.DocumentHelper
import org.dom4j.Element
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.regex.Pattern

object MobileProvisionUtil {
    private val pattern = Pattern.compile("<plist[\\s\\S]*</plist>")
    private const val Q_NAME_DICT = "dict"
    private const val KEY_EXPIRATION_DATE = "ExpirationDate"
    private const val KEY_NAME = "Name"
    private const val KEY_UUID = "UUID"
    private const val KEY_TEAM_NAME = "TeamName"

    fun parse(mpContent: ByteArray): MobileProvisionInfo? {
        val plistContent = getPlistFromMobileProvision(mpContent)
        return if (plistContent == null) {
            null
        } else {
            getMobileProvisionFromPlist(plistContent)
        }
    }

    private fun getMobileProvisionFromPlist(plistContent: String): MobileProvisionInfo? {
        var expirationDate: LocalDateTime? = null
        var name: String? = null
        var uuid: String? = null
        var teamName: String? = null

        val document = DocumentHelper.parseText(plistContent)
        val rootElement = document.rootElement
        val rootItr = rootElement.elementIterator(Q_NAME_DICT)
        val dictRootElement = rootItr.next() as Element
        val dictItr = dictRootElement.elementIterator()
        while (dictItr.hasNext()) {
            val keyElement = dictItr.next() as Element
            val dataElement = dictItr.next() as Element
            when (keyElement.data) {
                KEY_EXPIRATION_DATE -> expirationDate = parseLocalDateTime(dataElement.data as String)
                KEY_NAME -> name = dataElement.data as String
                KEY_UUID -> uuid = dataElement.data as String
                KEY_TEAM_NAME -> teamName = dataElement.data as String
            }
        }

        return if (expirationDate == null || name == null || uuid == null || teamName == null) {
            null
        } else {
            MobileProvisionInfo(expirationDate, name, uuid, teamName)
        }
    }

    private fun getPlistFromMobileProvision(content: ByteArray): String? {
        val str = String(content, StandardCharsets.UTF_8)
        val matcher = pattern.matcher(str)
        return if (!matcher.find()) {
            null
        } else {
            str.substring(matcher.start(), matcher.end())
        }
    }

    private fun parseLocalDateTime(date: String): LocalDateTime {
        val dateWithoutZone = date.replace("z", "", ignoreCase = true)
        return LocalDateTime.parse(dateWithoutZone)
    }
}
