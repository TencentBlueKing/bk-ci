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

package com.tencent.bkrepo.rpm.util.rpm

import com.google.common.collect.Lists
import com.tencent.bkrepo.rpm.util.redline.model.RpmFormat
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmChangeLog
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmChecksum
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmEntry
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmFile
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmHeaderRange
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmLocation
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmMetadata
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmPackage
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmSize
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmTime
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmVersion
import org.apache.commons.lang.StringUtils
import org.redline_rpm.header.AbstractHeader
import org.redline_rpm.header.Flags
import org.redline_rpm.header.Header
import org.redline_rpm.header.RpmType
import org.redline_rpm.header.Signature
import java.util.LinkedList

object RpmMetadataUtils {

    fun interpret(rawFormat: RpmFormat, size: Long, checkSum: String, href: String): RpmMetadata {
        val header: Header = rawFormat.format.header
        val signature: Signature = rawFormat.format.signature
        return RpmMetadata(
            listOf(
                RpmPackage(
                    "rpm",
                    getName(header)!!,
                    if (rawFormat.type == RpmType.SOURCE) {
                        "src"
                    } else {
                        getArchitecture(header)!!
                    },
                    RpmVersion(getEpoch(header), getVersion(header)!!, getRelease(header)!!),
                    "pkgid",
                    RpmChecksum(checkSum),
                    getSummary(header),
                    getDescription(header),
                    getPackager(header),
                    getUrl(header),
                    RpmTime(System.currentTimeMillis(), getBuildTime(header)),
                    RpmSize(size, getInstalledSize(header), getArchiveSize(signature)),
                    RpmLocation(href),
                    com.tencent.bkrepo.rpm.util.xStream.pojo.RpmFormat(
                        getLicense(header),
                        getVendor(header),
                        getGroup(header)!!,
                        getBuildHost(header)!!,
                        getSourceRpm(header)!!,
                        RpmHeaderRange(rawFormat.headerStart, rawFormat.headerEnd),
                        resolveEntriesEntries(
                            header,
                            Header.HeaderTag.PROVIDENAME,
                            Header.HeaderTag.PROVIDEFLAGS,
                            Header.HeaderTag.PROVIDEVERSION
                        ),
                        resolveEntriesEntries(
                            header,
                            Header.HeaderTag.REQUIRENAME,
                            Header.HeaderTag.REQUIREFLAGS,
                            Header.HeaderTag.REQUIREVERSION
                        ),
                        resolveEntriesEntries(
                            header,
                            Header.HeaderTag.CONFLICTNAME,
                            Header.HeaderTag.CONFLICTFLAGS,
                            Header.HeaderTag.CONFLICTVERSION
                        ),
                        resolveEntriesEntries(
                            header,
                            Header.HeaderTag.OBSOLETENAME,
                            Header.HeaderTag.OBSOLETEFLAGS,
                            Header.HeaderTag.OBSOLETEVERSION
                        ),
                        resolveFiles(header),
                        resolveChangeLogs(header)
                    )
                )
            ),
            1L
        )
    }

