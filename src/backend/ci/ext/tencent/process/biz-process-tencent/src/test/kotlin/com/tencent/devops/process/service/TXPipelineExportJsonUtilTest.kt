package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.DispatchSubInfoRegisterLoader
import com.tencent.devops.common.pipeline.DispatchSubTypeRegisterLoader
import com.tencent.devops.common.pipeline.ElementSubTypeRegisterLoader
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class TXPipelineExportJsonUtilTest {

    @Test
        /*比较内容是否一致*/
    fun similarTest() {
        val baseModel = getStrFromResource("modelExportJsonUtil.json")
        ElementSubTypeRegisterLoader.registerElement(null)
        DispatchSubTypeRegisterLoader.registerType()
        DispatchSubInfoRegisterLoader.registerInfo()
        val load = JsonUtil.to(baseModel, PipelineModelAndSetting::class.java)
        val out = JsonUtil.toJson(load)
        val sOut = JsonUtil.toSortJson(load)
        Assertions.assertTrue(JSONObject(sOut).similar(JSONObject(out)))
    }

    private fun getStrFromResource(testYaml: String): String {
        val classPathResource = ClassPathResource(testYaml)
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }
        inputStream.close()
        return sb.toString()
    }
}
