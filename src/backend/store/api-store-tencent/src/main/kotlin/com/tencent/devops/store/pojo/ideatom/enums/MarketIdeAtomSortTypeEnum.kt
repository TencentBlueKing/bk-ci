package com.tencent.devops.store.pojo.ideatom.enums

enum class MarketIdeAtomSortTypeEnum {
    NAME,
    CREATE_TIME,
    UPDATE_TIME,
    PUBLISHER,
    DOWNLOAD_COUNT,
    WEIGHT;

    companion object {
        fun getSortType(type: String?): String {
            return when (type) {
                MarketIdeAtomSortTypeEnum.NAME.name -> "ATOM_NAME"
                MarketIdeAtomSortTypeEnum.UPDATE_TIME.name -> MarketIdeAtomSortTypeEnum.UPDATE_TIME.name
                MarketIdeAtomSortTypeEnum.PUBLISHER.name -> MarketIdeAtomSortTypeEnum.PUBLISHER.name
                MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT.name -> MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT.name
                MarketIdeAtomSortTypeEnum.WEIGHT.name -> MarketIdeAtomSortTypeEnum.WEIGHT.name
                else -> MarketIdeAtomSortTypeEnum.CREATE_TIME.name
            }
        }
    }
}