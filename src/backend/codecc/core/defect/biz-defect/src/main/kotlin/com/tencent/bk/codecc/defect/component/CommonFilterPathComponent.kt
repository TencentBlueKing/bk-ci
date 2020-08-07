package com.tencent.bk.codecc.defect.component

import org.springframework.stereotype.Component

@Component("CommonFilterPathComponent")
class CommonFilterPathComponent : IFilterPathComponent {

    //一般创建来源处理过滤文件
    override fun judgeFilter(filePath: String?): Boolean {
           return false;
    }
}