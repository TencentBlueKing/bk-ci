/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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