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

package com.tencent.devops.store.atom.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AtomSelectScorerTest {

    private fun entry(code: String, keywords: String, summary: String? = null) = AtomSelectScorer.Entry(
        atomCode = code,
        summary = summary,
        keywords = AtomSelectScorer.parseKeywords(keywords),
        coverage = "AUTO_EXTRACTED",
        stale = false
    )

    private val innercos = entry(
        code = "innercos",
        keywords = "上传文件,推送构建产物,发布静态资源,COS 对象存储,内部 COS,腾讯云 COS,归档上传结果",
        summary = "把构建产物或静态资源上传到内部或腾讯云 COS，并返回上传结果和 JSON 归档制品。"
    )
    private val uploadArtifactory = entry(
        code = "UploadArtifactory",
        keywords = "归档构件,上传制品,保存构建产物,归档文件,上传到自定义仓库,生成下载链接,制品扫描," +
            "archive artifacts,upload artifacts",
        summary = "归档构建产物到蓝盾流水线仓库或自定义仓库，可附带下载链接、元数据、MD5 校验和制品扫描。"
    )
    private val helm = entry(
        code = "helm",
        keywords = "Helm 部署,部署 release,更新 release,灰度发布,Helm Diff",
        summary = "在蓝盾流水线里部署、更新、灰度、删除 Helm release。"
    )

    @Test
    fun `parseQueries trims, dedups ignoring case, drops short words and caps the count`() {
        Assertions.assertEquals(
            listOf("COS", "上传", "a1", "b2", "c3", "d4", "e5", "f6"),
            AtomSelectScorer.parseQueries(" COS ,cos,上传,x,,a1,b2,c3,d4,e5,f6,g7")
        )
        Assertions.assertEquals(emptyList<String>(), AtomSelectScorer.parseQueries(null))
    }

    @Test
    fun `parseQueries also splits on whitespace and chinese separators`() {
        Assertions.assertEquals(
            listOf("归档", "制品", "artifact", "企业微信", "机器人"),
            AtomSelectScorer.parseQueries("归档 制品  artifact，企业微信、机器人")
        )
    }

    @Test
    fun `matchNames ignores case and whitespace and keeps same-name atoms`() {
        val codesByName = mapOf(
            "拉取构件-流水线仓库(new)" to listOf("BuildArchiveGetTask"),
            AtomSelectScorer.normalizeName("BK AIDev 平台智能体") to listOf("bkaidev_agent", "bkaidev_agent_v2")
        )
        val segments = AtomSelectScorer.parseSegments("拉取构件-流水线仓库(new), bk aidev 平台智能体")
        Assertions.assertEquals(
            listOf("BuildArchiveGetTask", "bkaidev_agent", "bkaidev_agent_v2"),
            AtomSelectScorer.matchNames(segments, codesByName)
        )
        Assertions.assertEquals(emptyList<String>(), AtomSelectScorer.matchNames(listOf("流水线仓库"), codesByName))
    }

    @Test
    fun `exact keyword beats containment which beats summary`() {
        val exact = AtomSelectScorer.score(innercos, listOf("上传文件"))
        val contained = AtomSelectScorer.score(innercos, listOf("对象存储"))
        val summaryOnly = AtomSelectScorer.score(innercos, listOf("静态资源上传到"))
        Assertions.assertEquals(listOf(3, 2, 1), listOf(exact.score, contained.score, summaryOnly.score))
    }

    @Test
    fun `query containing a keyword counts as medium hit`() {
        val hit = AtomSelectScorer.score(helm, listOf("用 Helm Diff 预览"))
        Assertions.assertEquals(2, hit.score)
    }

    @Test
    fun `one query scores once even when several keywords match`() {
        val hit = AtomSelectScorer.score(innercos, listOf("COS"))
        Assertions.assertEquals(1, hit.hitCount)
        Assertions.assertEquals(2, hit.score)
    }

    @Test
    fun `matching is case insensitive and keeps the caller spelling`() {
        val hit = AtomSelectScorer.score(uploadArtifactory, listOf("Upload"))
        Assertions.assertEquals(listOf("Upload"), hit.matched)
    }

    private val codebuddy = entry(
        code = "codebuddy-in-devops",
        keywords = "AI 代码生成,代码审查,CodeBuddy,提示词执行,流水线 AI 助手,自动编码,智能体,代码问答",
        summary = "在蓝盾流水线中用指定模型执行 CodeBuddy Prompt，返回原始 JSON 结果与 Markdown 报告。"
    )
    private val rainbow = entry(
        code = "rainbowgo",
        keywords = "rainbow 配置同步,七彩石配置,rainbow 发布",
        summary = "需要把七彩石或 Rainbow 配置同步到流水线时使用，支持 container 内运行。"
    )

    @Test
    fun `short ascii query only matches whole words`() {
        Assertions.assertEquals(0, AtomSelectScorer.score(rainbow, listOf("AI")).hitCount)
        val hit = AtomSelectScorer.score(codebuddy, listOf("AI"))
        Assertions.assertEquals(2, hit.score)
        Assertions.assertTrue(hit.keywordHit)
    }

    @Test
    fun `short ascii keyword inside a longer query only matches whole words`() {
        val ci = entry(code = "ci", keywords = "ci", summary = null)
        Assertions.assertEquals(0, AtomSelectScorer.score(ci, listOf("pencil")).hitCount)
        Assertions.assertEquals(2, AtomSelectScorer.score(ci, listOf("ci 构建")).score)
    }

    @Test
    fun `longer query still matches inside words`() {
        Assertions.assertEquals(2, AtomSelectScorer.score(codebuddy, listOf("buddy")).score)
    }

    @Test
    fun `summary only hit is not a keyword hit`() {
        val hit = AtomSelectScorer.score(codebuddy, listOf("markdown 报告"))
        Assertions.assertEquals(1, hit.score)
        Assertions.assertFalse(hit.keywordHit)
    }

    @Test
    fun `recall puts keyword hits before summary only hits`() {
        val hits = AtomSelectScorer.recall(
            entries = listOf(innercos, helm),
            queries = listOf("静态资源上传到", "json 归档制品", "灰度"),
            maxSize = 10
        )
        Assertions.assertEquals(listOf("helm", "innercos"), hits.map { it.atomCode })
        Assertions.assertEquals(listOf(1, 2), hits.map { it.hitCount })
    }

    @Test
    fun `recall ranks by hit count before score and drops misses`() {
        val hits = AtomSelectScorer.recall(
            entries = listOf(uploadArtifactory, helm, innercos),
            queries = listOf("COS", "对象存储", "上传", "upload"),
            maxSize = 10
        )
        Assertions.assertEquals(listOf("innercos", "UploadArtifactory"), hits.map { it.atomCode })
        Assertions.assertEquals(listOf(3, 2), hits.map { it.hitCount })
    }

    @Test
    fun `recall respects max size`() {
        val hits = AtomSelectScorer.recall(listOf(uploadArtifactory, innercos), listOf("上传"), maxSize = 1)
        Assertions.assertEquals(1, hits.size)
    }
}
