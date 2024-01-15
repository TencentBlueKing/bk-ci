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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.environment.dao.job.NetworkAreaDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.UnknownHostException

@Service("AutoChooseAgentInstallChannelIdService")
class ChooseAgentInstallChannelIdService @Autowired constructor(
    private val dslContext: DSLContext,
    private val networkAreaDao: NetworkAreaDao
) {
    fun autoChooseAgentInstallChannelId(ip: String): Int {
        if (ip.isBlank()) {
            throw ParamBlankException(
                message = "innerIp or innerIpv6 must be selected."
            )
        }
        val networkAreaRecord = networkAreaDao.getNetworkAreaList(dslContext)
        val networkAreaMap = networkAreaRecord.map {
            it.netArea to it.netSegment.split(",")
        }.toMap()
        var networkArea = "SUPPORTING"
        networkAreaMap.forEach { (key, value) ->
            if (ipInRange(ip, value)) {
                networkArea = key
                return@forEach
            }
        }
        return when (networkArea) {
            "SUPPORTING" -> 4
            "OSS" -> 5
            "DEVNET" -> 6
            else -> -1 // 实际走不到，5 6匹配不到就4了
        }
    }

    private fun ipInRange(ip: String, ranges: List<String>): Boolean {
        val ipAddr: InetAddress = try {
            InetAddress.getByName(ip)
        } catch (e: UnknownHostException) {
            return false
        }
        for (range in ranges) {
            val parts = range.split("/")
            val subnet = parts[0]
            val subnetAddr: InetAddress = try {
                InetAddress.getByName(subnet)
            } catch (e: UnknownHostException) {
                return false
            }
            val prefixLength = parts[1].toInt()
            val subnetMask = 0xffffffff shl (32 - prefixLength)
            val subnetBytes = (subnetAddr.address.map { it.toInt() and 0xff }).toIntArray()
            val ipBytes = (ipAddr.address.map { it.toInt() and 0xff }).toIntArray()
            var maskIndex = 0
            while (maskIndex < 4 && ipBytes[maskIndex] and subnetMask.toInt() == subnetBytes[maskIndex]) {
                maskIndex++
            }
            if (maskIndex == 4) {
                return true
            }
        }
        return false
    }
}