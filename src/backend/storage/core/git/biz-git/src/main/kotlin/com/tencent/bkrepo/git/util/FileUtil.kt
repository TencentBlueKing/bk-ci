package com.tencent.bkrepo.git.util

import org.eclipse.jgit.util.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

object FileUtil {

    private val logger = LoggerFactory.getLogger(FileUtil::class.java)

    @Throws(IOException::class)
    fun entryName(f: File, srcDir: String): String {
        if (f.isDirectory) {
            f.canonicalPath.replace(srcDir, "")
        }
        return f.parent.replace(srcDir, "") + File.separator + f.name
    }

    @Throws(IOException::class)
    fun getAllFiles(dir: File, filesToArchive: MutableList<File>, onlyFile: Boolean) {
        var files: Array<File>
        requireNotNull(dir.listFiles().also { files = it })
        for (i in files.indices) {
            val file = files[i]
            if (file.isDirectory) {
                getAllFiles(file, filesToArchive, onlyFile)
            }
            if (!onlyFile || file.isFile) {
                filesToArchive.add(file)
            }
        }
    }

    fun recursiveDelete(dir: File) {
        val options = (
            FileUtils.RECURSIVE or FileUtils.RETRY
                or FileUtils.SKIP_MISSING
            )
        try {
            FileUtils.delete(dir, options)
        } catch (e: IOException) {
            logger.info("Failed to delete: ", e)
        }
    }
}
