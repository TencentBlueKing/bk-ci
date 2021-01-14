package com.tencent.bk.codecc.defect.utils

import com.tencent.bk.codecc.defect.vo.DefectDetailVO
import org.json.JSONArray
import org.json.JSONException
import org.slf4j.LoggerFactory
import java.util.*

object CommonDefectAttributeUtils {

    private val logger = LoggerFactory.getLogger(CommonDefectAttributeUtils::class.java)

    fun getAuthor(defectDetailVO: DefectDetailVO, scmJsonArr: JSONArray?): Set<String> {
        val authorSet = TreeSet<String>()
        if (scmJsonArr == null) {
            logger.info("Scm change records json is empty!")
            return authorSet
        }
        val srcFile = defectDetailVO.filePathname
        val lineNum = defectDetailVO.lineNumber
        var affStr = ""
        for (i in 0 until scmJsonArr.length()) {
            val jsonFile = scmJsonArr.getJSONObject(i)
            var fileName = jsonFile.getString("filename")
            val changeArr = jsonFile.getJSONArray("change_records")

            fileName = fileName.replace("\\", "/")

            /*
             * 由于windows下，fileName前面会有磁盘号，比如：D:/workspace/svnauth_proxysvr/app/models/SimpleResModel.java，
             * 而srcFile 由于经过klocwork分析处理的路径为：/d/workspace/svnauth_proxysvr/app/models/simpleresmodel.java，
             * 所以要将fileName的磁盘号做转换，并且全部转换成小写在比较
             */
            if (fileName.indexOf("/") > 0) {
                fileName = "/" + fileName.toLowerCase().replaceFirst(":".toRegex(), "")
            }

            if (fileName.indexOf(srcFile) != -1) {
                for (j in 0 until changeArr.length()) {
                    val jsonRec = changeArr.getJSONObject(j)
                    val affArr = jsonRec.getJSONArray("affected_lines")
                    var authCur: String? = ""
                    try {
                        val userName = jsonRec.getString("username")
                        if (userName.isNotBlank()) {
                            val keyIndex = userName.indexOf("@tencent.com")
                            if (keyIndex != -1) {
                                authCur = userName.substring(0, keyIndex)
                            }
                        }
                        if (authCur.isNullOrBlank()) {
                            authCur = jsonRec.getString("author")

                            if (authCur.isNotBlank()) {
                                // 去掉中文名
                                val keyIndex = authCur.indexOf("(")
                                if (keyIndex != -1) {
                                    authCur = authCur.substring(0, keyIndex)
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        logger.error("there is not author", e)
                    }

                    // 遍历文件修改记录
                    for (k in 0 until affArr.length()) {
                        val lineArr = ArrayList<Int>()
                        affStr = affArr.get(k).toString()
                        // 修改点是一个范围，如[5,10]
                        if (affStr.indexOf("[") != -1) {
                            affStr = affStr.substring(1, affStr.length - 1)
                            val strArr = affStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            lineArr.add(Integer.parseInt(strArr[0].trim { it <= ' ' }))
                            lineArr.add(Integer.parseInt(strArr[1].trim { it <= ' ' }))
                        } else {
                            lineArr.add(Integer.parseInt(affStr))
                        }
                        if (lineArr.size > 1 && lineNum >= lineArr[0] && lineNum <= lineArr[1] || lineArr.size == 1 && lineNum == lineArr[0]) {
                            authorSet.add(authCur!!)
                        }
                    }
                }
            }
        }
        logger.info("get author for id: {}, author: {}", defectDetailVO.id, authorSet.toString())
        return authorSet
    }
}