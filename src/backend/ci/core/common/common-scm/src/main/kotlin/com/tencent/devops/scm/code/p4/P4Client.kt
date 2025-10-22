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

package com.tencent.devops.scm.code.p4

import com.perforce.p4java.client.IClient
import com.perforce.p4java.core.IChangelist
import com.perforce.p4java.core.file.FileSpecBuilder
import com.perforce.p4java.core.file.IFileSpec
import com.perforce.p4java.impl.generic.core.Changelist
import com.perforce.p4java.impl.mapbased.client.Client
import com.perforce.p4java.option.client.AddFilesOptions
import com.perforce.p4java.option.client.DeleteFilesOptions
import com.perforce.p4java.option.server.DeleteClientOptions
import com.perforce.p4java.server.IOptionsServer
import com.tencent.devops.scm.pojo.p4.Workspace
import org.apache.commons.io.FileUtils
import java.io.File

class P4Client(
    val server: IOptionsServer,
    val workspace: Workspace
) : AutoCloseable {

    private val client: IClient = clientLogin()

    private fun clientLogin(): Client {
        val client = with(workspace) {
            Client.newClient(
                server, // p4java server object
                name, // client name
                description, // client description
                root, // client root
                mappings.toTypedArray() // client mappings
            )
        }
        server.createClient(client)
        server.currentClient = client
        return client
    }

    fun addFile(desc: String, path: String): IFileSpec {
        if (!path.startsWith(client.root)) {
            throw IllegalArgumentException("file path not on workspace")
        }
        val (changelist, fileSpecs) = prepareFileOperation(desc, path)
        val opt = AddFilesOptions()
        opt.changelistId = changelist.id
        client.addFiles(fileSpecs, opt)
        // Submit changelist
        return changelist.submit(false)[0]
    }

    fun deleteFile(desc: String, path: String): IFileSpec {
        val (changelist, fileSpecs) = prepareFileOperation(desc, path)
        val opt = DeleteFilesOptions()
        opt.changelistId = changelist.id
        client.deleteFiles(fileSpecs, opt)
        // Submit changelist
        return changelist.submit(false)[0]
    }

    private fun prepareFileOperation(
        desc: String,
        path: String
    ): Pair<IChangelist, MutableList<IFileSpec>> {
        // Create changelist object
        val changeListImpl = Changelist.newChangelist(
            server, // Perforce server object
            client.name, // Client associated with this changelist
            desc // description for changelist
        )

        // Create changelist on server
        val changelist = client.createChangelist(changeListImpl)

        // Create a filespec for the file being added to changelist
        val fileSpecs = FileSpecBuilder.makeFileSpecList(path)
        return Pair(changelist, fileSpecs)
    }

    private fun deleteClient(name: String, isForce: Boolean = true): String {
        val deleteClientOptions = DeleteClientOptions()
        deleteClientOptions.isForce = isForce
        return server.deleteClient(name, deleteClientOptions)
    }

    override fun close() {
        deleteClient(workspace.name)
        if (File(workspace.root).exists()) {
            FileUtils.forceDelete(File(workspace.root))
        }
    }
}
