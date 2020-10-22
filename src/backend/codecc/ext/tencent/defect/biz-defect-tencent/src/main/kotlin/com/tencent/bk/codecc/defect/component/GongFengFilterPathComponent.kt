package com.tencent.bk.codecc.defect.component

import org.springframework.stereotype.Component

@Component
class GongfengFilterPathComponent : IFilterPathComponent {

    override fun judgeFilter(
            filePath: String?
    ): Boolean {
        //todo 后续要加所有的过滤路径
        return (!filePath.isNullOrBlank() &&
                (filePath.startsWith("/data/landun/workspace/.temp") ||
                        filePath.startsWith("/data/landun/workspace/.git")))
    }
}