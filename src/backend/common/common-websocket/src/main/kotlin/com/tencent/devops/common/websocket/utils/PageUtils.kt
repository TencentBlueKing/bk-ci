package com.tencent.devops.common.websocket.utils

object PageUtils {
    fun buildTagPage(page: String, tagName: String): String {
        if (page.endsWith("/")) {
            return page + tagName
        } else {
            return "$page/$tagName"
        }
    }

    fun replacePage(page: String, newPageTage: String, oldPageTage: String): String? {
        var newPage: String? = null
        if (page.contains(oldPageTage)) {
            newPage = page.replace(oldPageTage, newPageTage)
        }
        return newPage
    }

    fun replaceAssociationPage(page: String): String?
    {
        var newPage: String? = null
        if (page.contains("upgrade")) {
            newPage = replacePage(page, "shelf", "upgrade")
        }
        if (page.contains("shelf")) {
            newPage = replacePage(page, "upgrade", "shelf")
        }
        return newPage
    }

    // 因流水线列表页有三个tag页，故此处区别于另外两种情况，需要推三个页面
    fun createAllTagPage(page: String): MutableList<String> {
        val pageList = mutableListOf<String>()
        pageList.add(page)
        pageList.add(PageUtils.buildTagPage(page, "allPipeline"))
        pageList.add(PageUtils.buildTagPage(page, "collect"))
        pageList.add(PageUtils.buildTagPage(page, "myPipeline"))
        return pageList
    }
}