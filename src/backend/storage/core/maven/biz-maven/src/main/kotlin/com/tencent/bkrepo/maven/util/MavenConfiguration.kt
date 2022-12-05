/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.maven.constants.CHECKSUM_POLICY
import com.tencent.bkrepo.maven.constants.MAX_UNIQUE_SNAPSHOTS
import com.tencent.bkrepo.maven.constants.SNAPSHOT_BEHAVIOR
import com.tencent.bkrepo.maven.enum.SnapshotBehaviorType
import com.tencent.bkrepo.maven.pojo.MavenRepoConf
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotNonUniqueUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotUniqueUri

object MavenConfiguration {
    fun RepositoryConfiguration.toMavenRepoConf(): MavenRepoConf {
        val checksumPolicy = this.getIntegerSetting(CHECKSUM_POLICY) ?: 0
        val snapshotBehavior = (
            this.getIntegerSetting(SNAPSHOT_BEHAVIOR)?.let {
                when (it) {
                    1 -> SnapshotBehaviorType.NON_UNIQUE
                    2 -> SnapshotBehaviorType.DEPLOYER
                    // 建议配置为 0
                    else -> SnapshotBehaviorType.UNIQUE
                }
            }
            ) ?: SnapshotBehaviorType.UNIQUE
        val maxUniqueSnapshots = this.getIntegerSetting(MAX_UNIQUE_SNAPSHOTS)
        return MavenRepoConf(checksumPolicy, snapshotBehavior, maxUniqueSnapshots)
    }

    /**
     * 检查构件路径是否与仓库 [SnapshotBehaviorType] 冲突
     * [SnapshotBehaviorType.DEPLOYER] 不做处理
     */
    fun MavenRepoConf.versionBehaviorConflict(artifactUrl: String): Boolean {
        if (this.mavenSnapshotVersionBehavior == SnapshotBehaviorType.DEPLOYER) {
            return false
        }
        return (
            (
                this.mavenSnapshotVersionBehavior == SnapshotBehaviorType.UNIQUE &&
                    artifactUrl.isSnapshotNonUniqueUri()
                ) ||
                (
                    this.mavenSnapshotVersionBehavior == SnapshotBehaviorType.NON_UNIQUE &&
                        artifactUrl.isSnapshotUniqueUri()
                    )
            )
    }
}
