package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("自定义产出物报告", description = ReportArchiveElement.classType)
data class ReportArchiveElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "python文件编译",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待上传文件夹）", required = true)
    val fileDir: String = "",
    @ApiModelProperty("入口文件）", required = false)
    val indexFile: String = "",
    @ApiModelProperty("标签别名", required = true)
    val reportName: String = "",
    @ApiModelProperty("开启邮件", required = false)
    val enableEmail: Boolean?,
    @ApiModelProperty("邮件接收者", required = false)
    val emailReceivers: Set<String>?,
    @ApiModelProperty("邮件标题", required = false)
    val emailTitle: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "reportArchive"
    }

    override fun getClassType() = classType
}
// fun main(array: Array<String>) {
//    val e = ReportArchiveElement("python文件编译", "id", "true", "dir", "file"
//    ,"reportName", true, setOf("a","b","d","dd"),"title")
//    val toMutableMap = JsonUtil.toMutableMap(e)
//    val reportArchiveElement = JsonUtil.mapTo(toMutableMap, ReportArchiveElement::class.java)
//    println("$reportArchiveElement")
//    println("map=$toMutableMap")
//    val emailReceivers = toMutableMap["emailReceivers"]
//    val toJson = JsonUtil.toJson(emailReceivers!!)
//    println(toJson::class.java.isAssignableFrom(String::class.java))
//    println(JsonUtil.toJson("abc"))
//    println(JsonUtil.toJson(123))
//    val bean = setOf("dd", "bb")
//    val toJson1 = JsonUtil.toJson(bean)
//    val to = JsonUtil.to<List<String>>("dd,bb")
//    to.forEach {
//        println("{$it}")
//    }
//
//    to.joinToString(",") {
//        if ()
//    }
//
// }
