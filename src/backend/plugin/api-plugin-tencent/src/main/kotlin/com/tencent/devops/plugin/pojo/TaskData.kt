package com.tencent.devops.plugin.pojo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.annotations.ApiModel

/**
 * Created by Aaron Sheng on 2018/4/26.
 */
@ApiModel("任务数据-多态基类")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
        JsonSubTypes.Type(value = FileTaskData::class, name = FileTaskData.classType)
)
interface TaskData {
    val cost: Int
}