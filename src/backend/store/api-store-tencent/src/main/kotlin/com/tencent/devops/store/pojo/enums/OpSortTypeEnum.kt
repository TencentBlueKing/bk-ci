package com.tencent.devops.store.pojo.enums

enum class OpSortTypeEnum(val sortType: String) {
    atomName("NAME"),
    atomCode("ATOM_CODE"),
    jobType("JOB_TYPE"),
    classifyId("CLASSIFY_ID"),
    atomType("ATOM_TYPE"),
    atomStatus("ATOM_STATUS"),
    defaultFlag("DEFAULT_FLAG"),
    latestFlag("LATEST_FLAG"),
    publisher("PUBLISHER"),
    weight("WEIGHT"),
    creator("CREATOR"),
    modifier("MODIFIER"),
    createTime("CREATE_TIME"),
    updateTime("UPDATE_TIME")
}