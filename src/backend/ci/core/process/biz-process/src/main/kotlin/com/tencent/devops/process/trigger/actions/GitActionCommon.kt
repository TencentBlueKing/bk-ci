package com.tencent.devops.process.trigger.actions

import com.tencent.devops.process.trigger.common.Constansts
import com.tencent.devops.process.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.trigger.git.pojo.PacGitCred
import com.tencent.devops.process.trigger.git.pojo.PacGitTreeFileInfo
import com.tencent.devops.process.trigger.git.pojo.StreamGitTreeFileInfoType
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
        val yamlPathList = getCIYamlList(action, gitProjectId, ref, cred).toMutableList()

        // 兼容旧的根目录yml文件
        val (isCIYamlExist, blobId) = isCIYamlExist(action, gitProjectId, ref, cred)

        if (isCIYamlExist) {
            yamlPathList.add(Pair(Constansts.ciFileName, blobId))
        }
        return yamlPathList
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
        ).filter { (it.type == "blob") && checkStreamPipelineFile(it.name) && !checkStreamTemplateFile(it.name) }
        return ciFileList.map {
            Pair(Constansts.ciFileDirectoryName + File.separator + it.name, getBlobId(it))
        }.toList()
    }

    private fun checkStreamPipelineFile(fileName: String): Boolean =
        (
            fileName.endsWith(Constansts.ciFileExtensionYml) ||
                fileName.endsWith(Constansts.ciFileExtensionYaml)
            ) &&
            // 加以限制：最多仅限一级子目录
            (fileName.count { it == '/' } <= 1)

    private fun checkStreamTemplateFile(fileName: String): Boolean = fileName.startsWith("templates/")

    fun checkStreamPipelineAndTemplateFile(fullPath: String): Boolean =
        if (fullPath.startsWith(Constansts.ciFileDirectoryName)) {
            val removePrefix = fullPath.removePrefix(Constansts.ciFileDirectoryName + "/")
            checkStreamPipelineFile(removePrefix) || checkStreamTemplateFile(removePrefix)
        } else false

    private fun getBlobId(f: PacGitTreeFileInfo?): String? {
        return if (f != null && f.type == StreamGitTreeFileInfoType.BLOB.value && !f.id.isNullOrBlank()) {
            f.id
        } else {
            null
        }
    }

    /**
     * @return isExist,blobId
     */
    private fun isCIYamlExist(
        action: BaseAction,
        gitProjectId: String,
        ref: String?,
        cred: PacGitCred?
    ): Pair<Boolean, String?> {
        val ciFileList = action.api.getFileTree(
            gitProjectId = gitProjectId,
            cred = cred ?: action.getGitCred(),
            path = "",
            ref = ref?.let { getTriggerBranch(it) },
            recursive = false,
            retry = ApiRequestRetryInfo(true)
        ).filter { it.name == Constansts.ciFileName }
        return Pair(ciFileList.isNotEmpty(), getBlobId(ciFileList.ifEmpty { null }?.first()))
    }

    fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
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
}
