package com.tencent.devops.process.yaml.actions

import com.tencent.devops.process.yaml.common.Constansts
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitCred
import com.tencent.devops.process.yaml.git.pojo.PacGitTreeFileInfo
import com.tencent.devops.process.yaml.git.pojo.PacGitTreeFileInfoType
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object GitActionCommon {

    private val logger = LoggerFactory.getLogger(GitActionCommon::class.java)

    /**
     * 拿到所有的ci文件的文件列表
     * @return file,blobId
     */
    fun getYamlPathList(
        action: BaseAction,
        gitProjectId: String,
        ref: String?,
        cred: PacGitCred? = null
    ): MutableList<Pair<String, String?>> {
        // 获取指定目录下所有yml文件
        return getCIYamlList(action, gitProjectId, ref, cred).toMutableList()
    }

    /**
     * @return name,blobId
     */
    private fun getCIYamlList(
        action: BaseAction,
        gitProjectId: String,
        ref: String?,
        cred: PacGitCred?
    ): List<Pair<String, String?>> {
        val ciFileList = action.api.getFileTree(
            gitProjectId = gitProjectId,
            cred = cred ?: action.getGitCred(),
            path = Constansts.ciFileDirectoryName,
            ref = ref?.let { getTriggerBranch(it) },
            recursive = true,
            retry = ApiRequestRetryInfo(true)
        ).filter { (it.type == "blob") && checkYamlPipelineFile(it.name) && !checkYamlTemplateFile(it.name) }
        return ciFileList.map {
            Pair(Constansts.ciFileDirectoryName + File.separator + it.name, getBlobId(it))
        }.toList()
    }

    fun checkYamlPipelineFile(fileName: String): Boolean =
        (
            fileName.endsWith(Constansts.ciFileExtensionYml) ||
                fileName.endsWith(Constansts.ciFileExtensionYaml)
            ) &&
            // 加以限制：最多仅限一级子目录
            (fileName.count { it == '/' } <= 1)

    private fun checkYamlTemplateFile(fileName: String): Boolean = fileName.startsWith("templates/")

    fun checkStreamPipelineAndTemplateFile(fullPath: String): Boolean =
        if (fullPath.startsWith(Constansts.ciFileDirectoryName)) {
            val removePrefix = fullPath.removePrefix(Constansts.ciFileDirectoryName + "/")
            checkYamlPipelineFile(removePrefix) || checkYamlTemplateFile(removePrefix)
        } else false

    private fun getBlobId(f: PacGitTreeFileInfo?): String? {
        return if (f != null && f.type == PacGitTreeFileInfoType.BLOB.value && !f.id.isNullOrBlank()) {
            f.id
        } else {
            null
        }
    }

    fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    /**
     * 如果是fork库,则分支使用fork namespace:branch
     */
    fun getRealRef(action: BaseAction, branch: String): String {
        val fork = action.data.eventCommon.fork
        val sourceGitNamespace = action.data.eventCommon.sourceGitNamespace
        return if (fork) {
            "$sourceGitNamespace:$branch"
        } else {
            branch
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
}
