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
	"bytes"
	"errors"
	"fmt"
	bconfig "github.com/astaxie/beego/config"
	"github.com/astaxie/beego/logs"
	"io/ioutil"
	"pkg/util/command"
	"pkg/util/fileutil"
	"pkg/util/systemutil"
	"strconv"
	"strings"
)

const (
	ConfigKeyProjectId     = "devops.project.id"
	ConfigKeyAgentId       = "devops.agent.id"
	ConfigKeySecretKey     = "devops.agent.secret.key"
	ConfigKeyDevopsGateway = "landun.gateway"
	ConfigKeyTaskCount     = "devops.parallel.task.count"
	ConfigKeyEnvType       = "landun.env"
	ConfigKeySlaveUser     = "devops.slave.user"
	ConfigKeyCollectorOn   = "devops.agent.collectorOn"
)

type AgentConfig struct {
	Gateway           string
	BuildType         string
	ProjectId         string
	AgentId           string
	SecretKey         string
	ParallelTaskCount int
	EnvType           string
	SlaveUser         string
	CollectorOn       bool
}

type AgentEnv struct {
	OsName           string
	AgentIp          string
	HostName         string
	SlaveVersion     string
	AgentVersion     string
	AgentInstallPath string
}

var GAgentEnv *AgentEnv
var GAgentConfig *AgentConfig
var GIsAgentUpgrading = false
var GWorkDir string
var GEnvVars map[string]string

func Init() {
	err := LoadAgentConfig()
	if err != nil {
		logs.Error("load agent config err: ", err)
		systemutil.ExitProcess(1)
	}
	LoadAgentEnv()
}

func LoadAgentEnv() {
	GAgentEnv = new(AgentEnv)
	GAgentEnv.AgentIp = systemutil.GetAgentIp()
	GAgentEnv.HostName = systemutil.GetHostName()
	GAgentEnv.OsName = systemutil.GetOsName()
	GAgentEnv.SlaveVersion = DetectWorkerVersion()
	GAgentEnv.AgentVersion = DetectAgentVersion()
}

func DetectAgentVersion() string {
	workDir := systemutil.GetWorkDir()
	output, err := command.RunCommand(workDir+"/"+GetClienAgentFile(), []string{"version"}, workDir, nil)
	if err != nil {
		logs.Warn("detect agent version failed: ", err.Error())
		GAgentEnv.AgentVersion = ""
		return ""
	}
	logs.Info("agent version: ", string(output))

	return strings.TrimSpace(string(output))
}

func DetectWorkerVersion() string {
	output, err := command.RunCommand(GetJava(),
		[]string{"-cp", BuildAgentJarPath(), "com.tencent.devops.agent.AgentVersionKt"}, systemutil.GetWorkDir(), nil)

	if err != nil {
		logs.Warn("detect worker version failed: ", err.Error())
		GAgentEnv.SlaveVersion = ""
		return ""
	}
	logs.Info("worker version: ", string(output))

	return strings.TrimSpace(string(output))
}

func BuildAgentJarPath() string {
	path := fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), "worker-agent.jar")
	if !fileutil.Exists(path) {
		logs.Warn("worker-agent.jar not exist, use agent.jar")
		path = fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), "agent.jar")
	}
	return path
}

