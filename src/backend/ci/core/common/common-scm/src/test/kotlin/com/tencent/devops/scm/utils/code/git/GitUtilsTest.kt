/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.scm.utils.code.git

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GitUtilsTest {

    private val domain = "github.com"
    private val repoName = "Tencent/bk-ci"

    @Test
    fun getDomainAndRepoName4Http() {
        var domainAndRepoName = GitUtils.getDomainAndRepoName("https://github.com/Tencent/bk-ci.git")
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
        domainAndRepoName = GitUtils.getDomainAndRepoName("http://github.com/Tencent/bk-ci.git")
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
    }

    @Test
    fun getSpecialDomainAndRepoName() {
        val baseRepoName = "aaa-bbb/aaa_github"
        var domainAndRepoName = GitUtils.getDomainAndRepoName("https://github.com/aaa-bbb/aaa_github.git")
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(baseRepoName, domainAndRepoName.second)
        domainAndRepoName = GitUtils.getDomainAndRepoName("https://github.com/aaa-bbb/aaa_github")
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(baseRepoName, domainAndRepoName.second)
        domainAndRepoName = GitUtils.getDomainAndRepoName("git@github.com:aaa-bbb/aaa_github.git")
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(baseRepoName, domainAndRepoName.second)
    }

    @Test
    fun getGitApiUrl() {
        var apiUrl = "http://aaa.com/api/v3"
        var repoApiUrl = "http://github.com/api/v3"
        val httpsApiUrl = "https://aaa.com/api/v3"
        val httpsRepoApiUrl = "https://github.com/api/v3"
        var actual = GitUtils.getGitApiUrl(apiUrl, "http://github.com/Tencent/bk-ci.git")
        assertEquals(repoApiUrl, actual)
        actual = GitUtils.getGitApiUrl(apiUrl, "http://aaa.com/Tencent/bk-ci.git")
        assertEquals(apiUrl, actual)
        actual = GitUtils.getGitApiUrl(apiUrl, "https://aaa.com/Tencent/bk-ci.git")
        assertEquals(apiUrl, actual)
        actual = GitUtils.getGitApiUrl(apiUrl, "https://github.com/Tencent/bk-ci.git")
        assertEquals(repoApiUrl, actual)
        val errorApiUrl = "api/v3"
        actual = GitUtils.getGitApiUrl(errorApiUrl, "http://aaa.com/Tencent/bk-ci.git")
        assertEquals(apiUrl, actual)
        actual = GitUtils.getGitApiUrl(errorApiUrl, "https://aaa.com/Tencent/bk-ci.git")
        assertEquals(httpsApiUrl, actual)
        actual = GitUtils.getGitApiUrl(errorApiUrl, "https://github.com/Tencent/bk-ci.git")
        assertEquals(httpsRepoApiUrl, actual)

        apiUrl = "http://aaa.com:8080/api/v3"
        repoApiUrl = "http://gitlab.com:8080/api/v3"
        actual = GitUtils.getGitApiUrl(apiUrl, "http://gitlab.com:8080/Tencent/bk-ci.git")
        assertEquals(repoApiUrl, actual)
        actual = GitUtils.getGitApiUrl(apiUrl, "http://aaa.com:8080/Tencent/bk-ci.git")
        assertEquals(apiUrl, actual)
    }

    @Test
    fun getDomainAndRepoName4SSH() {
        val domainAndRepoName = GitUtils.getDomainAndRepoName("git@github.com:Tencent/bk-ci.git")
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
    }

    @Test
    fun getDomainAndRepoName() {
        var url = "git@github.com:Tencent/bk-ci.git"
        var domainAndRepoName = GitUtils.getDomainAndRepoName(url)
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
        url = "git@github.com:2244/Tencent/bk-ci.git"
        domainAndRepoName = GitUtils.getDomainAndRepoName(url)
        assertEquals("$domain:2244", domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
        url = "ssh://git@github.com:2244/Tencent/bk-ci.git"
        domainAndRepoName = GitUtils.getDomainAndRepoName(url)
        assertEquals("$domain:2244", domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
        url = "http://github.com/Tencent/bk-ci.git"
        domainAndRepoName = GitUtils.getDomainAndRepoName(url)
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
        url = "https://github.com/Tencent/bk-ci.git"
        domainAndRepoName = GitUtils.getDomainAndRepoName(url)
        assertEquals(domain, domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
        url = "http://github.com:8080/Tencent/bk-ci.git"
        domainAndRepoName = GitUtils.getDomainAndRepoName(url)
        assertEquals("$domain:8080", domainAndRepoName.first)
        assertEquals(repoName, domainAndRepoName.second)
    }

    @Test
    fun getProjectName() {
        var projectName = GitUtils.getProjectName("git@github.com:Tencent/bk-ci.git")
        assertEquals(repoName, projectName)
        projectName = GitUtils.getProjectName("git@github.com:2Tencent/bk-ci.git")
        assertEquals("2$repoName", projectName)
        projectName = GitUtils.getProjectName("git@git.xxx.com:Tencent/bk-ci.git")
        assertEquals(repoName, projectName)
        projectName = GitUtils.getProjectName("https://github.com/Tencent/bk-ci.git")
        assertEquals(repoName, projectName)
        projectName = GitUtils.getProjectName("http://github.com/Tencent/bk-ci.git")
        assertEquals(repoName, projectName)
        projectName = GitUtils.getProjectName("http://github.com/Tencent/bk-ci")
        assertEquals(repoName, projectName)
    }

    @Test
    fun isLegalHttpUrl() {
        Assertions.assertTrue(GitUtils.isLegalHttpUrl("https://github.com/Tencent/bk-ci.git"))
        Assertions.assertTrue(GitUtils.isLegalHttpUrl("http://github.com/Tencent/bk-ci.git"))
        Assertions.assertTrue(GitUtils.isLegalHttpUrl("http://github.com:8080/Tencent/bk-ci.git"))
        Assertions.assertFalse(GitUtils.isLegalHttpUrl("http://github.com:8080/Tencent/bk-ci"))
        Assertions.assertFalse(GitUtils.isLegalHttpUrl("git@git.xxx.com:Tencent/bk-ci.git"))
    }

    @Test
    fun isLegalSshUrl() {
        Assertions.assertFalse(GitUtils.isLegalSshUrl("https://github.com/Tencent/bk-ci.git"))
        Assertions.assertFalse(GitUtils.isLegalSshUrl("http://github.com/Tencent/bk-ci.git"))
        Assertions.assertFalse(GitUtils.isLegalSshUrl("http://github.com:8080/Tencent/bk-ci.git"))
        Assertions.assertFalse(GitUtils.isLegalSshUrl("http://github.com:8080/Tencent/bk-ci"))
        Assertions.assertTrue(GitUtils.isLegalSshUrl("git@git.xxx.com:Tencent/bk-ci.git"))
        Assertions.assertFalse(GitUtils.isLegalHttpUrl("git@git.xxx.com:Tencent/bk-ci"))
    }

    @Test
    fun getRepoGroupAndName() {
        assertEquals(GitUtils.getRepoGroupAndName("Tencent/bk-ci"), Pair("Tencent", "bk-ci"))
        assertEquals(GitUtils.getRepoGroupAndName("Tencent/plugin/bk-ci"), Pair("Tencent/plugin", "bk-ci"))
    }

    @Test
    fun getShortSha() {
        assertEquals(GitUtils.getShortSha("e49ca115cf9a5fd433cbb539ee37316b67bde243"), "e49ca115")
        assertEquals(GitUtils.getShortSha(""), "")
    }
}
