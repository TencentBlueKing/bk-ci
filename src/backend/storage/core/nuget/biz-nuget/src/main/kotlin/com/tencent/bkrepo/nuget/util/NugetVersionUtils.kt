package com.tencent.bkrepo.nuget.util

import com.github.zafarkhaja.semver.Version
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

object NugetVersionUtils {
    private val logger = LoggerFactory.getLogger(NugetVersionUtils::class.java)

    fun compareSemVer(v1: String, v2: String): Int {
        val left = parseSemVer2(v1)
        val right = parseSemVer2(v2)
        return if (left != null && right != null) {
            left.compareWithBuildsTo(right)
        } else if (left == null && right == null) {
            v1.compareTo(v2)
        } else {
            left?.let { 1 } ?: -1
        }
    }

    private fun parseSemVer2(version: String): Version? {
        return try {
            Version.valueOf(version)
        } catch (ignored: Exception) {
            logger.trace("Failed to parse version $version as server2, trying to adjust version to server2.")
            val eliminateZeros = eliminateZeros(version)
            eliminateZeros ?: getVersionCleanExtraDots(version)
        }
    }

    private fun getVersionCleanExtraDots(version: String): Version? {
        val i = StringUtils.ordinalIndexOf(version, ".", 3)
        return if (i == -1) {
            logger.trace("Failed to adjust version [$version] to semver2.")
            null
        } else {
            val adjustedToSemVer2 = version.substring(0, i) + "+" + version.substring(i + 1)
            try {
                Version.valueOf(adjustedToSemVer2)
            } catch (e: Exception) {
                logger.trace("Failed to parse version $version as server2.")
                null
            }
        }
    }

    private fun eliminateZeros(version: String): Version? {
        val split = version.split(".")
        return if (split.size < 3) {
            null
        } else {
            val finalVersion =
                StringBuffer().append(trimLeadingZeros(split[0])).append('.').append(trimLeadingZeros(split[1]))
                    .append('.').append(trimLeadingZeros(split[2])).append(resolveSuffix(version)).toString()
            // exception
            try {
                Version.valueOf(finalVersion)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun resolveSuffix(version: String): String {
        val suffixIndex = StringUtils.ordinalIndexOf(version, ".", 3)
        var suffix = ""
        if (suffixIndex != -1) {
            suffix = version.substring(suffixIndex)
        }

        return suffix
    }

    private fun trimLeadingZeros(chunk: String): String {
        return if (chunk.length == 1) {
            chunk
        } else {
            var i = 0
            while (i < chunk.length - 1 && chunk[i] == '0') {
                ++i
            }
            chunk.substring(i)
        }
    }
}
