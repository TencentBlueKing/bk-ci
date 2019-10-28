package com.tencent.devops.plugin.service.cos

import com.tencent.devops.common.cos.COSClient
import com.tencent.devops.common.cos.COSClientConfig
import com.tencent.devops.common.cos.model.exception.COSException
import com.tencent.devops.common.cos.request.AppendObjectRequest
import com.tencent.devops.common.cos.request.ClientGetObjectRequest
import com.tencent.devops.common.cos.request.DeleteObjectRequest
import com.tencent.devops.common.cos.request.PutObjectRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CosService {

    @Throws(COSException::class)
    fun upload(
        cosClientConfig: COSClientConfig,
        bucket: String,
        fileName: String,
        headers: Map<String, String>,
        content: ByteArray,
        contentType: String
    ): String {
        try {
            val cosClient = COSClient(cosClientConfig)

            val putObjectRequest = PutObjectRequest(bucket, fileName, headers, content, contentType)
            val putObjectResponse = cosClient.putObject(putObjectRequest)

            if (!putObjectResponse.isSuccess()) {
                val errorMessage = String.format("Upload file(%s) to COS failed: %s", fileName, putObjectResponse.getErrorMessage())
                logger.error(errorMessage)
                throw COSException(errorMessage)
            } else {
                return putObjectResponse.getSha1()
            }
        } catch (e: COSException) {
            val errorMessage = String.format("Upload file(%s) to COS failed: %s", fileName, e.message)
            logger.error(errorMessage, e)
            throw COSException(errorMessage)
        }
    }

    @Throws(COSException::class)
    fun append(
        cosClientConfig: COSClientConfig,
        bucket: String,
        fileName: String,
        headers: Map<String, String>?,
        content: ByteArray,
        positionAppend: Long,
        contentType: String
    ): Long {
        val cosClient = COSClient(cosClientConfig)

        val appendObjectRequest = AppendObjectRequest(
                bucket,
                fileName,
                headers,
                content,
                positionAppend,
                contentType
        )
        val appendObjectResponse = cosClient.appendObject(appendObjectRequest)

        if (!appendObjectResponse.isSuccess) {
            val errorMessage = String.format("Append file(%s) trunk at position %s to COS failed", fileName, positionAppend)
            logger.error(errorMessage + appendObjectResponse.errorMessage)
            throw COSException(errorMessage)
        }
        return appendObjectResponse.getNextAppendPosition()
    }

    fun deleteFile(cosClientConfig: COSClientConfig, bucket: String, fileName: String): Boolean {
        try {
            val cosClient = COSClient(cosClientConfig)
            val request = DeleteObjectRequest(bucket, fileName)
            val response = cosClient.deleteObject(request)
            if (response.isSuccess) {
                val msg = String.format(
                        "Delete existing file(%s) from COS succeeded", fileName)
                logger.info(msg)
                return true
            }
            if (response.isNotFound) {
                val msg = String.format(
                        "Delete existing file(%s) from COS succeeded: file not originally exists ", fileName)
                logger.info(msg)
                return true
            }
            val msg = String.format("Delete file(%s) from COS failed: %s", fileName, response.getErrorMessage())
            logger.error(msg)
            return false
        } catch (ex: COSException) {
            val msg = String.format("Delete file(%s) from COS  failed: %s", fileName, ex.message)
            logger.error(msg, ex)
            return false
        }
    }

    @Throws(COSException::class)
    fun clientGetObjectUrl(cosClientConfig: COSClientConfig, bucket: String, fileName: String, expireSeconds: Long): String {
        val cosClient = COSClient(cosClientConfig)

        val clientGetObjectRequest = ClientGetObjectRequest(
                bucket,
                fileName,
                expireSeconds
        )
        val clientGetObjectResponse = cosClient.clientGetObject(clientGetObjectRequest)

        if (!clientGetObjectResponse.isSuccess()) {
            val errorMessage = String.format("Get object url failed, fileName: %s", fileName)
            logger.error(errorMessage)
            throw COSException(errorMessage)
        }
        return clientGetObjectResponse.url
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CosService::class.java)
    }
}