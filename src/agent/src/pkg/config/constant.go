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

package config

import (
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

const ActionUpgrade = "upgrade"
const ActionInstall = "install"
const ActionUninstall = "uninstall"
const CertFilePath = ".cert"

const (
	ScriptFileInstallWindows   = "install.bat"
	ScriptFileInstallLinux     = "install.sh"
	ScriptFileUninstallWindows = "uninstall.bat"
	ScriptFileUninstallLinux   = "uninstall.sh"
	ScriptFileStopWindows      = "stop.bat"
	ScriptFileStopLinux        = "stop.sh"
)

const (
	AgentFileClientWindows = "devopsAgent.exe"
	AgentFileClientLinux   = "devopsAgent"
	AgentFileServerWindows = "devopsAgent.exe"
	AgentFileServerLinux   = "devopsAgent_linux"
	AgentFileServerMacos   = "devopsAgent_macos"

	DaemonFileServerWindows = "devopsDaemon.exe"
	DaemonFileServerLinux   = "devopsDaemon_linux"
	DaemonFileServerMacos   = "devopsDaemon_macos"

	DaemonFileClientWindows = "devopsDaemon.exe"
	DaemonFileClientLinux   = "devopsDaemon"

	UpgraderFileClientWindows = "upgrader.exe"
	UpgraderFileClientLinux   = "upgrader"
	UpgraderFileServerWindows = "upgrader.exe"
	UpgraderFileServerLinux   = "upgrader_linux"
	UpgraderFileServerMacOs   = "upgrader_macos"

	WorkAgentFile = "worker-agent.jar"

	JdkClientFile = "jdk.zip"

	DockerInitFile = "agent_docker_init.sh"
)

const DEFAULT_LANGUAGE_TYPE = "zh_CN"

// DEFAULT_IMAGE_DEBUG_PORT_RANGE 默认的可以进行远程登录调试的范围，取自kubernetes nodeport
const DEFAULT_IMAGE_DEBUG_PORT_RANGE = "30000-32767"

// Auth Header

const AuthHeaderBuildType = "X-DEVOPS-BUILD-TYPE"       // 构建类型
const AuthHeaderProjectId = "X-DEVOPS-PROJECT-ID"       // 项目ID
const AuthHeaderAgentId = "X-DEVOPS-AGENT-ID"           // Agent ID
const AuthHeaderSecretKey = "X-DEVOPS-AGENT-SECRET-KEY" // Agent密钥

const BuildTypeAgent = "AGENT"

const AgentStatusImportOk = "IMPORT_OK" // 用户已经在界面导入并且agent工作正常（构建机只有在这个状态才能正常工作）
const AgentStatusDelete = "DELETE"      // Agent被卸载

// GetServerAgentFile 根据平台生成对应要下载的devopsAgent文件名
func GetServerAgentFile() string {
	if systemutil.IsWindows() {
		return AgentFileServerWindows
	} else if systemutil.IsMacos() {
		return AgentFileServerMacos
	} else {
		return AgentFileServerLinux
	}
}

// GetServerUpgraderFile 根据平台生成对应upgrader文件名
func GetServerUpgraderFile() string {
	if systemutil.IsWindows() {
		return UpgraderFileServerWindows
	} else if systemutil.IsMacos() {
		return UpgraderFileServerMacOs
	} else {
		return UpgraderFileServerLinux
	}
}

// GetClienAgentFile 根据平台生成对应要保存的devopsAgent文件名
func GetClienAgentFile() string {
	if systemutil.IsWindows() {
		return AgentFileClientWindows
	} else {
		return AgentFileClientLinux
	}
}

// GetClientUpgraderFile 根据平台生成对应要保存的upgrader文件名
func GetClientUpgraderFile() string {
	if systemutil.IsWindows() {
		return UpgraderFileClientWindows
	} else {
		return UpgraderFileClientLinux
	}
}

// GetServerDaemonFile 根据平台生成对应要下载的devopsDaemon文件名
func GetServerDaemonFile() string {
	if systemutil.IsWindows() {
		return DaemonFileServerWindows
	} else if systemutil.IsMacos() {
		return DaemonFileServerMacos
	} else {
		return DaemonFileServerLinux
	}
}

// GetClientDaemonFile 根据平台生成对应要保存的devopsDaemon文件名
func GetClientDaemonFile() string {
	if systemutil.IsWindows() {
		return DaemonFileClientWindows
	} else {
		return DaemonFileClientLinux
	}
}

// GetInstallScript 根据平台生成对应install脚本
func GetInstallScript() string {
	if systemutil.IsWindows() {
		return ScriptFileInstallWindows
	} else {
		return ScriptFileInstallLinux
	}
}

// GetUninstallScript 根据平台生成对应uninstall脚本
func GetUninstallScript() string {
	if systemutil.IsWindows() {
		return ScriptFileUninstallWindows
	} else {
		return ScriptFileUninstallLinux
	}
}

// GetStopScript 根据平台生成对应stop脚本
func GetStopScript() string {
	if systemutil.IsWindows() {
		return ScriptFileStopWindows
	} else {
		return ScriptFileStopLinux
	}
}
