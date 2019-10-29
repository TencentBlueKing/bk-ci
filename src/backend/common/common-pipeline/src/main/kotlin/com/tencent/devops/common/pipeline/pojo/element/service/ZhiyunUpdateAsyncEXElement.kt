package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("织云-异步升级包", description = ZhiyunUpdateAsyncEXElement.classType)
data class ZhiyunUpdateAsyncEXElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "织云-异步升级包",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("业务名", required = true)
    val product: String = "",
    @ApiModelProperty("包名", required = true)
    val pkgName: String = "",
    @ApiModelProperty("升级前版本，为\"\"的话系统自动计算", required = false)
    val fromVersion: String?,
    @ApiModelProperty("升级后版本，填写 LATEST(大写)可直接升级为最新版本", required = true)
    val toVersion: String = "",
    @ApiModelProperty("安装路径，如 /usr/local/services/taylor-1.0", required = true)
    val installPath: String = "",
    @ApiModelProperty("IP数组，不可重复,逗号分隔", required = true)
    val ips: String = "",
    @ApiModelProperty("有效值为\"true\"，不关注请传\"\"，传入为字符串\"true\"时，表示升级前stop", required = false)
    val stop: Boolean?,
    @ApiModelProperty("有效值为\"true\"，不关注请传\"\"，传入为字符串\"true\"时，表示强制升级", required = false)
    val force: Boolean?,
    @ApiModelProperty("有效值为\"true\"，不关注请传\"\"，传入为字符串\"true\"时，表示升级后重启，一般应该传入", required = false)
    val restart: Boolean?,
    @ApiModelProperty("有效值为\"true\"，不关注请传\"\"，传入为字符串\"true\"时，表示热启动", required = false)
    val graceful: Boolean?,
    @ApiModelProperty("分批升级的每批数量", required = false)
    val batchNum: String?,
    @ApiModelProperty("分批升级的间隔时间（秒）", required = false)
    val batchInterval: String?,
    @ApiModelProperty("有效值为\"true\"，不关注请传\"\"，传入为字符串\"true\"时，用install命令，否则用cp命令", required = false)
    val installCp: Boolean?,
    @ApiModelProperty("忽略升级的文件列表，换行符隔开", required = false)
    val ignore: String?,
    @ApiModelProperty("\t有效值为\"true\"，不关注请传\"\"，传入为字符串\"true\"时，" +
            "表示用新的init.xml中的app_name替换旧版本的init.xml中的app_name，非true或者不传入则与之前的保持一致", required = false)
    val updateAppName: Boolean?,
    @ApiModelProperty("有效值为\"false\"，不关注请传\"\"，传入为字符串\"false\"表示与之前的保持一致，" +
            "否则会用新的init.xml中的替换旧的init.xml中的", required = false)
    val updatePort: Boolean?,
    @ApiModelProperty("有效值为\"false\"，不关注请传\"\"，传入为字符串\"false\"表示与之前的保持一致，" +
            "否则会用新的init.xml中的替换旧的init.xml中的。对取了新的app_name的情况," +
            "当变更了init.xml升级时一般传入\"update_start_stop\":\"false\"", required = false)
    val updateStartStop: Boolean?,
    @ApiModelProperty("不关注请传\"\"", required = false)
    val restartApp: String?,
    @ApiModelProperty("启用关联路由，有效值为\"true\"，不关注请传\"\"", required = false)
    val route: Boolean?
) : Element(name, id, status) {
    companion object {
        const val classType = "zhiyunUpdateAsyncEX"
    }

    override fun getTaskAtom(): String {
        return "zhiyunUpdateAsyncEXTaskAtom"
    }

    override fun getClassType() = classType
}
