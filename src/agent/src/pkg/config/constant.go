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

package config

import (
	"pkg/util/systemutil"
)

const ActionUpgrade = "upgrade"
const ActionUninstall = "uninstall"

const (
	ScriptFileInstallWindows   = "install.bat"
	ScriptFileInstallLinux     = "install.sh"
	ScriptFileUninstallWindows = "uninstall.bat"
	ScriptFileUnistallLinux    = "uninstall.sh"
	ScriptFileStartWindows     = "start.bat"
	ScriptFileStartLinux       = "start.sh"
	ScriptFileStopWindows      = "stop.bat"
	ScriptFileStopLinux        = "stop.sh"
	
	AgentFileClientWindows = "devopsAgent.exe"
	AgentFileClientLinux   = "devopsAgent"
	AgentFileServerWindows = "devopsAgent.exe"
	AgentFileServerLinux   = "devopsAgent_linux"
	AgentFileServerMacos   = "devopsAgent_macos"

	UpgraderFileClientWindows = "upgrader.exe"
	UpgraderFileClientLinux   = "upgrader"
	UpgraderFileServerWindows = "upgrader.exe"
	UpgraderFileServerLinux   = "upgrader_linux"
	UpgraderFileServerMacOs   = "upgrader_macos"

	WorkAgentFile = "worker-agent.jar"
)

// Auth Header
const AuthHeaderSodaProjectId = "X-SODA-PID" //项目ID

const AuthHeaderBuildType = "X-DEVOPS-BUILD-TYPE"       // 构建类型
const AuthHeaderProjectId = "X-DEVOPS-PROJECT-ID"       // 项目ID
const AuthHeaderAgentId = "X-DEVOPS-AGENT-ID"           // Agent ID
const AuthHeaderSecretKey = "X-DEVOPS-AGENT-SECRET-KEY" // Agent密钥
const AuthHeaderPipelineId = "X-DEVOPS-PIPELINE-ID"     //流水线ID
const AuthHeaderBuildId = "X-DEVOPS-BUILD-ID"           //构建ID
const AuthHeaderVmSeqId = "X-DEVOPS-VM-SID"             //VM Seq Id
const AuthHeaderUserId = "X-DEVOPS-UID"                 //用户ID

// 环境变量
const EnvWorkspace = "WORKSPACE"

// 环境类型
const EnvTypeProd = "PROD"
const EnvTypeTest = "TEST"
const EnvTypeDev = "DEV"

// BuildType
const BuildTypeWorkder = "WORKER"
const BuildTypeAgent = "AGENT"
const BuildTypePluginAgent = "PLUGIN_AGENT"
const BuildTypeDocker = "DOCKER"
const BuildTypeDockerHost = "DOCKER_HOST"
const BuildTypeTstack = "TSTACK_AGENT"

// AgentStatus
const AgentStatusUnimport = "UN_IMPORT"
const AgentStatusUnimportOk = "UN_IMPORT_OK"
const AgentStatusImportOk = "IMPORT_OK"
const AgentStatusImportException = "IMPORT_EXCEPTION"
const AgentStatusDelete = "DELETE"

func GetServerAgentFile() string {
	if systemutil.IsWindows() {
		return AgentFileServerWindows
	} else if systemutil.IsMacos() {
		return AgentFileServerMacos
	} else {
		return AgentFileServerLinux
	}
}

func GetServerUpgraderFile() string {
	if systemutil.IsWindows() {
		return UpgraderFileServerWindows
	} else if systemutil.IsMacos() {
		return UpgraderFileServerMacOs
	} else {
		return UpgraderFileServerLinux
	}
}

func GetClienAgentFile() string {
	if systemutil.IsWindows() {
		return AgentFileClientWindows
	} else {
		return AgentFileClientLinux
	}
}

func GetClientUpgraderFile() string {
	if systemutil.IsWindows() {
		return UpgraderFileClientWindows
	} else {
		return UpgraderFileClientLinux
	}
}

func GetInstallScript() string {
	if systemutil.IsWindows() {
		return ScriptFileInstallWindows
	} else {
		return ScriptFileInstallLinux
	}
}

func GetUninstallScript() string {
	if systemutil.IsWindows() {
		return ScriptFileUninstallWindows
	} else {
		return ScriptFileUnistallLinux
	}
}

func GetStartScript() string {
	if systemutil.IsWindows() {
		return ScriptFileStartWindows
	} else {
		return ScriptFileStartLinux
	}
}

func GetStopScript() string {
	if systemutil.IsWindows() {
		return ScriptFileStopWindows
	} else {
		return ScriptFileStopLinux
	}
}
