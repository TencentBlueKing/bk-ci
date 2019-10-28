package com.tencent.devops.ticket.util

import sun.security.x509.X500Name
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Aaron Sheng on 2018/5/6.
 */
object CertUtil {
    private val JKS = "JKS"

    fun parseJks(byteArray: ByteArray, password: String, alias: String, aliasPassword: String): JksInfo {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val keystore = KeyStore.getInstance(JKS)

        try {
            keystore.load(byteArrayInputStream, password.toCharArray())
        } catch (e: Exception) {
            throw RuntimeException("Keystore file invalid or wrong password")
        }

        try {
            val entry = keystore.getEntry(alias, KeyStore.PasswordProtection(aliasPassword.toCharArray())) as KeyStore.PrivateKeyEntry
            val certificate = entry.certificateChain[0] as X509Certificate

            val dn = certificate.issuerDN as X500Name
            val country = dn.country
            val state = dn.state
            val locality = dn.locality
            val organization = dn.organization
            val organizationalUnit = dn.organizationalUnit
            val commonName = dn.commonName
            val version = certificate.version
            val issue = LocalDateTime.ofInstant(certificate.notBefore.toInstant(), ZoneId.systemDefault())
            val expire = LocalDateTime.ofInstant(certificate.notAfter.toInstant(), ZoneId.systemDefault())
            return JksInfo(country, state, locality, organization, organizationalUnit, commonName, version, issue, expire)
        } catch (e: TypeCastException) {
            throw RuntimeException("Keystore alis not exist")
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Keystore wrong alias password")
        }
    }

    fun validJksPassword(byteArray: ByteArray, password: String): Boolean {
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val keystore = KeyStore.getInstance(JKS)

        byteArrayInputStream.use {
            try {
                keystore.load(byteArrayInputStream, password.toCharArray())
                return true
            } catch (e: Exception) {
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
            } catch (e: Exception) {
                throw RuntimeException("Keystore file invalid or wrong password")
            }

            try {
                keystore.getEntry(alias, KeyStore.PasswordProtection(aliasPassword.toCharArray())) as KeyStore.PrivateKeyEntry
                return true
            } catch (e: Exception) {
            }
        }
        return false
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
}

/*
fun main(args: Array<String>) {
    val fs = FileInputStream("/Users/sheng/Downloads/csdn.keystore")
    val byteArrayOutputStream = ByteArrayOutputStream()
    fs.copyTo(byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    println(CertUtil.validJksPassword(byteArray, "123456"))
    println(CertUtil.validJksAlias(byteArray, "123456", "csdn", "csdn123456"))
    CertUtil.parseJks(byteArray,"123456", "csdn", "csdn123456")
}
*/