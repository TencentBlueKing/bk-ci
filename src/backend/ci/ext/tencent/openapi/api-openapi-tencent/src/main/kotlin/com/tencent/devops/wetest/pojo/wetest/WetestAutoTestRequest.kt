/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest提交测试请求")
data class WetestAutoTestRequest(
    @ApiModelProperty("上传得到的apkid")
    val apkid: Int?,
    @ApiModelProperty("上传得到的ipaid")
    val ipaid: Int?,
    @ApiModelProperty("上传得到的scriptid")
    val scriptid: Int?,
    @ApiModelProperty("公有云种类，随机还是TOP机型，1Top机型，2随机机型")
    val toptype: Int,
    @ApiModelProperty("测试机型的数量")
    val topnum: Int,
    @ApiModelProperty("测试类型:install（快速兼容测试）、unity（Unity兼容测试）、app（app功能测试）、ios(ios测试)、othermonkey(第三方脚本测试)")
    val testtype: String,
    @ApiModelProperty("测试时间长度，单位秒,私有云最长8小时;公有云快速兼容最长900秒,其余测试2小时", required = false)
    val runtime: Int? = null,
    @ApiModelProperty("登陆类型:none,qq,wechat,custom；默认none不登陆", required = false)
    val login: String? = null,
    @ApiModelProperty("如果login是custom,需要提供登陆帐号；格式[[name1,pwd1],[name2,pwd2]]", required = false)
    val custom_account: List<List<String>>? = null,
    @ApiModelProperty("如果为app测试，需要指示框架类型appium、robotium、uiautomator、gautomator", required = false)
    val frametype: String? = null,
    @ApiModelProperty("如果为xctest测试，需要指示框架类型xctest", required = false)
    val extraid: Int? = null,
    @ApiModelProperty("机型id数组或者逗号间隔的id，比如从私有云拉取的机型id", required = false)
    val models: List<String>? = null,
    @ApiModelProperty("私有云ID，使用所有的私有云机器", required = false)
    val cloudid: String? = null,
    @ApiModelProperty("团队ID，需要向小助手咨询查询ID", required = false)
    val projectid: Int? = null,
    @ApiModelProperty("测试的备注", required = false)
    val comments: String? = null,
    @ApiModelProperty("测试来源")
    val test_from: String? = null,
    @ApiModelProperty("多apk信息", required = false)
    val extrainfo: String? = null
)
