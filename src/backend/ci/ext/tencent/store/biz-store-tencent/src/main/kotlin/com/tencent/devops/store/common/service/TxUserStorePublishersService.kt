package com.tencent.devops.store.common.service

import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface TxUserStorePublishersService {



    fun updateComponentFirstPublisher(type:StoreTypeEnum): Boolean
}