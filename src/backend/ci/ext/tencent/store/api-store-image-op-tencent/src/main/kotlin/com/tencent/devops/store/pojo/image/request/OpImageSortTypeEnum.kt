package com.tencent.devops.store.pojo.image.request

enum class OpImageSortTypeEnum(val sortType: String) {
    imageCode("IMAGE_CODE"),
    imageName("IMAGE_NAME"),
    classifyId("CLASSIFY_ID"),
    imageStatus("IMAGE_STATUS"),
    imageType("IMAGE_TYPE"),
    latestFlag("LATEST_FLAG"),
    publisher("PUBLISHER"),
    creator("CREATOR"),
    modifier("MODIFIER"),
    createTime("CREATE_TIME"),
    updateTime("UPDATE_TIME")
}