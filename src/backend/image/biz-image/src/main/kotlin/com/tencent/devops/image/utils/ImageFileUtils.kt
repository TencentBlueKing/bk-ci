package com.tencent.devops.image.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.image.pojo.DockerImage
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.FileInputStream

object ImageFileUtils {
    fun parseImageMeta(imageFilePath: String): List<DockerImage> {
        val fis = FileInputStream(imageFilePath)
        val tarInputStream = TarArchiveInputStream(BufferedInputStream(fis))
        var entry: ArchiveEntry? = tarInputStream.nextTarEntry
        while (entry != null) {
            val tarEntry = entry as TarArchiveEntry
            val entryName = tarEntry.name
            if (entryName == "repositories") {
                return parseImagesFromContent(tarInputStream)
            }
            entry = tarInputStream.nextTarEntry
        }
        throw RuntimeException("解析镜像文件失败")
    }

    private fun parseImagesFromContent(tarInputStream: TarArchiveInputStream): List<DockerImage> {
        val content = tarInputStream.readBytes()
//                {
//                    "dockerhub.blueking.com:8090/bcs.abc/istio/mixer": {
//                        "latest": "0ae802bf6b7203a4fe91ee4c2877e6fae1d60f027aa059d118026116b6a80f70"
//                    ,
//                    "dockerhub.blueking.com:8090/bcs/network": {
//                        "latest": "0aaa4e2d51cfd8ed86960837584204c9aebf8a721a4ee21074e427374fefb33f"
//                    }
//                }
        val data: Map<String, Any> = jacksonObjectMapper().readValue(content)
        val imageList = mutableListOf<DockerImage>()
        for ((repoKey, repoValue) in data) {
            val imageTags = repoValue as Map<String, Any>
            for ((k) in imageTags) {
                imageList.add(DockerImage(repoKey, k, parseImageShortName(repoKey)))
            }
        }
        return imageList
    }

    // 从imageRepo中解析imageName(去掉域名和端口）
    private fun parseImageShortName(imageTag: String): String {
        val index = imageTag.indexOf('/')
        return if (index == -1) {
            imageTag
        } else {
            val pre = imageTag.substring(0, index)
            if (pre.contains(':') || pre.contains('.')) {
                imageTag.substring(index + 1, imageTag.length)
            } else {
                imageTag
            }
        }
    }
}