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

	"github.com/Tencent/bk-ci/src/agent/src/pkg/util"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/command"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	bconfig "github.com/astaxie/beego/config"
	"github.com/astaxie/beego/logs"
)

const (
	KeyProjectId         = "devops.project.id"
	KeyAgentId           = "devops.agent.id"
	KeySecretKey         = "devops.agent.secret.key"
	KeyDevopsGateway     = "landun.gateway"
	KeyDevopsFileGateway = "landun.fileGateway"
	KeyTaskCount         = "devops.parallel.task.count"
	KeyEnvType           = "landun.env"
	KeySlaveUser         = "devops.slave.user"
	KeyCollectorOn       = "devops.agent.collectorOn"
	KeyRequestTimeoutSec = "devops.agent.request.timeout.sec"
	KeyDetectShell       = "devops.agent.detect.shell"
	KeyIgnoreLocalIps    = "devops.agent.ignoreLocalIps"
	KeyBatchInstall      = "devops.agent.batch.install"
)

// AgentConfig Agent 配置
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
	DetectShell       bool
	IgnoreLocalIps    string
	BatchInstallKey   string
}

// AgentEnv Agent 环境配置
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
var GEnvVars map[string]string
var UseCert bool

// Init 加载和初始化配置
func Init() {
	err := LoadAgentConfig()
	if err != nil {
		logs.Error("load agent config err: ", err)
		systemutil.ExitProcess(1)
	}
	initCert()
	LoadAgentEnv()
}

// LoadAgentEnv 加载Agent环境
func LoadAgentEnv() {
	GAgentEnv = new(AgentEnv)

	/*
	   忽略一些在Windows机器上VPN代理软件所产生的虚拟网卡（有Mac地址）的IP，一般这类IP
	   更像是一些路由器的192开头的IP，属于干扰IP，安装了这类软件的windows机器IP都会变成相同，所以需要忽略掉
	*/
	if len(GAgentConfig.IgnoreLocalIps) > 0 {
		splitIps := util.SplitAndTrimSpace(GAgentConfig.IgnoreLocalIps, ",")
		GAgentEnv.AgentIp = systemutil.GetAgentIp(splitIps)
	} else {
		GAgentEnv.AgentIp = systemutil.GetAgentIp([]string{})
	}

	GAgentEnv.HostName = systemutil.GetHostName()
	GAgentEnv.OsName = systemutil.GetOsName()
	GAgentEnv.SlaveVersion = DetectWorkerVersion()
	GAgentEnv.AgentVersion = DetectAgentVersion()
}

// DetectAgentVersion 检测Agent版本
func DetectAgentVersion() string {
	return DetectAgentVersionByDir(systemutil.GetWorkDir())
}

// DetectAgentVersionByDir 检测指定目录下的Agent文件版本
func DetectAgentVersionByDir(workDir string) string {
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

// DetectWorkerVersion 检查worker版本
func DetectWorkerVersion() string {
	return DetectWorkerVersionByDir(systemutil.GetWorkDir())
}

// DetectWorkerVersionByDir 检测指定目录下的Worker文件版本
func DetectWorkerVersionByDir(workDir string) string {
	jar := fmt.Sprintf("%s/%s", workDir, WorkAgentFile)
	output, err := command.RunCommand(GetJava(),
		[]string{"-Xmx256m", "-cp", jar, "com.tencent.devops.agent.AgentVersionKt"},
		workDir, nil)

	if err != nil {
		logs.Warn("detect worker version failed: ", err.Error())
		logs.Warn("output: ", string(output))
		GAgentEnv.SlaveVersion = ""
		return ""
	}

	return parseWorkerVersion(string(output))
}

// parseWorkerVersion 解析worker版本
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

// BuildAgentJarPath 生成jar寻址路径
func BuildAgentJarPath() string {
	return fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), WorkAgentFile)
}

