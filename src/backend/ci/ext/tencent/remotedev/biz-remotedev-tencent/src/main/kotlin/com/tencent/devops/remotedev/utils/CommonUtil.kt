package com.tencent.devops.remotedev.utils

object CommonUtil {
    /**
     * 检查项目是不是属于个人
     * 因为未来方案要换，所以全部同一成一个方法，未来好检索和修改
     */
    fun ifProjectPersonal(projectCode: String) = projectCode.startsWith("_")
}