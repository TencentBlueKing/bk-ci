package com.tencent.devops.plugin.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚app扫描回调结果")
data class JinGangAppCallback(
    @ApiModelProperty("0表示成功，其他表示失败")
    val status: String,
    @ApiModelProperty("失败提示信息，成功则为空")
    val msg: String,
    @ApiModelProperty("构建号")
    val buildId: String,
    @ApiModelProperty("构建下面对应的任务号")
    val taskId: String,
    @ApiModelProperty("element ID")
    val elementId: String,
    @ApiModelProperty("该次扫描文件md5")
    val md5: String,
    @ApiModelProperty("结果html地址")
    @JsonProperty("scan_url")
    val scanUrl: String,
    @ApiModelProperty("结果xml下载地址")
    @JsonProperty("scan_xml")
    val scanXml: String,
    @ApiModelProperty("上传人")
    @JsonProperty("responseuser")
    val responseUser: String

)
/*
'{"status":"0","msg":"","buildId":"0","taskId":"0",
"md5":"a781144592236df91e763847ca2b5f6a",
"scan_url":"http://kk.oa.com:8080/report/new/8c3fd81aa725b0908aa5dcb5d4ed7788629876.html",
"scan_xml":"http://kk.oa.com:8080/report/xml/8c3fd81aa725b0908aa5dcb5d4ed7788.xml",
"responseuser":"tom"}'


 */