// LoadAgentConfig 加载 .agent.properties文件信息
func LoadAgentConfig() error {
	GAgentConfig = new(AgentConfig)

	conf, err := bconfig.NewConfig("ini", systemutil.GetWorkDir()+"/.agent.properties")
	if err != nil {
		logs.Error("load agent config failed, ", err)
		return errors.New("load agent config failed")
	}

	parallelTaskCount, err := conf.Int(KeyTaskCount)
	if err != nil || parallelTaskCount < 0 {
		return errors.New("invalid parallelTaskCount")
	}

	projectId := strings.TrimSpace(conf.String(KeyProjectId))
	if len(projectId) == 0 {
		return errors.New("invalid projectId")
	}

	agentId := conf.String(KeyAgentId)
	if len(agentId) == 0 {
		return errors.New("invalid agentId")
	}

	secretKey := strings.TrimSpace(conf.String(KeySecretKey))
	if len(secretKey) == 0 {
		return errors.New("invalid secretKey")
	}

	landunGateway := strings.TrimSpace(conf.String(KeyDevopsGateway))
	if len(landunGateway) == 0 {
		return errors.New("invalid landunGateway")
	}

	landunFileGateway := strings.TrimSpace(conf.String(KeyDevopsFileGateway))
	if len(landunFileGateway) == 0 {
		logs.Warn("fileGateway is empty")
	}

	envType := strings.TrimSpace(conf.String(KeyEnvType))
	if len(envType) == 0 {
		return errors.New("invalid envType")
	}

	slaveUser := strings.TrimSpace(conf.String(KeySlaveUser))
	if len(slaveUser) == 0 {
		slaveUser = systemutil.GetCurrentUser().Username
	}

	collectorOn, err := conf.Bool(KeyCollectorOn)
	if err != nil {
		collectorOn = true
	}
	timeout, err := conf.Int64(KeyRequestTimeoutSec)
	if err != nil {
		timeout = 5
	}
	detectShell := conf.DefaultBool(KeyDetectShell, false)

	ignoreLocalIps := strings.TrimSpace(conf.String(KeyIgnoreLocalIps))
	if len(ignoreLocalIps) == 0 {
		ignoreLocalIps = "127.0.0.1,192.168.10.255" // 临时代码，上线更新即移除
	}

	GAgentConfig.BatchInstallKey = strings.TrimSpace(conf.String(KeyBatchInstall))

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
	GAgentConfig.DetectShell = detectShell
	logs.Info("DetectShell: ", GAgentConfig.DetectShell)
	GAgentConfig.IgnoreLocalIps = ignoreLocalIps
	logs.Info("IgnoreLocalIps: ", GAgentConfig.IgnoreLocalIps)
	logs.Info("BatchInstallKey: ", GAgentConfig.BatchInstallKey)
	// 初始化 GAgentConfig 写入一次配置, 往文件中写入一次程序中新添加的 key
	return GAgentConfig.SaveConfig()
}

// SaveConfig 将配置回写到agent.properties文件保存
func (a *AgentConfig) SaveConfig() error {
	filePath := systemutil.GetWorkDir() + "/.agent.properties"

	content := bytes.Buffer{}
	content.WriteString(KeyProjectId + "=" + GAgentConfig.ProjectId + "\n")
	content.WriteString(KeyAgentId + "=" + GAgentConfig.AgentId + "\n")
	content.WriteString(KeySecretKey + "=" + GAgentConfig.SecretKey + "\n")
	content.WriteString(KeyDevopsGateway + "=" + GAgentConfig.Gateway + "\n")
	content.WriteString(KeyDevopsFileGateway + "=" + GAgentConfig.FileGateway + "\n")
	content.WriteString(KeyTaskCount + "=" + strconv.Itoa(GAgentConfig.ParallelTaskCount) + "\n")
	content.WriteString(KeyEnvType + "=" + GAgentConfig.EnvType + "\n")
	content.WriteString(KeySlaveUser + "=" + GAgentConfig.SlaveUser + "\n")
	content.WriteString(KeyRequestTimeoutSec + "=" + strconv.FormatInt(GAgentConfig.TimeoutSec, 10) + "\n")
	content.WriteString(KeyDetectShell + "=" + strconv.FormatBool(GAgentConfig.DetectShell) + "\n")
	content.WriteString(KeyIgnoreLocalIps + "=" + GAgentConfig.IgnoreLocalIps + "\n")

	err := ioutil.WriteFile(filePath, []byte(content.String()), 0666)
	if err != nil {
		logs.Error("write config failed:", err.Error())
		return errors.New("write config failed")
	}
	return nil
}

// GetAuthHeaderMap 生成鉴权头部
func (a *AgentConfig) GetAuthHeaderMap() map[string]string {
	authHeaderMap := make(map[string]string)
	authHeaderMap[AuthHeaderBuildType] = a.BuildType
	authHeaderMap[AuthHeaderProjectId] = a.ProjectId
	authHeaderMap[AuthHeaderAgentId] = a.AgentId
	authHeaderMap[AuthHeaderSecretKey] = a.SecretKey
	return authHeaderMap
}

// GetJava 获取本地java命令路径
func GetJava() string {
	workDir := systemutil.GetWorkDir()
	if systemutil.IsMacos() {
		return workDir + "/jre/Contents/Home/bin/java"
	} else {
		return workDir + "/jre/bin/java"
	}
}

// initCert 初始化证书
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
