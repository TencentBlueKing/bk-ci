package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-文件信息")
data class FileInfo(
    @ApiModelProperty("文件名", required = true)
    val name: String,
    @ApiModelProperty("文件全名", required = true)
    val fullName: String,
    @ApiModelProperty("文件路径", required = true)
    val path: String,
    @ApiModelProperty("文件全路径", required = true)
    val fullPath: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("是否文件夹", required = true)
    val folder: Boolean,
    @ApiModelProperty("更新时间", required = true)
    val modifiedTime: Long,
    @ApiModelProperty("仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("元数据", required = true)
    val properties: List<Property>?,
    @ApiModelProperty("app版本", required = true)
    val appVersion: String? = null,
    @ApiModelProperty("下载短链接", required = true)
    val shortUrl: String? = null
) : Comparable<FileInfo> {
    constructor(name: String, fullName: String, path: String, fullPath: String, size: Long, folder: Boolean, modifiedTime: Long, artifactoryType: ArtifactoryType) :
            this(name, fullName, path, fullPath, size, folder, modifiedTime, artifactoryType, null)

    override fun compareTo(other: FileInfo): Int {
        // 都是加固的，则按名字排
        return if ((this.name.endsWith(".apk") || this.name.endsWith(".ipa")) &&
                (other.name.endsWith(".apk") || other.name.endsWith(".ipa"))) {
            if ((this.name.endsWith(".shell.apk") || this.name.endsWith("_enterprise_sign.ipa")) &&
                    (other.name.endsWith(".shell.apk") || other.name.endsWith("_enterprise_sign.ipa"))) {
                this.name.compareTo(other.name)
            } else if ((this.name.endsWith(".shell.apk") || this.name.endsWith("_enterprise_sign.ipa")) &&
                    (!other.name.endsWith(".shell.apk") && !other.name.endsWith("_enterprise_sign.ipa"))) {
                -1
            } else {
                1
            }
        } else if ((this.name.endsWith(".apk") || this.name.endsWith(".ipa")) &&
                (!other.name.endsWith(".apk") && !other.name.endsWith(".ipa"))) {
            -1
        } else {
            1
        }
    }
}

// fun main(args: Array<String>) {
//     val fileList = mutableListOf<FileInfo>()
//     fileList.add(FileInfo("soda0.apk", "/soda.apk", "/", "/", 100, false, 1, ArtifactoryType.PIPELINE, null, null, null))
//     fileList.add(FileInfo("soda1.shell.apk", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE, null, null, null))
//     fileList.add(FileInfo("soda2_enterprise_sign.ipa", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE,null, null, null))
//     fileList.add(FileInfo("soda3_enterprise_sign.ipa", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE,null, null, null))
//     fileList.add(FileInfo("soda4.shell.apk", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE,null, null, null))
//     fileList.add(FileInfo("soda5.shell.apk", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE,null, null, null))
//     fileList.add(FileInfo("soda6_enterprise_sign.apk", "/soda.apk", "/", "/", 100, false, 1, ArtifactoryType.PIPELINE, null, null, null))
//     fileList.add(FileInfo("soda7_enterprise_sign.apk", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE,null, null, null))
//     fileList.add(FileInfo("soda8.tar", "/soda.apk", "/", "/", 100, false, 1, ArtifactoryType.PIPELINE, null, null, null))
//     fileList.add(FileInfo("soda9.xxx", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE,null, null, null))
//     fileList.add(FileInfo("soda10.ipa", "/soda.apk", "/", "/", 100, false, 1,  ArtifactoryType.PIPELINE,null, null, null))
//
//     val sortList = fileList.sorted()
//     fileList.forEach {
//         println(it.name)
//     }
//     println("=============================")
//     sortList.forEach {
//         println(it.name)
//     }
//
// }