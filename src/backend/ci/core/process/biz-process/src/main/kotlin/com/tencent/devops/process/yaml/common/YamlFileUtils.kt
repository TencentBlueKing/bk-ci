package com.tencent.devops.process.yaml.common

import com.tencent.devops.process.pojo.pipeline.enums.YamlResourceType
import org.joda.time.DateTime
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object YamlFileUtils {

    fun checkYamlPipelineFile(fileName: String): Boolean =
        (
            fileName.endsWith(Constansts.ciFileExtensionYml) ||
                fileName.endsWith(Constansts.ciFileExtensionYaml)
            ) &&
            // 加以限制：最多仅限一级子目录
            (fileName.count { it == '/' } <= 1)

    fun checkYamlTemplateFile(fileName: String): Boolean = fileName.startsWith("templates/")

    fun checkStreamPipelineAndTemplateFile(fullPath: String): Boolean =
        if (fullPath.startsWith(Constansts.ciFileDirectoryName)) {
            val removePrefix = fullPath.removePrefix(Constansts.ciFileDirectoryName + "/")
            checkYamlPipelineFile(removePrefix) || checkYamlTemplateFile(removePrefix)
        } else {
            false
        }

    fun trimRef(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    /**
     * 扩展引用名称
     * @param name 原始名称
     * @param prefix 前缀
     * @return 扩展后的名称
     */
    fun expandRef(name: String, prefix: String): String {
        val trimmedPrefix = prefix.removeSuffix("/")
        return if (name.startsWith(trimmedPrefix)) {
            name
        } else {
            "$trimmedPrefix/$name"
        }
    }

    fun getCommitTimeStamp(commitTimeStamp: String?): String {
        return if (commitTimeStamp.isNullOrBlank()) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            formatter.format(Date())
        } else {
            val time = DateTime.parse(commitTimeStamp)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            sdf.format(time.toDate())
        }
    }

    fun isCiFile(name: String): Boolean {
        if (name == Constansts.ciFileName) {
            return true
        }
        return name.startsWith(Constansts.ciFileDirectoryName) &&
            (name.endsWith(Constansts.ciFileExtensionYml) || name.endsWith(Constansts.ciFileExtensionYaml))
    }

    fun getCiDirectory(filePath: String): String {
        return filePath.let { it.substring(0, it.indexOfLast { c -> c == '/' }) }
    }

    fun getCiFileName(filePath: String): String {
        return filePath.removePrefix(".ci/")
    }

    fun getCiTemplateName(filePath: String): String {
        return filePath.removePrefix("${Constansts.ciTemplateDirectoryName}${File.separator}")
    }

    fun getCiFilePath(fileName: String): String {
        return "${Constansts.ciFileDirectoryName}${File.separator}$fileName"
    }

    fun getSourceRef(fork: Boolean, sourceFullName: String, sourceBranch: String): String {
        return if (fork) {
            "$sourceFullName:$sourceBranch"
        } else {
            sourceBranch
        }
    }

    fun isTemplateFile(filePath: String): Boolean {
        return filePath.startsWith(".ci/templates")
    }

    fun getYamlResourceType(filePath: String, fileContent: String): YamlResourceType {
        // TODO PAC 局部模版时,需要读取文件内容判断
        return if (isTemplateFile(filePath)) {
            YamlResourceType.PIPELINE_TEMPLATE
        } else {
            YamlResourceType.PIPELINE
        }
    }
}