    private fun getName(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.NAME)
    }

    private fun getArchitecture(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.ARCH)
    }

    private fun getVersion(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.VERSION)
    }

    private fun getEpoch(header: Header): Int {
        return getIntHeader(header, Header.HeaderTag.EPOCH)
    }

    private fun getRelease(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.RELEASE)
    }

    private fun getSummary(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.SUMMARY)
    }

    private fun getDescription(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.DESCRIPTION)
    }

    private fun getPackager(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.PACKAGER)
    }

    private fun getUrl(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.URL)
    }

    private fun getBuildTime(header: Header): Int {
        return getIntHeader(header, Header.HeaderTag.BUILDTIME)
    }

    private fun getInstalledSize(header: Header): Int {
        return getIntHeader(header, Header.HeaderTag.SIZE)
    }

    private fun getArchiveSize(signature: Signature): Int {
        return getIntHeader(signature, Signature.SignatureTag.PAYLOADSIZE)
    }

    private fun getLicense(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.LICENSE)
    }

    private fun getVendor(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.VENDOR)
    }

    private fun getGroup(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.GROUP)
    }

    private fun getSourceRpm(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.SOURCERPM)
    }

    private fun getBuildHost(header: Header): String? {
        return getStringHeader(header, Header.HeaderTag.BUILDHOST)
    }

    private fun resolveEntriesEntries(
        header: Header,
        namesTag: Header.HeaderTag,
        flagsTag: Header.HeaderTag,
        versionsTag: Header.HeaderTag
    ): LinkedList<RpmEntry> {
        val entries: LinkedList<RpmEntry> = Lists.newLinkedList()
        val entryNames = getStringArrayHeader(header, namesTag)
        val entryFlags = getIntArrayHeader(header, flagsTag)
        val entryVersions = getStringArrayHeader(header, versionsTag)
        for (i in entryNames.indices) {
            val entryName = entryNames[i]
            val rpmEntry = RpmEntry(
                entryName
            )
            if (entryFlags.size > i) {
                val entryFlag = entryFlags[i]
                setEntryFlags(entryFlag, rpmEntry)
                if (entryFlag and Flags.PREREQ > 0) {
                    rpmEntry.pre = "1"
                }
            }
            if (entryVersions.size > i) {
                setEntryVersionFields(entryVersions[i], rpmEntry)
            }
            entries.add(rpmEntry)
        }
        return entries
    }

    private fun setEntryFlags(entryFlags: Int, rpmEntry: RpmEntry): Int {
        if (entryFlags and Flags.LESS > 0 && entryFlags and Flags.EQUAL > 0) {
            rpmEntry.flags = "LE"
        } else if (entryFlags and Flags.GREATER > 0 && entryFlags and Flags.EQUAL > 0) {
            rpmEntry.flags = "GE"
        } else if (entryFlags and Flags.EQUAL > 0) {
            rpmEntry.flags = "EQ"
        } else if (entryFlags and Flags.LESS > 0) {
            rpmEntry.flags = "LT"
        } else if (entryFlags and Flags.GREATER > 0) {
            rpmEntry.flags = "GT"
        }
        return entryFlags
    }

    private fun setEntryVersionFields(entryVersion: String?, rpmEntry: RpmEntry) {
        if (StringUtils.isNotBlank(entryVersion)) {
            val versionTokens: Array<String> = StringUtils.split(entryVersion, '-')
            val versionValue = versionTokens[0]
            val versionValueTokens: Array<String> = StringUtils.split(versionValue, ':')
            if (versionValueTokens.size > 1) {
                rpmEntry.epoch = versionValueTokens[0]
                rpmEntry.ver = versionValueTokens[1]
            } else {
                rpmEntry.epoch = "0"
                rpmEntry.ver = versionValueTokens[0]
            }
            if (versionTokens.size > 1) {
                val releaseValue = versionTokens[1]
                if (StringUtils.isNotBlank(releaseValue)) {
                    rpmEntry.rel = releaseValue
                }
            }
        }
    }

    private fun resolveFiles(header: Header): LinkedList<RpmFile> {
        val files: LinkedList<RpmFile> = Lists.newLinkedList()
        val baseNames = getStringArrayHeader(header, Header.HeaderTag.BASENAMES)
        val baseNameDirIndexes = getIntArrayHeader(header, Header.HeaderTag.DIRINDEXES)

        val dirPaths = getStringArrayHeader(header, Header.HeaderTag.DIRNAMES)
        for (i in baseNames.indices) {
            val baseName = baseNames[i]
            val baseNameDirIndex = baseNameDirIndexes[i]
            val filePath = dirPaths[baseNameDirIndex] + baseName
            val dir = dirPaths.contains("$filePath/")
            val file = if (dir) RpmFile("dir", filePath) else RpmFile(null, filePath)
            files.add(file)
        }
        return files
    }

    private fun resolveChangeLogs(header: Header): LinkedList<RpmChangeLog> {
        val changeLogs: LinkedList<RpmChangeLog> = Lists.newLinkedList()
        val changeLogAuthors = getStringArrayHeader(header, Header.HeaderTag.CHANGELOGNAME)
        val changeLogDates = getIntArrayHeader(header, Header.HeaderTag.CHANGELOGTIME)
        val changeLogTexts = getStringArrayHeader(header, Header.HeaderTag.CHANGELOGTEXT)
        for (i in changeLogTexts.indices) {
            val changeLog = RpmChangeLog(
                changeLogAuthors[i],
                changeLogDates[i],
                changeLogTexts[i]
            )
            changeLogs.add(changeLog)
        }
        return changeLogs
    }

    private fun getStringHeader(header: AbstractHeader, tag: AbstractHeader.Tag): String? {
        val values = getStringArrayHeader(header, tag)
        return if (values.isEmpty()) {
            null
        } else values[0]
    }

    private fun getStringArrayHeader(header: AbstractHeader, tag: AbstractHeader.Tag): Array<String?> {
        val entry = header.getEntry(tag) ?: return arrayOfNulls(0)
        return entry.values as Array<String?>
    }

    private fun getIntHeader(header: AbstractHeader, tag: AbstractHeader.Tag): Int {
        val values = getIntArrayHeader(header, tag)
        return if (values.isEmpty()) {
            0
        } else values[0]
    }

    private fun getIntArrayHeader(header: AbstractHeader, tag: AbstractHeader.Tag): IntArray {
        val entry = header.getEntry(tag) ?: return IntArray(0)
        return entry.values as IntArray
    }
}
