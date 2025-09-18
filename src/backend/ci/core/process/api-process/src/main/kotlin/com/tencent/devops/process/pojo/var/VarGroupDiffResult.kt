package com.tencent.devops.process.pojo.`var`

data class VarGroupDiffResult(
    val varsToRemove: Set<String>, // 需要移除的变量名
    val varsToUpdate: Set<String>, // 需要更新的变量名
    val varsToAdd: Set<String> // 需要新增的变量名
)