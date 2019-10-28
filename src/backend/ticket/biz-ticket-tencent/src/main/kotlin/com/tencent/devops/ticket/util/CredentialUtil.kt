package com.tencent.devops.ticket.util

import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.enums.CredentialType

/**
 * Created by Aaron Sheng on 2018/1/29.
 */
object CredentialUtil {
    private val SSH_PRIVATE_PREFIX = "-----BEGIN RSA PRIVATE KEY-----"
    private val SSH_PRIVATE_SUFFIX = "-----END RSA PRIVATE KEY-----"
    private val credentialMixer = "******"

    fun isValid(credentialCreate: CredentialCreate): Boolean {
        return isValid(credentialCreate.credentialType,
                credentialCreate.v1,
                credentialCreate.v2,
                credentialCreate.v3,
                credentialCreate.v4)
    }

    fun isValid(credentialUpdate: CredentialUpdate): Boolean {
        return isValidUpdate(credentialUpdate.credentialType,
                credentialUpdate.v1,
                credentialUpdate.v2,
                credentialUpdate.v3,
                credentialUpdate.v4)
    }

    private fun isValid(credentialType: CredentialType, v1: String, v2: String?, v3: String?, v4: String?): Boolean {
        return when (credentialType) {
            CredentialType.PASSWORD -> {
                true
            }
            CredentialType.ACCESSTOKEN -> {
                true
            }
            CredentialType.USERNAME_PASSWORD -> {
                true
            }
            CredentialType.SECRETKEY -> {
                true
            }
            CredentialType.APPID_SECRETKEY -> {
                v2 ?: return false
                true
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (!(v1.startsWith(SSH_PRIVATE_PREFIX) && v1.endsWith(SSH_PRIVATE_SUFFIX))) {
                    return false
                }
                true
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                v2 ?: return false
                if (!(v2.startsWith(SSH_PRIVATE_PREFIX) && v2.endsWith(SSH_PRIVATE_SUFFIX))) {
                    return false
                }
                true
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                true
            }
            CredentialType.COS_APPID_SECRETID_SECRETKEY_REGION -> {
                true
            }
        }
    }

    private fun isValidUpdate(credentialType: CredentialType, v1: String, v2: String?, v3: String?, v4: String?): Boolean {
        return when (credentialType) {
            CredentialType.PASSWORD -> {
                true
            }
            CredentialType.ACCESSTOKEN -> {
                true
            }
            CredentialType.USERNAME_PASSWORD -> {
                true
            }
            CredentialType.SECRETKEY -> {
                true
            }
            CredentialType.APPID_SECRETKEY -> {
                v2 ?: return false
                true
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (!(v1.startsWith(SSH_PRIVATE_PREFIX) && v1.endsWith(SSH_PRIVATE_SUFFIX)) && v1 != credentialMixer) {
                    return false
                }
                true
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                v2 ?: return false
                if (!(v2.startsWith(SSH_PRIVATE_PREFIX) && v2.endsWith(SSH_PRIVATE_SUFFIX)) && v2 != credentialMixer) {
                    return false
                }
                true
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                true
            }
            CredentialType.COS_APPID_SECRETID_SECRETKEY_REGION -> {
                true
            }
        }
    }
}