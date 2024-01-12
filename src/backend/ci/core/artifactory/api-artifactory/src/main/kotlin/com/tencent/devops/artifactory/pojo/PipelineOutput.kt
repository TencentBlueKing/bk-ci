package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.pojo.TaskReport
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线产出物")
data class PipelineOutput(
    @Schema(description = "仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @Schema(description = "产出物名", required = true)
    val name: String,

    @Schema(description = "文件全名", required = false)
    val fullName: String? = null,
    @Schema(description = "文件路径", required = false)
    val path: String? = null,
    @Schema(description = "文件全路径", required = false)
    val fullPath: String? = null,
    @Schema(description = "文件大小(byte)", required = false)
    val size: Long? = null,
    @Schema(description = "是否文件夹", required = false)
    val folder: Boolean? = null,
    @Schema(description = "元数据", required = false)
    val properties: List<Property>? = null,
    @Schema(description = "app版本", required = false)
    val appVersion: String? = null,
    @Schema(description = "下载短链接", required = false)
    val shortUrl: String? = null,
    @Schema(description = "下载链接", required = false)
    var downloadUrl: String? = null,
    @Schema(description = "MD5", required = false)
    var md5: String? = null,

    @Schema(description = "首页地址", required = false)
    val indexFileUrl: String? = null,
    @Schema(description = "报告类型", required = false)
    val reportType: String? = null,

    @Schema(description = "创建时间", required = true)
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
