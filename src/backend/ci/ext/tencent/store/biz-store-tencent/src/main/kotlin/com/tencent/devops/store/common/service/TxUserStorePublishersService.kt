package com.tencent.devops.store.common.service

interface TxUserStorePublishersService {



    fun updateComponentFirstPublisher(userId: String): Boolean
}