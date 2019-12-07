package com.tencent.devops.store.pojo.image.enums

enum class MarketImageSortTypeEnum() {
    NAME,
    CREATE_TIME,
    UPDATE_TIME,
    PUBLISHER,
    DOWNLOAD_COUNT;

    companion object {
        fun getSortType(type: String?): String {
            return when (type) {
                NAME.name -> "IMAGE_NAME"
                else -> type ?: CREATE_TIME.name
            }
        }

        fun getSortTypeEnum(type: String?): MarketImageSortTypeEnum {
            values().forEach {
                if (it.name.toLowerCase() == type?.toLowerCase()) {
                    return it
                }
            }
            return NAME
        }
    }
}