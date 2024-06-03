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

package com.tencent.devops.scm.code.p4

import com.perforce.p4java.admin.ITriggerEntry
import com.perforce.p4java.core.IDepot
import com.perforce.p4java.core.file.FileSpecBuilder
import com.perforce.p4java.core.file.IFileSpec
import com.perforce.p4java.core.file.IObliterateResult
import com.perforce.p4java.impl.generic.admin.TriggerEntry
import com.perforce.p4java.impl.generic.core.Depot
import com.perforce.p4java.impl.generic.core.file.FileSpec
import com.perforce.p4java.option.server.GetDepotFilesOptions
import com.perforce.p4java.option.server.ObliterateFilesOptions
import com.perforce.p4java.option.server.TrustOptions
import com.perforce.p4java.server.IOptionsServer
import com.perforce.p4java.server.ServerFactory.getOptionsServer
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.scm.pojo.p4.DepotInfo
import com.tencent.devops.scm.pojo.p4.TriggerInfo
import java.time.ZoneId
import java.util.Date

@SuppressWarnings("TooManyFunctions", "MagicNumber")
class P4Server(
    // p4java://localhost:1666"
    val p4port: String,
    val userName: String,
    val password: String
) : AutoCloseable {

    companion object {
        private const val MAX_CHANGE_LIST_FILES = 1000
    }

    private val server = if (p4port.startsWith("ssl:")) {
        getOptionsServer("p4javassl://${p4port.substring(4)}", null)
    } else {
        getOptionsServer("p4java://$p4port", null)
    }

    fun connectionRetry() {
        return RetryUtils.execute(action = object : RetryUtils.Action<Unit> {
            override fun execute() {
                if (p4port.startsWith("ssl:")) {
                    server.addTrust(TrustOptions().setAutoAccept(true))
                }
                server.connect()
                server.userName = userName
                server.login(password, true)
            }
        }, retryTime = 3)
    }

    fun getServer(): IOptionsServer {
        return server
    }

    fun disconnect() {
        server.disconnect()
    }

    fun addTriggers(triggers: List<TriggerInfo>): String {
        val entryList = triggers.map { TriggerEntry(it.toString(), it.order) }.toList()
        return server.createTriggerEntries(entryList)
    }

    fun updateTriggers(triggers: List<TriggerInfo>): String {
        val entryList = triggers.map { TriggerEntry(it.toString(), it.order) }.toList()
        return server.updateTriggerEntries(entryList)
    }

    fun getTriggers(): List<ITriggerEntry> {
        return server.triggerEntries
    }

    fun listFiles(depotName: String): List<IFileSpec> {
        return server.getDepotFiles(
            FileSpecBuilder.makeFileSpecList("//$depotName/..."), GetDepotFilesOptions()
        ).toList()
    }

    fun obliterateFiles(file: String): List<IObliterateResult> {
        val opt = ObliterateFilesOptions()
        val fileSpec = FileSpecBuilder.makeFileSpecList(file)
        return server.obliterateFiles(fileSpec, opt.setExecuteObliterate(true))
    }

    fun createDepot(depotInfo: DepotInfo): String {
        with(depotInfo) {
            // Create depot object
            val depot = Depot(
                name, // depotName
                userName, // depotUser
                Date.from(modDate.atZone(ZoneId.systemDefault()).toInstant()), // modtime
                description, // depotDescription
                depotType, // depotType
                address, // address
                suffix, // suffix
                streamDepth, // streamDepth
                depotMap, // depotMap
                specMap // specMap
            )
            // Create depot on perforce server
            return server.createDepot(depot)
        }
    }

    fun deleteDepot(name: String): String {
        return server.deleteDepot(name)
    }

    fun getDepot(name: String): IDepot? {
        return server.getDepot(name)
    }

    fun getChangelistFiles(change: Int): List<IFileSpec> {
        return server.getChangelistFiles(change, MAX_CHANGE_LIST_FILES)
    }

    fun getShelvedFiles(change: Int): List<IFileSpec> {
        return server.getShelvedFiles(change, MAX_CHANGE_LIST_FILES)
    }

    fun getFileContent(filePath: String, reversion: Int): String {
        val fileSpec = FileSpec(filePath)
        fileSpec.startRevision = reversion
        fileSpec.endRevision = reversion
        return server.getFileContents(listOf(fileSpec), false, true)
            .bufferedReader().use { it.readText() }
    }

    override fun close() {
        disconnect()
    }
}
