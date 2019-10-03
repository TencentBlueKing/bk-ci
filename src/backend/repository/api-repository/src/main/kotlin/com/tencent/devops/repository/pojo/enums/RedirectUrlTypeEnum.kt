package com.tencent.devops.repository.pojo.enums

enum class RedirectUrlTypeEnum(val type: String) {
    ATOM_MARKET("atomMarket"),
    ATOM_REPOSITORY("atomRepository"),
    DEFAULT("default");

    companion object {
        fun getRedirectUrlType(type: String): RedirectUrlTypeEnum {
            return when (type) {
                "atomMarket" -> ATOM_MARKET
                "atomRepository" -> ATOM_REPOSITORY
                "default" -> ATOM_REPOSITORY
                else -> DEFAULT
            }
        }
    }
}