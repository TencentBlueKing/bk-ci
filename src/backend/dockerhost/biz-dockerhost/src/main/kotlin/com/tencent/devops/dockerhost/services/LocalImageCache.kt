package com.tencent.devops.dockerhost.services

import java.util.Date

object LocalImageCache {

    private val imageUsedDataMap = mutableMapOf<String, Date>()

    fun saveOrUpdate(imageName: String) {
        imageUsedDataMap[imageName] = Date()
    }

    fun getDate(imageName: String): Date? {
        return imageUsedDataMap[imageName]
    }
}