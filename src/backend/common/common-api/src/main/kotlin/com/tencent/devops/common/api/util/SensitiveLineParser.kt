package com.tencent.devops.common.api.util

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object SensitiveLineParser {
    private val pattern = Pattern.compile("oauth2:(\\w+)@")
    private val patternPassword = Pattern.compile("http://.*:.*@")

    fun onParseLine(line: String): String {
        if (line.contains("http://oauth2:")) {
            val matcher = pattern.matcher(line)
            val replace = matcher.replaceAll("oauth2:***@")
            logger.info("Parse the line from $line to $replace")
            return replace
        }
        if (line.contains("http://")) {
            return patternPassword.matcher(line).replaceAll("http://***:***@")
        }
        return line
    }

    private val logger = LoggerFactory.getLogger(SensitiveLineParser::class.java)
}
