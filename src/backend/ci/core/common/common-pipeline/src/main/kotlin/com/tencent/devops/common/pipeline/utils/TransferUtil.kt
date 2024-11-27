package com.tencent.devops.common.pipeline.utils

import org.json.JSONObject

object TransferUtil {

    /*
    * 简化input, 如果是默认值则去掉
    * */
    fun simplifyParams(defaultValue: JSONObject?, input: Map<String, Any>): MutableMap<String, Any> {
        val out = input.toMutableMap()
        defaultValue?.keys()?.forEach { key ->
            val inputValue = out[key] ?: return@forEach
            if (JSONObject(key to defaultValue[key]).similar(JSONObject(key to inputValue))) {
                out.remove(key)
            }
        }
        return out
    }

    /*
    * 填充input，如果input没有，defaultValueMap有，则填充进去。
    * */
    fun mixParams(defaultValue: JSONObject?, input: Map<String, Any?>?): MutableMap<String, Any?> {
        val out = input?.toMutableMap() ?: mutableMapOf()
        defaultValue?.toMap()?.forEach { (k, v) ->
            val value = out[k]
            if (value == null) {
                out[k] = v
            }
        }
        return out
    }
}
