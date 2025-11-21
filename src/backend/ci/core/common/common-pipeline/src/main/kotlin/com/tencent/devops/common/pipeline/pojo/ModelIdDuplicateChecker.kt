package com.tencent.devops.common.pipeline.pojo

data class ModelIdDuplicateChecker(
    val idSet: MutableSet<String> = mutableSetOf(),
    val duplicateIdSet: MutableSet<String> = mutableSetOf()
) {
    fun addId(id: String) {
        if (idSet.contains(id)) {
            duplicateIdSet.add(id)
        } else {
            idSet.add(id)
        }
    }
}
