/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.scm.code.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.tencent.devops.scm.code.git.api.GitCredentialSetter
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.util.FS

class CodeGitCredentialSetter constructor(
    private val privateKey: String,
    private val passPhrase: String?
) : GitCredentialSetter {
    override fun setGitCredential(command: TransportCommand<*, *>) {
        val sshSessionFactory = object : JschConfigSessionFactory() {
            override fun configure(host: OpenSshConfig.Host, session: Session) {
                // 对于 ~/.ssh/known_hosts 中没有 git.yy.com 一项的，会报
                //  "UnknownHostKey: git.yy.com. RSA key fingerprint is xxxx" 的错误，
                //  解决方法1：可以手动在服务器上 git clone 一次 git.yy.com 的代码，并信任该域名IP，即在 ~/.ssh/known_hosts 中添加该项；
                //  解决方法2：在 configure() 中统一加 session.setConfig("StrictHostKeyChecking", "no")， 但会有安全问题，
                //      ref: https://stackoverflow.com/questions/2003419/com-jcraft-jsch-jschexception-unknownhostkey
                //  不过考虑到都是 git.yy.com，所以问题不大；
                //  解决方法3：使用 command.setCredentialsProvider(new GitCodeCredentialsProvider())， 其中 ，
                //      GitCodeCredentialsProvider 中对于如下的 YesOrNo 咨询，都 return true, 有同样的效果
                //  "The authenticity of host 'git.yy.com' can't be established.RSA key fingerprint is
                //      c6:35:2d:a3:de:73:b1:ca:6b:d1:21:49:47:58:1c:f5. Are you sure you want to continue connecting?"

                session.setConfig("StrictHostKeyChecking", "no")
                session.userInfo = object : UserInfo {
                    override fun getPassphrase(): String? {
                        return passPhrase
                    }

                    override fun getPassword(): String? {
                        return null
                    }

                    override fun promptPassword(message: String): Boolean {
                        return false
                    }

                    override fun promptPassphrase(message: String): Boolean {
                        return true
                    }

                    override fun promptYesNo(message: String): Boolean {
                        return false
                    }

                    override fun showMessage(message: String) {}
                }
            }

            override fun createDefaultJSch(fs: FS): JSch {
                val defaultJSch = super.createDefaultJSch(fs)
                defaultJSch.addIdentity(
                    "TempIdentity",
                    privateKey.toByteArray(), null,
                    passPhrase?.toByteArray()
                )
                return defaultJSch
            }
        }
        command.setTransportConfigCallback { transport ->
            val sshTransport = transport as SshTransport
            sshTransport.sshSessionFactory = sshSessionFactory
        }
    }
}