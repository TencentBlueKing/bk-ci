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

package com.tencent.devops.dockerhost.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DockerHostConfig {

    @Value("\${dockerCli.dockerConfig:/root/.docke}")
    var dockerConfig: String? = null

    @Value("\${dockerCli.apiVersion:1.23}")
    var apiVersion: String? = null

    @Value("\${dockerCli.registryUrl:#{null}}")
    var registryUrl: String? = null

    @Value("\${dockerCli.registryUsername:#{null}}")
    var registryUsername: String? = null

    @Value("\${dockerCli.registryPassword:#{null}}")
    var registryPassword: String? = null

    @Value("\${dockerCli.volumeWorkspace:/data/devops/workspace}")
    var volumeWorkspace: String? = null

    @Value("\${dockerCli.volumeCommonEnv:/data/devops/common_env}")
    var volumeCommonEnv: String? = null

    @Value("\${dockerCli.volumeProjectShare:/data/devops/share}")
    var volumeProjectShare: String? = null

    @Value("\${dockerCli.volumeMavenRepo:/root/.m2/repository}")
    var volumeMavenRepo: String? = null

    @Value("\${dockerCli.volumeNpmPrefix:/root/Downloads/npm/prefix}")
    var volumeNpmPrefix: String? = null

    @Value("\${dockerCli.volumeNpmCache:root/Downloads/npm/cache}")
    var volumeNpmCache: String? = null

    @Value("\${dockerCli.volumeNpmRc://root/.npmrc}")
    var volumeNpmRc: String? = null

    @Value("\${dockerCli.volumeCcache:/root/.ccache}")
    var volumeCcache: String? = null

    @Value("\${dockerCli.volumeApps:/data/devops/apps/}")
    var volumeApps: String? = null

    @Value("\${dockerCli.volumeCodecc:#{null}}")
    var volumeCodecc: String? = null

    @Value("\${dockerCli.volumeInit:/data/init.sh}")
    var volumeInit: String? = null

    @Value("\${dockerCli.volumeSleep:/data/sleep.sh}")
    var volumeSleep: String? = null

    @Value("\${dockerCli.volumeLogs:/data/logs/}")
    var volumeLogs: String? = null

    @Value("\${dockerCli.volumeGradleCache:/root/.gradle/caches}")
    var volumeGradleCache: String? = null

    @Value("\${dockerCli.volumeGolangCache:/root/go/pkg/mod}")
    var volumeGolangCache: String? = null

    @Value("\${dockerCli.volumeSbtCache:/root/.ivy2}")
    var volumeSbtCache: String? = null

    @Value("\${dockerCli.volumeSbt2Cache:/root/.cache}")
    var volumeSbt2Cache: String? = null

    @Value("\${dockerCli.volumeYarnCache:/usr/local/share/.cache/}")
    var volumeYarnCache: String? = null

    @Value("\${dockerCli.hostPathWorkspace:#{null}}")
    var hostPathWorkspace: String? = null

    @Value("\${dockerCli.hostPathCommonEnv:#{null}}")
    var hostPathCommonEnv: String? = null

    @Value("\${dockerCli.hostPathProjectShare:#{null}}")
    var hostPathProjectShare: String? = null

    @Value("\${dockerCli.hostPathMavenRepo:#{null}}")
    var hostPathMavenRepo: String? = null

    @Value("\${dockerCli.hostPathNpmPrefix:#{null}}")
    var hostPathNpmPrefix: String? = null

    @Value("\${dockerCli.hostPathNpmCache:#{null}}")
    var hostPathNpmCache: String? = null

    @Value("\${dockerCli.hostPathNpmRc:#{null}}")
    var hostPathNpmRc: String? = null

    @Value("\${dockerCli.hostPathCcache:#{null}}")
    var hostPathCcache: String? = null

    @Value("\${dockerCli.hostPathApps:#{null}}")
    var hostPathApps: String? = null

    @Value("\${dockerCli.hostPathCodecc:#{null}}")
    var hostPathCodecc: String? = null

    @Value("\${dockerCli.hostPathInit:#{null}}")
    var hostPathInit: String? = null

    @Value("\${dockerCli.hostPathSleep:#{null}}")
    var hostPathSleep: String? = null

    @Value("\${dockerCli.hostPathLogs:#{null}}")
    var hostPathLogs: String? = null

    @Value("\${dockerCli.hostPathGradleCache:#{null}}")
    var hostPathGradleCache: String? = null

    @Value("\${dockerCli.hostPathGolangCache:#{null}}")
    var hostPathGolangCache: String? = null

    @Value("\${dockerCli.hostPathSbtCache:#{null}}")
    var hostPathSbtCache: String? = null

    @Value("\${dockerCli.hostPathSbt2Cache:#{null}}")
    var hostPathSbt2Cache: String? = null

    @Value("\${dockerCli.hostPathYarnCache:#{null}}")
    var hostPathYarnCache: String? = null

    @Value("\${dockerCli.hostPathLinkDir}")
    var hostPathLinkDir: String = "/tmp/bkci"

    @Value("\${dockerCli.hostPathHosts}")
    var hostPathHosts: String? = null

    @Value("\${dockerCli.hostPathOverlayfsCache:#{null}}")
    var hostPathOverlayfsCache: String? = "/data/overlayfscache"

    @Value("\${dockerCli.shareProjectCodeWhiteList}")
    var shareProjectCodeWhiteList: String? = null

    @Value("\${dockerCli.memoryLimitBytes:34359738368}")
    var memory: Long = 34359738368L // 1024 * 1024 * 1024 * 32 Memory limit in bytes. 32G

    @Value("\${dockerCli.cpuPeriod:10000}")
    var cpuPeriod: Int = 10000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dockerCli.cpuQuota:160000}")
    var cpuQuota: Int = 160000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dockerCli.blkioDeviceWriteBps:125829120}")
    var blkioDeviceWriteBps: Long = 125829120 // 默认磁盘IO写速率：120M/s

    @Value("\${dockerCli.blkioDeviceReadBps:125829120}")
    var blkioDeviceReadBps: Long = 125829120 // 默认磁盘IO读速率：120M/s

    @Value("\${dockerCli.dockerAgentPath}")
    var dockerAgentPath: String? = null

    @Value("\${dockerCli.downloadDockerAgentUrl}")
    var downloadDockerAgentUrl: String? = null

    @Value("\${dockerCli.landunEnv}")
    var landunEnv: String? = null

    @Value("\${dockerCli.localImageCacheDays}")
    var localImageCacheDays: Int = 7

    @Value("\${dockerhost.mode:#{null}}")
    var dockerhostMode: String? = null

    @Value("\${dockerhost.dispatch.urlPrefix:ms/dispatch-docker}")
    var dispatchUrlPrefix: String? = "ms/dispatch-docker"

    @Value("\${dockerhost.gatewayHeaderTag:#{null}}")
    var gatewayHeaderTag: String? = null

    @Value("\${dockerhost.localIp:#{null}}")
    var dockerhostLocalIp: String? = null

    /**
     * 运行dockerRun启动mysql,redis服务时的初始映射端口
     */
    @Value("\${dockerhost.dockerRun.startPort:20000}")
    var dockerRunStartPort: Int? = 20000

    /**
     * DockerHost母机开启容器负载检测的CPU阈值
     */
    @Value("\${dockerhost.elasticity.systemCpuThreshold:80}")
    var elasticitySystemCpuThreshold: Int? = 80

    /**
     * DockerHost母机开启容器负载检测的应用内存阈值
     */
    @Value("\${dockerhost.elasticity.systemMemThreshold:80}")
    var elasticitySystemMemThreshold: Int? = 80

    /**
     * DockerHost容器负载弹性扩缩cpuPeriod配置--指定容器对CPU的使用要在多长时间内做一次重新分配
     */
    @Value("\${dockerhost.elasticity.cpuPeriod:10000}")
    var elasticityCpuPeriod: Int? = 10000

    /**
     * DockerHost容器负载弹性扩缩cpuQuota配置--指定在这个周期内，最多可以有多少时间用来跑这个容器
     */
    @Value("\${dockerhost.elasticity.cpuQuota:80000}")
    var elasticityCpuQuota: Int? = 80000

    /**
     * DockerHost容器负载弹性扩缩，容器CPU阈值
     */
    @Value("\${dockerhost.elasticity.cpuThreshold:80}")
    var elasticityCpuThreshold: Int? = 80

    /**
     * DockerHost容器负载弹性扩缩，容器内存配置
     */
    @Value("\${dockerhost.elasticity.memReservation:34359738368}")
    var elasticityMemReservation: Long? = 32 * 1024 * 1024 * 1024L

    /**
     * DockerHost容器负载弹性扩缩，容器内存阈值
     */
    @Value("\${dockerhost.elasticity.memThreshold:80}")
    var elasticityMemThreshold: Int? = 80

    /**
     * Codecc集群下是否开启dockerRun日志上报
     */
    @Value("\${codecc.dockerRun.log:false}")
    var dockerRunLog: Boolean? = false

    /**
     * bazel overlayfs lower层路径
     */
    @Value("\${dockerCli.bazelLowerPath:/data/bazelcache}")
    var bazelLowerPath: String? = null

    /**
     * bazel overlayfs upper路径
     */
    @Value("\${dockerCli.bazelUpperPath:/data/landun/thirdparty/bazel_cache}")
    var bazelUpperPath: String? = null

    /**
     * bazel 缓存容器路径
     */
    @Value("\${dockerCli.bazelContainerPath:/root/.bazelcache}")
    var bazelContainerPath: String? = null
}
