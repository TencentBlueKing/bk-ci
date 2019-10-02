package com.tencent.devops.plugin.pojo.wetest

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest设备信息")
data class WetestDevice(
    @ApiModelProperty("cloudid")
    @JsonProperty("cloudid")
    val cloudId: String?,
    @ApiModelProperty("cpu_ghz")
    @JsonProperty("cpu_ghz")
    val cpuGhz: String?,
    @ApiModelProperty("cpu_total")
    @JsonProperty("cpu_total")
    val cpuTotal: String?,
    @ApiModelProperty("deviceid")
    @JsonProperty("deviceid")
    val deviceId: String?,
    @ApiModelProperty("id")
    @JsonProperty("id")
    val id: String?,
    @ApiModelProperty("location")
    @JsonProperty("location")
    val location: String?,
    @ApiModelProperty("mac")
    @JsonProperty("mac")
    val mac: String?,
    @ApiModelProperty("manu")
    @JsonProperty("manu")
    val manu: String?,
    @ApiModelProperty("mem_show")
    @JsonProperty("mem_show")
    val memShow: String?,
    @ApiModelProperty("model")
    @JsonProperty("model")
    val model: String?,
    @ApiModelProperty("model_en")
    @JsonProperty("model_en")
    val modelEn: String?,
    @ApiModelProperty("modelid")
    @JsonProperty("modelid")
    val modelId: String?,
    @ApiModelProperty("opcode")
    @JsonProperty("opcode")
    val opcode: String?,
    @ApiModelProperty("opcodedesc")
    @JsonProperty("opcodedesc")
    val opCodeDesc: String?,
    @ApiModelProperty("resolution")
    @JsonProperty("resolution")
    val resolution: String?,
    @ApiModelProperty("state")
    @JsonProperty("state")
    val state: String?,
    @ApiModelProperty("statedesc")
    @JsonProperty("statedesc")
    val stateDesc: String?,
    @ApiModelProperty("testid")
    @JsonProperty("testid")
    val testId: String?,
    @ApiModelProperty("usernum")
    @JsonProperty("usernum")
    val userNum: String?,
    @ApiModelProperty("version")
    @JsonProperty("version")
    val version: String?
)

/*

    {
        "usernum": 918981,
        "mem_show": 3072,
        "modelid": 918486,
        "cpu_ghz": "2.5",
        "manu": "Xiaomi",
        "cpu_total": 4,
        "opcode": 15,
        "opcodedesc": "标;深;调;性",
        "deviceid": "06611bfba1dc673840a7c6124aa56897",
        "version": "4.4",
        "resolution": "1080x1920",
        "mac": "74:51:ba:32:c1:dd",
        "statedesc": "空闲",
        "cloudid": 0,
        "model": "4",
        "testid": "a54b3d97e66384fc0638d2b5c0300d13",
        "location": "wetest-A6-device-server009",
        "state": 1,
        "id": 918486
    }

*/