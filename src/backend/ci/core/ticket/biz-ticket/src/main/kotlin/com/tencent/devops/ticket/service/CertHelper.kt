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

import com.tencent.devops.common.api.exception.EncryptException
import com.tencent.devops.common.api.util.AESUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.naming.InvalidNameException
import javax.naming.ldap.LdapName
import javax.naming.ldap.Rdn
import javax.security.auth.x500.X500Principal

@Component
class CertHelper {

    @Value("\${cert.aes-key}")
    private val aesKey = "d/R%3{?OS}IeGT21"

    companion object {
        private const val JKS = "JKS"
    }

    @Suppress("ALL")
    fun parseJks(byteArray: ByteArray, password: String, alias: String, aliasPassword: String): JksInfo {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val keystore = KeyStore.getInstance(JKS)

        try {
            keystore.load(byteArrayInputStream, password.toCharArray())
        } catch (e: Exception) {
            throw EncryptException("Keystore file invalid or wrong password", e)
        }

        try {
            val entry = keystore.getEntry(
                alias,
                KeyStore.PasswordProtection(aliasPassword.toCharArray())
            ) as KeyStore.PrivateKeyEntry
            val certificate = entry.certificateChain[0] as X509Certificate

            val dn = certificate.issuerX500Principal as X500Principal
            val country = dn.getCountry()
            val state = dn.getState()
            val locality = dn.getLocality()
            val organization = dn.getOrganization()
            val organizationalUnit = dn.getOrganizationalUnit()
            val commonName = dn.getCommonName()
            val version = certificate.version
            val issue = LocalDateTime.ofInstant(certificate.notBefore.toInstant(), ZoneId.systemDefault())
            val expire = LocalDateTime.ofInstant(certificate.notAfter.toInstant(), ZoneId.systemDefault())
            return JksInfo(
                country = country,
                state = state,
                locality = locality,
                organization = organization,
                organizationalUnit = organizationalUnit,
                commonName = commonName,
                version = version,
                issueDate = issue,
                expireDate = expire
            )
        } catch (e: TypeCastException) {
            throw EncryptException("Keystore alis not exist", e)
        } catch (e: UnrecoverableKeyException) {
            throw EncryptException("Keystore wrong alias password", e)
        }
    }

    fun validJksPassword(byteArray: ByteArray, password: String): Boolean {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val keystore = KeyStore.getInstance(JKS)

        byteArrayInputStream.use {
            try {
                keystore.load(byteArrayInputStream, password.toCharArray())
                return true
            } catch (ignored: Exception) {
            }
        }
        return false
    }

    fun validJksAlias(byteArray: ByteArray, password: String, alias: String, aliasPassword: String): Boolean {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val keystore = KeyStore.getInstance(JKS)

        byteArrayInputStream.use {
            try {
                keystore.load(byteArrayInputStream, password.toCharArray())
            } catch (ignored: Exception) {
                throw EncryptException("Keystore file invalid or wrong password", ignored)
            }

            try {
                keystore.getEntry(
                    alias,
                    KeyStore.PasswordProtection(aliasPassword.toCharArray())
                ) as KeyStore.PrivateKeyEntry
                return true
            } catch (ignored: Exception) {
            }
        }
        return false
    }

    fun encryptBytes(bytes: ByteArray?): ByteArray? {
        return if (bytes != null) {
            AESUtil.encrypt(aesKey, bytes)
        } else null
    }

    fun decryptBytes(bytes: ByteArray?): ByteArray? {
        return if (bytes != null) {
            AESUtil.decrypt(aesKey, bytes)
        } else null
    }

    data class JksInfo(
        val country: String?,
        val state: String?,
        val locality: String?,
        val organization: String?,
        val organizationalUnit: String?,
        val commonName: String?,
        val version: Int,
        val issueDate: LocalDateTime,
        val expireDate: LocalDateTime
    )

    private fun getAttributes(principal: X500Principal): Map<String, String> {
        val attributes: MutableMap<String, String> = HashMap()
        try {
            val ldapName = LdapName(principal.name)
            val rdns: List<Rdn> = ldapName.rdns
            for (rdn in rdns) {
                attributes[rdn.type] = rdn.value.toString()
            }
        } catch (e: InvalidNameException) {
            e.printStackTrace()
        }
        return attributes
    }

    private fun X500Principal.getCountry(): String? {
        return getAttributes(this)["C"]
    }

    private fun X500Principal.getState(): String? {
        return getAttributes(this)["ST"]
    }

    private fun X500Principal.getLocality(): String? {
        return getAttributes(this)["L"]
    }

    private fun X500Principal.getOrganization(): String? {
        return getAttributes(this)["O"]
    }

    private fun X500Principal.getOrganizationalUnit(): String? {
        return getAttributes(this)["OU"]
    }

    private fun X500Principal.getCommonName(): String? {
        return getAttributes(this)["CN"]
    }
}
