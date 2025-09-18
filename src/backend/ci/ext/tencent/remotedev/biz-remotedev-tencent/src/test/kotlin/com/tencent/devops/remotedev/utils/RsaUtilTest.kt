package com.tencent.devops.remotedev.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RsaUtilTest {

    companion object {
        private const val TEST_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2hEkRrR6qS/v6caAMG+z\n" +
                "MFduvYXxKU7QV991QAGj/0augHObOULzaxL5AV0/Gy7hKZhdtRpKGaK5GiCwrHgI\n" +
                "4dbeeiAwZgbamM6HO77eTwH8fzKpJthFfBVf5XtOj9i3YNO4D7f3+bBFyVbjJ7oX\n" +
                "I9zjreVPpB9BaHQdqLbDzfed9kw7kDfwgWHjPyrmmtv1ka3Vq5O1Q3F3ou4y1pCf\n" +
                "oy/+6zm5t/KnYu7jBKV5ZZ1xMQu+J3TAbin7jkGHxiDdmqockp8Av73A6DlrtKAE\n" +
                "JpjliYBNdvuGZp1yixFM/8EYuErugCfu1X9yRPgYPiAFmIZqFzwK5rCYbqzn8hXA\n" +
                "gwIDAQAB\n" +
                "-----END PUBLIC KEY-----"
        private const val TEST_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDaESRGtHqpL+/p\n" +
                "xoAwb7MwV269hfEpTtBX33VAAaP/Rq6Ac5s5QvNrEvkBXT8bLuEpmF21GkoZorka\n" +
                "ILCseAjh1t56IDBmBtqYzoc7vt5PAfx/Mqkm2EV8FV/le06P2Ldg07gPt/f5sEXJ\n" +
                "VuMnuhcj3OOt5U+kH0FodB2otsPN9532TDuQN/CBYeM/Kuaa2/WRrdWrk7VDcXei\n" +
                "7jLWkJ+jL/7rObm38qdi7uMEpXllnXExC74ndMBuKfuOQYfGIN2aqhySnwC/vcDo\n" +
                "OWu0oAQmmOWJgE12+4ZmnXKLEUz/wRi4Su6AJ+7Vf3JE+Bg+IAWYhmoXPArmsJhu\n" +
                "rOfyFcCDAgMBAAECggEAAsrXABP6wVF2Oc24G00S3Year2qYdlyMzNNCGRofeeFZ\n" +
                "0rAOry9tFoeDqls06tz+A4S9jnG4iTBNCa/P4CV/vgSWWYUxmXZ1AVS25P9b5Ji0\n" +
                "UJyn3D6thRoKkR6AsjumlxYFWvSShFJkuMZ+c7/HFBQVn+BtbwvHadYyzjpCf5yq\n" +
                "CupeqFwtAfe5VjkBPufwkqfgVhJ2HkFpJ/STcMgCAE+mGLCRzZMppmIdbxA6p1dS\n" +
                "2DM28bEMqcAPewatVcKm4yXJ56Uq2s7c7lVojXXNp2m9XqW+E7bGHY2S1tUlAMF1\n" +
                "rpCKbFM1t8z/dE4uraPqokBrH0Q8UpG/ZGebQ9BJoQKBgQD833WQXatDuS78STOs\n" +
                "Q8xg8kohXS1+Rqix7V0UICuDKEzBd6gh3PuOuwDILif5cQXVMgZFVZ7B8GNFdJ4u\n" +
                "ABNQh3SiaAbdJlesYWuNHhb/hmgcgsvsYjFuPfOhK4HjHMifmLqfnnJkFgCjGcEZ\n" +
                "1opoXQQS+/Vfvl2L9RxafvEzHQKBgQDcw36T6bFaIUuEwAzdoZ6/g6z6+N0BUtSq\n" +
                "cqi7diLb9KY6LPWfQSsR1tudCnOz0syhwSOBkqGJ9hy6ChfkgARS7qPzgYMe+EuP\n" +
                "KJ9aaxv1H8Pyo6Z97ATWp+dw2lF6JVS1o2aFCfdMVMCvC7jj27BgDHRBPSZWG32l\n" +
                "UhddTm3QHwKBgAS0F1i3FU9uK1yP9AEZRbzr5MsYE5eMsuiUVK3iJ00KO9tBYYQ3\n" +
                "uwodmlce3Kl8G7KqeolhnVFYKlCJacPLRpCx5E59BtuoMetH6Js4Ww8nrdoR6L8d\n" +
                "2HOHfXea+pCB4Y1uZtI/PTt1WU+vU/MqFWr5h/DedOLyBIVs6rkYMV5VAoGBAJlR\n" +
                "oDgDpetNklAYvRazZzfksB8A9SQ1LYNO7EZ9DkqqR0PB87ftn4bdLFqNZrutm/Z+\n" +
                "1zTm7A+PgHXoCp3kFpyuJek4uiKpGHjNIpa+KoV70S9TXo63esjFhWQIC2wEPyeC\n" +
                "2vFscXZuqf8n6fk9mump3Jdua5CDuSg2sglYLcXLAoGAFdD1TK2DvC8/YA/TWnEV\n" +
                "Is3pqrMhTxAMMk31Sq+MbuS8cIoo/nrM4dfinuNySw8OncVhXS78AuCSewzRRo3q\n" +
                "/77R0lsPQveYC52B1q6hbnIj+D1hydgrEceu1m0pGqmhEIYpTBxdZKP0jkUDj+JB\n" +
                "D7DxMgZxFTtHGWuRTVq2ARU=\n" +
                "-----END PRIVATE KEY-----"
    }

    @Test
    fun generatePublicKey() {
        RsaUtil.generatePublicKey(TEST_PUBLIC_KEY.toByteArray())
    }

    @Test
    fun generatePrivateKey() {
        RsaUtil.generatePrivateKey(TEST_PRIVATE_KEY.toByteArray())
    }

    @Test
    fun rsaEncryptAndDecrypt() {
        val value = "123456"
        val encryptValue = RsaUtil.rsaEncrypt(
            value = value,
            publicKey = RsaUtil.generatePublicKey(TEST_PUBLIC_KEY.toByteArray())
        )
        val decryptValue = RsaUtil.rsaDecrypt(
            value = encryptValue,
            privateKey = RsaUtil.generatePrivateKey(TEST_PRIVATE_KEY.toByteArray())
        )
        Assertions.assertEquals(value, decryptValue)
    }
}
