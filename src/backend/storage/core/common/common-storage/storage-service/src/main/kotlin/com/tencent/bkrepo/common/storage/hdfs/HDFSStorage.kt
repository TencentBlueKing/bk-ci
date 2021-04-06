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

package com.tencent.bkrepo.common.storage.hdfs

import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.bound
import com.tencent.bkrepo.common.storage.core.AbstractFileStorage
import com.tencent.bkrepo.common.storage.credentials.HDFSCredentials
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import java.io.File
import java.io.InputStream
import java.net.URI

open class HDFSStorage : AbstractFileStorage<HDFSCredentials, HDFSClient>() {

    override fun store(path: String, name: String, file: File, client: HDFSClient) {
        val localPath = Path(file.absolutePath)
        val remotePath = concatRemotePath(path, name, client)
        client.fileSystem.copyFromLocalFile(localPath, remotePath)
    }

    override fun store(path: String, name: String, inputStream: InputStream, size: Long, client: HDFSClient) {
        val remotePath = concatRemotePath(path, name, client)
        val outputStream = client.fileSystem.create(remotePath, true)
        outputStream.use { inputStream.copyTo(outputStream) }
    }

    override fun load(path: String, name: String, range: Range, client: HDFSClient): InputStream? {
        val remotePath = concatRemotePath(path, name, client)
        val inputStream = client.fileSystem.open(remotePath)
        return inputStream.apply { seek(range.start) }.bound(range)
    }

    override fun delete(path: String, name: String, client: HDFSClient) {
        val remotePath = concatRemotePath(path, name, client)
        if (client.fileSystem.exists(remotePath)) {
            client.fileSystem.delete(remotePath, false)
        }
    }

    override fun exist(path: String, name: String, client: HDFSClient): Boolean {
        val remotePath = concatRemotePath(path, name, client)
        return client.fileSystem.exists(remotePath)
    }

    private fun concatRemotePath(path: String, filename: String, client: HDFSClient): Path {
        val childPath = Path(path, filename)
        val parentPath = client.workingPath
        return Path.mergePaths(parentPath, childPath)
    }

    override fun onCreateClient(credentials: HDFSCredentials): HDFSClient {
        val configuration = Configuration()
        val username = credentials.user
        var url = credentials.url
        val workingPath = Path(URI.create(credentials.workingDirectory))
        if (credentials.clusterMode) {
            url = "hdfs://${credentials.clusterName}"
            configuration["fs.defaultFS"] = url
            configuration["dfs.replication"] = 2.toString()
            configuration["dfs.nameservices"] = credentials.clusterName
            configuration["dfs.ha.namenodes.${credentials.clusterName}"] =
                credentials.nameNodeMap.keys.joinToString(separator = ",")
            credentials.nameNodeMap.forEach { (node, address) ->
                configuration["dfs.namenode.rpc-address.${credentials.clusterName}.$node"] = address
            }
            configuration["dfs.client.failover.proxy.provider.${credentials.clusterName}"] =
                "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"
        }
        val fileSystem = FileSystem.get(URI.create(url), configuration, username)
        return HDFSClient(workingPath, fileSystem)
    }
}
