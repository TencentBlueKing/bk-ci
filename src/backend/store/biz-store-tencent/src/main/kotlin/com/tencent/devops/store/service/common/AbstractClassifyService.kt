package com.tencent.devops.store.service.common

abstract class AbstractClassifyService {

    /**
     * 获取删除分类标识
     */
    abstract fun getDeleteClassifyFlag(classifyId: String): Boolean
}
