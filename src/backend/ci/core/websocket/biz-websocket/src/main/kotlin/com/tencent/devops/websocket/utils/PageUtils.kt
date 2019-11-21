package com.tencent.devops.websocket.utils


object PageUtils {

    fun buildNormalPage(page: String): String{
        if(page.endsWith("/executeDetail")){
            return page.replace("/executeDetail","")
        }

        if(page.contains("/list/") && !page.endsWith("/list")){
            val index = page.indexOf("/list")
            return page.substring( 0, index+5)
        }
        return page
    }
}