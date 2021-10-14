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
	"crypto/tls"
	"crypto/x509"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/command"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	bconfig "github.com/astaxie/beego/config"
	"github.com/astaxie/beego/logs"
)

const (
	ConfigKeyProjectId         = "devops.project.id"
	ConfigKeyAgentId           = "devops.agent.id"
	ConfigKeySecretKey         = "devops.agent.secret.key"
	ConfigKeyDevopsGateway     = "landun.gateway"
	ConfigKeyDevopsFileGateway = "landun.fileGateway"
	ConfigKeyTaskCount         = "devops.parallel.task.count"
	ConfigKeyEnvType           = "landun.env"
	ConfigKeySlaveUser         = "devops.slave.user"
	ConfigKeyCollectorOn       = "devops.agent.collectorOn"
	ConfigKeyRequestTimeoutSec = "devops.agent.request.timeout.sec"
)

type AgentConfig struct {
	Gateway           string
	FileGateway       string
	BuildType         string
	ProjectId         string
	AgentId           string
	SecretKey         string
	ParallelTaskCount int
	EnvType           string
	SlaveUser         string
	CollectorOn       bool
	TimeoutSec        int64
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
var UseCert bool

func Init() {
	err := LoadAgentConfig()
	if err != nil {
		logs.Error("load agent config err: ", err)
		systemutil.ExitProcess(1)
	}
	initCert()
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
	agentExecutable := workDir + "/" + GetClienAgentFile()

	if systemutil.IsLinux() || systemutil.IsMacos() {
		if !fileutil.Exists(agentExecutable) {
			logs.Warn("agent executable not exists")
			return ""
		}
		err := fileutil.SetExecutable(agentExecutable)
		if err != nil {
			logs.Warn(fmt.Errorf("chmod agent file failed: %v", err))
			return ""
		}
	}

	output, err := command.RunCommand(agentExecutable, []string{"version"}, workDir, nil)
	if err != nil {
		logs.Warn("detect agent version failed: ", err.Error())
		logs.Warn("output: ", string(output))
		GAgentEnv.AgentVersion = ""
		return ""
	}
	agentVersion := strings.TrimSpace(string(output))
	logs.Info("agent version: ", agentVersion)

	return strings.TrimSpace(agentVersion)
}

func DetectWorkerVersion() string {
	output, err := command.RunCommand(GetJava(),
		[]string{"-cp", BuildAgentJarPath(), "com.tencent.devops.agent.AgentVersionKt"}, systemutil.GetWorkDir(), nil)

	if err != nil {
		logs.Warn("detect worker version failed: ", err.Error())
		logs.Warn("output: ", string(output))
		GAgentEnv.SlaveVersion = ""
		return ""
	}

	return parseWorkerVersion(string(output))
}

func parseWorkerVersion(output string) string {
	lines := strings.Split(output, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if !(line == "") && !strings.Contains(line, " ") && !strings.Contains(line, "OPTIONS") {
			if len(line) > 64 {
				line = line[:64]
			}
			logs.Info("worker version: ", line)
			return line
		}
	}
	return ""
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

	landunFileGateway := strings.TrimSpace(conf.String(ConfigKeyDevopsFileGateway))
	if len(landunFileGateway) == 0 {
		logs.Warn("fileGateway is empty")
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
	timeout, err := conf.Int64(ConfigKeyRequestTimeoutSec)
	if err != nil {
		timeout = 5
	}

	GAgentConfig.Gateway = landunGateway
	systemutil.DevopsGateway = landunGateway
	logs.Info("Gateway: ", GAgentConfig.Gateway)
	GAgentConfig.FileGateway = landunFileGateway
	logs.Info("FileGateway: ", GAgentConfig.FileGateway)
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
	GAgentConfig.TimeoutSec = timeout
	logs.Info("TimeoutSec: ", GAgentConfig.TimeoutSec)
	// 初始化 GAgentConfig 写入一次配置, 往文件中写入一次程序中新添加的 key
	return GAgentConfig.SaveConfig()
}

func (a *AgentConfig) SaveConfig() error {
	filePath := systemutil.GetWorkDir() + "/.agent.properties"

	systemutil.IsWindows()
	content := bytes.Buffer{}
	content.WriteString(ConfigKeyProjectId + "=" + GAgentConfig.ProjectId + "\n")
	content.WriteString(ConfigKeyAgentId + "=" + GAgentConfig.AgentId + "\n")
	content.WriteString(ConfigKeySecretKey + "=" + GAgentConfig.SecretKey + "\n")
	content.WriteString(ConfigKeyDevopsGateway + "=" + GAgentConfig.Gateway + "\n")
	content.WriteString(ConfigKeyDevopsFileGateway + "=" + GAgentConfig.FileGateway + "\n")
	content.WriteString(ConfigKeyTaskCount + "=" + strconv.Itoa(GAgentConfig.ParallelTaskCount) + "\n")
	content.WriteString(ConfigKeyEnvType + "=" + GAgentConfig.EnvType + "\n")
	content.WriteString(ConfigKeySlaveUser + "=" + GAgentConfig.SlaveUser + "\n")
	content.WriteString(ConfigKeyRequestTimeoutSec + "=" + strconv.FormatInt(GAgentConfig.TimeoutSec, 10) + "\n")

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

func initCert() {
	AbsCertFilePath := systemutil.GetWorkDir() + "/" + CertFilePath
	fileInfo, err := os.Stat(AbsCertFilePath)
	if err != nil {
		// 证书不一定需要存在
		logs.Warn("stat cert file error", err.Error())
		return
	}
	if fileInfo.IsDir() {
		// 证书不一定需要存在
		logs.Warn("cert file is dir, skip")
		return
	}
	// Load client cert
	caCert, err := ioutil.ReadFile(AbsCertFilePath)
	if err != nil {
		logs.Warn("Reading server certificate: %s", err)
		return
	}
	logs.Informational("Cert content is: %s", string(caCert))
	caCertPool, err := x509.SystemCertPool()
	// Windows 下 SystemCertPool 返回 nil
	if err != nil || caCertPool == nil {
		logs.Warn("get system cert pool fail: %s or system cert pool is nil, use new cert pool", err)
		caCertPool = x509.NewCertPool()
	}
	caCertPool.AppendCertsFromPEM(caCert)
	tlsConfig := &tls.Config{RootCAs: caCertPool}
	http.DefaultTransport.(*http.Transport).TLSClientConfig = tlsConfig
	logs.Informational("load cert success")
	UseCert = true
}
