package com.tencent.bk.codecc.defect.component

import com.tencent.devops.common.constant.ComConstants
import org.springframework.stereotype.Component

@Component
class GongfengFilterPathComponent {

    fun judgeGongfengFilter(
        taskCreateFrom: String,
        filePath: String?
    ): Boolean {
        //todo 后续要加所有的过滤路径
        return (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == taskCreateFrom &&
            !filePath.isNullOrBlank() &&
            (filePath.startsWith("/data/landun/workspace/.temp") ||
                filePath.startsWith("/data/landun/workspace/.git")))
    }
}