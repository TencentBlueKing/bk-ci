package com.tencent.devops.scm.services

import com.tencent.devops.scm.pojo.SvnFileInfo
import com.tencent.devops.scm.pojo.enums.SvnFileType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNProperties
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.ByteArrayOutputStream

@Service
class SvnService {

    companion object {
        private val logger = LoggerFactory.getLogger(SvnService::class.java)
    }

    fun getFileContent(
        url: String,
        userId: String,
        svnType: String,
        filePath: String,
        reversion: Long,
        credential1: String,
        credential2: String?
    ): String {
        logger.info("get svn file content: $url, $userId, $svnType, $filePath, $reversion")
        val startEpoch = System.currentTimeMillis()
        try {
            val bos = ByteArrayOutputStream()
            val svnUrl = SVNURL.parseURIEncoded(url)
            val repository = SVNRepositoryFactory.create(svnUrl)
            val auth = when (svnType.toUpperCase()) {
                "HTTP" -> SVNPasswordAuthentication.newInstance(
                    credential1,
                    credential2?.toCharArray(),
                    false,
                    svnUrl,
                    false
                )
                "SSH" -> SVNSSHAuthentication.newInstance(
                    userId,
                    credential1.toCharArray(),
                    credential2?.toCharArray(),
                    22,
                    false,
                    svnUrl,
                    false
                )
                else -> throw RuntimeException("unknown svn repo type: ${svnType.toUpperCase()}")
            }
            val basicAuthenticationManager = BasicAuthenticationManager(arrayOf(auth))
            repository.authenticationManager = basicAuthenticationManager
            repository.getFile(filePath, reversion, SVNProperties(), bos)
            return bos.toString()
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get svn file content")
        }
    }

    fun getDirectories(
        url: String,
        userId: String,
        svnType: String,
        svnPath: String?,
        revision: Long,
        credential1: String,
        credential2: String,
        credential3: String?
    ): List<SvnFileInfo> {
        logger.info("get svn dir: [url=$url, userId=$userId, svnType=$svnType]")
        val startEpoch = System.currentTimeMillis()
        try {
            val svnUrl = SVNURL.parseURIEncoded(url)
            val repository = SVNRepositoryFactory.create(svnUrl)
            val auth = when (svnType.toUpperCase()) {
                "HTTP" -> SVNPasswordAuthentication.newInstance(
                    credential1,
                    credential2?.toCharArray(),
                    false,
                    svnUrl,
                    false
                )
                "SSH" -> SVNSSHAuthentication.newInstance(
                    credential1,
                    credential2.toCharArray(),
                    credential3?.toCharArray(),
                    22,
                    false,
                    svnUrl,
                    false
                )
                else -> throw RuntimeException("unknown svn repo type: ${svnType.toUpperCase()}")
            }
            val entries = mutableListOf<SVNDirEntry>()
            val basicAuthenticationManager = BasicAuthenticationManager(arrayOf(auth))
            repository.authenticationManager = basicAuthenticationManager
            repository.getDir(svnPath ?: "", revision, SVNProperties(), entries)

            return entries.map {
                val type = SvnFileType.valueOf(it.kind.toString().toUpperCase())
                val name = it.name
                SvnFileInfo(type, name)
            }
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the directories")
        }
    }
}