func LoadAgentConfig() error {
	GAgentConfig = new(AgentConfig)

	conf, err := bconfig.NewConfig("ini", systemutil.GetWorkDir()+"/.agent.properties")
	if err != nil {
		logs.Error("load agent config failed, ", err)
		return errors.New("load agent config failed")
	}

	parallelTaskCount, err := conf.Int(ConfigKeyTaskCount)
	if err != nil || parallelTaskCount < 0 {
		return errors.New("invalid parallelTaskCount")
	}

	projectId := strings.TrimSpace(conf.String(ConfigKeyProjectId))
	if len(projectId) == 0 {
		return errors.New("invalid projectId")
	}

	agentId := conf.String(ConfigKeyAgentId)
	if len(agentId) == 0 {
		return errors.New("invalid agentId")
	}

	secretKey := strings.TrimSpace(conf.String(ConfigKeySecretKey))
	if len(secretKey) == 0 {
		return errors.New("invalid secretKey")
	}

	landunGateway := strings.TrimSpace(conf.String(ConfigKeyDevopsGateway))
	if len(landunGateway) == 0 {
		return errors.New("invalid landunGateway")
	}

	envType := strings.TrimSpace(conf.String(ConfigKeyEnvType))
	if len(envType) == 0 {
		return errors.New("invalid envType")
	}

	slaveUser := strings.TrimSpace(conf.String(ConfigKeySlaveUser))
	if len(slaveUser) == 0 {
		slaveUser = systemutil.GetCurrentUser().Username
	}

	collectorOn, err := conf.Bool(ConfigKeyCollectorOn)
	if err != nil {
		collectorOn = true
	}

	GAgentConfig.Gateway = landunGateway
	logs.Info("Gateway: ", GAgentConfig.Gateway)
	GAgentConfig.BuildType = BuildTypeAgent
	logs.Info("BuildType: ", GAgentConfig.BuildType)
	GAgentConfig.ProjectId = projectId
	logs.Info("ProjectId: ", GAgentConfig.ProjectId)
	GAgentConfig.AgentId = agentId
	logs.Info("AgentId: ", GAgentConfig.AgentId)
	GAgentConfig.SecretKey = secretKey
	logs.Info("SecretKey: ", GAgentConfig.SecretKey)
	GAgentConfig.EnvType = envType
	logs.Info("EnvType: ", GAgentConfig.EnvType)
	GAgentConfig.ParallelTaskCount = parallelTaskCount
	logs.Info("ParallelTaskCount: ", GAgentConfig.ParallelTaskCount)
	GAgentConfig.SlaveUser = slaveUser
	logs.Info("SlaveUser: ", GAgentConfig.SlaveUser)
	GAgentConfig.CollectorOn = collectorOn
	logs.Info("CollectorOn: ", GAgentConfig.CollectorOn)
	return nil
}

func (a *AgentConfig) SaveConfig() error {
	filePath := systemutil.GetWorkDir() + "/.agent.properties"

	systemutil.IsWindows()
	content := bytes.Buffer{}
	content.WriteString(ConfigKeyProjectId + "=" + GAgentConfig.ProjectId + "\n")
	content.WriteString(ConfigKeyAgentId + "=" + GAgentConfig.AgentId + "\n")
	content.WriteString(ConfigKeySecretKey + "=" + GAgentConfig.SecretKey + "\n")
	content.WriteString(ConfigKeyDevopsGateway + "=" + GAgentConfig.Gateway + "\n")
	content.WriteString(ConfigKeyTaskCount + "=" + strconv.Itoa(GAgentConfig.ParallelTaskCount) + "\n")
	content.WriteString(ConfigKeyEnvType + "=" + GAgentConfig.EnvType + "\n")
	content.WriteString(ConfigKeySlaveUser + "=" + GAgentConfig.SlaveUser + "\n")

	err := ioutil.WriteFile(filePath, []byte(content.String()), 0666)
	if err != nil {
		logs.Error("write config failed:", err.Error())
		return errors.New("write config failed")
	}
	return nil
}

func (a *AgentConfig) GetAuthHeaderMap() map[string]string {
	authHeaderMap := make(map[string]string)
	authHeaderMap[AuthHeaderBuildType] = a.BuildType
	authHeaderMap[AuthHeaderSodaProjectId] = a.ProjectId
	authHeaderMap[AuthHeaderProjectId] = a.ProjectId
	authHeaderMap[AuthHeaderAgentId] = a.AgentId
	authHeaderMap[AuthHeaderSecretKey] = a.SecretKey
	return authHeaderMap
}

func GetJava() string {
	workDir := systemutil.GetWorkDir()
	if systemutil.IsMacos() {
		return workDir + "/jre/Contents/Home/bin/java"
	} else {
		return workDir + "/jre/bin/java"
	}
}
