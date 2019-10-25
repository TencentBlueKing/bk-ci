package com.tencent.devops.plugin.pojo.wetest

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
    @ApiModelProperty("机型id数组或者逗号间隔的id，比如从私有云拉取的机型id", required = false)
    val models: List<String>? = null,
    @ApiModelProperty("私有云ID，使用所有的私有云机器", required = false)
    val cloudid: String? = null,
    @ApiModelProperty("团队ID，需要向小助手咨询查询ID", required = false)
    val projectid: Int? = null,
    @ApiModelProperty("测试的备注", required = false)
    val comments: String? = null,
    @ApiModelProperty("测试来源")
    val test_from: String = "bk-devops",
    @ApiModelProperty("多apk信息")
    val extrainfo: String? = null
)
