package com.tencent.devops.ticket.util

import org.dom4j.DocumentHelper
import org.dom4j.Element
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.regex.Pattern

/**
 * Created by Aaron Sheng on 2017/11/26.
 */
object MobileProvisionUtil {
    private val pattern = Pattern.compile("<plist[\\s\\S]*</plist>")
    private val Q_NAME_DICT = "dict"
    private val KEY_EXPIRATION_DATE = "ExpirationDate"
    private val KEY_NAME = "Name"
    private val KEY_UUID = "UUID"
    private val KEY_TEAM_NAME = "TeamName"

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
