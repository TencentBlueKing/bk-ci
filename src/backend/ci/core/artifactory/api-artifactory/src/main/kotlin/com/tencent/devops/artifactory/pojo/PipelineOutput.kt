package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.pojo.TaskReport
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线产出物")
data class PipelineOutput(
    @ApiModelProperty("仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("产出物名", required = true)
    val name: String,

    @ApiModelProperty("文件全名", required = false)
    val fullName: String? = null,
    @ApiModelProperty("文件路径", required = false)
    val path: String? = null,
    @ApiModelProperty("文件全路径", required = false)
    val fullPath: String? = null,
    @ApiModelProperty("文件大小(byte)", required = false)
    val size: Long? = null,
    @ApiModelProperty("是否文件夹", required = false)
    val folder: Boolean? = null,
    @ApiModelProperty("元数据", required = false)
    val properties: List<Property>? = null,
    @ApiModelProperty("app版本", required = false)
    val appVersion: String? = null,
    @ApiModelProperty("下载短链接", required = false)
    val shortUrl: String? = null,
    @ApiModelProperty("下载链接", required = false)
    var downloadUrl: String? = null,
    @ApiModelProperty("MD5", required = false)
    var md5: String? = null,

    @ApiModelProperty("首页地址", required = false)
    val indexFileUrl: String? = null,
    @ApiModelProperty("报告类型", required = false)
    val reportType: String? = null,

    @ApiModelProperty("创建时间", required = true)
    val createTime: Long
) {
    companion object {
        fun convertFromFileInfo(fileInfo: FileInfo): PipelineOutput {
            with(fileInfo) {
                return PipelineOutput(
                    artifactoryType = artifactoryType,
                    name = name,
                    fullName = fullName,
                    path = path,
                    fullPath = fullPath,
                    size = size,
                    folder = folder,
                    properties = properties,
                    appVersion = appVersion,
                    shortUrl = shortUrl,
                    downloadUrl = downloadUrl,
                    md5 = md5,
                    createTime = modifiedTime
                )
            }
        }

        fun convertFromTaskReport(taskReport: TaskReport): PipelineOutput {
            with(taskReport) {
                return PipelineOutput(
                    artifactoryType = ArtifactoryType.REPORT,
                    name = name,
                    indexFileUrl = indexFileUrl,
                    reportType = type,
                    createTime = createTime.timestamp()
                )
            }
        }
    }
}
