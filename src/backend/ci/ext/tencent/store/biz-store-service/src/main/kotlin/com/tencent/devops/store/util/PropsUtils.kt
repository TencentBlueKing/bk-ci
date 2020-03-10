package com.tencent.devops.store.util

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JacksonUtil
import com.tencent.devops.store.pojo.Props

class PropsUtils

    val file = "{\n" +
        "  \"serviceCode\":\"extServiceDemo\",\n" +
        "  \"itemList\":[\n" +
        "    {\n" +
        "      \"itemCode\":\"jobDeploy\",\n" +
        "      \"props\":{\n" +
        "        \"entryResUrl\":\"index.html\",\n" +
        "        \"data\":{\n" +
        "            \"test\":\"hello world\"\n" +
        "        },\n" +
        "        \"options\":{\n" +
        "\n" +
        "        },\n" +
        "        \"tooltip\":\"tip\",\n" +
        "        \"iconUrl\":\"img/icon.png\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}"

    fun main(args: Array<String>) {
        println(file)
        val objectMapping = JacksonUtil.createObjectMapper()
        val objMap =objectMapping.readValue<HashMap<String, Any>>(file)
        println(objMap["serviceCode"])
        println(objMap["itemList"])
        val itemList = objMap["itemList"] as ArrayList<String>
        println(itemList)
        itemList.forEach {
            println(it)
//            println("key:${it.key}")
//            println("value:${it.value}")
        }
    }
