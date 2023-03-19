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

package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.CertAndroidInfo
import com.tencent.devops.ticket.pojo.CertEnterpriseInfo
import com.tencent.devops.ticket.pojo.CertIOSInfo
import com.tencent.devops.ticket.pojo.CertTlsInfo
import com.tencent.devops.ticket.pojo.CertIOS
import com.tencent.devops.ticket.pojo.CertWithPermission
import com.tencent.devops.ticket.pojo.CertEnterprise
import com.tencent.devops.ticket.pojo.CertAndroid
import com.tencent.devops.ticket.pojo.CertAndroidWithCredential
import com.tencent.devops.ticket.pojo.CertTls
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64

@Suppress("ALL")
interface CertService {

    fun uploadIos(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        certCredentialId: String?,
        p12InputStream: InputStream,
        p12Disposition: FormDataContentDisposition,
        mpInputStream: InputStream,
        mpDisposition: FormDataContentDisposition
    )

    fun updateIos(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        certCredentialId: String?,
        p12InputStream: InputStream?,
        p12Disposition: FormDataContentDisposition?,
        mpInputStream: InputStream?,
        mpDisposition: FormDataContentDisposition?
    )

    fun uploadEnterprise(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        mpInputStream: InputStream,
        mpDisposition: FormDataContentDisposition
    )

    fun updateEnterprise(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        mpInputStream: InputStream?,
        mpDisposition: FormDataContentDisposition?
    )

    fun uploadAndroid(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        credentialId: String,
        alias: String,
        aliasCredentialId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    )

    fun updateAndroid(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        credentialId: String,
        alias: String,
        aliasCredentialId: String,
        inputStream: InputStream?,
        disposition: FormDataContentDisposition?
    )

    fun uploadTls(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        serverCrtInputStream: InputStream,
        serverCrtDisposition: FormDataContentDisposition,
        serverKeyInputStream: InputStream,
        serverKeyDisposition: FormDataContentDisposition,
        clientCrtInputStream: InputStream?,
        clientCrtDisposition: FormDataContentDisposition?,
        clientKeyInputStream: InputStream?,
        clientKeyDisposition: FormDataContentDisposition?
    )

    fun updateTls(
        userId: String,
        projectId: String,
        certId: String,
        certRemark: String?,
        serverCrtInputStream: InputStream?,
        serverCrtDisposition: FormDataContentDisposition?,
        serverKeyInputStream: InputStream?,
        serverKeyDisposition: FormDataContentDisposition?,
        clientCrtInputStream: InputStream?,
        clientCrtDisposition: FormDataContentDisposition?,
        clientKeyInputStream: InputStream?,
        clientKeyDisposition: FormDataContentDisposition?
    )

    fun delete(userId: String, projectId: String, certId: String)

    fun list(
        userId: String,
        projectId: String,
        certType: String?,
        offset: Int,
        limit: Int
    ): SQLPage<CertWithPermission>

    fun list(
        projectId: String,
        offset: Int,
        limit: Int
    ): SQLPage<Cert>

    fun hasPermissionList(
        userId: String,
        projectId: String,
        certType: String?,
        authPermission: AuthPermission,
        offset: Int,
        limit: Int
    ): SQLPage<Cert>

    fun getIos(userId: String, projectId: String, certId: String): CertIOSInfo

    fun getEnterprise(projectId: String, certId: String): CertEnterpriseInfo

    fun getAndroid(userId: String, projectId: String, certId: String): CertAndroidInfo

    fun getTls(projectId: String, certId: String): CertTlsInfo

    fun queryIos(projectId: String, buildId: String, certId: String, publicKey: String): CertIOS

    fun queryEnterprise(
        projectId: String,
        buildId: String,
        certId: String,
        publicKey: String
    ): CertEnterprise

    fun queryEnterpriseByProject(projectId: String, certId: String, publicKey: String): CertEnterprise

    fun queryAndroid(projectId: String, buildId: String, certId: String, publicKey: String): CertAndroid

    fun queryAndroidByProject(
        projectId: String,
        certId: String,
        publicKey: String
    ): CertAndroidWithCredential

    fun queryTlsByProject(projectId: String, certId: String, publicKey: String): CertTls

    fun getCertByIds(certIds: Set<String>): List<Cert>?

    fun searchByCertId(
        projectId: String,
        offset: Int,
        limit: Int,
        certId: String
    ): SQLPage<Cert>

    private fun encryptCert(
        cert: ByteArray,
        publicKeyByteArray: ByteArray,
        serverPrivateKeyByteArray: ByteArray
    ): String {
        val certEncryptedContent = DHUtil.encrypt(cert, publicKeyByteArray, serverPrivateKeyByteArray)
        return String(Base64.getEncoder().encode(certEncryptedContent))
    }

    private fun read(inputStream: InputStream): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream.copyTo(byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CertService::class.java)
    }
}
