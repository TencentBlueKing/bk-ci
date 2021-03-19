package com.tencent.bk.codecc.defect.component

interface IFilterPathComponent {

    //过滤文件处理，区分创建来源工蜂特殊处理,是否过滤文件
    fun judgeFilter(filePath: String?): Boolean;

}