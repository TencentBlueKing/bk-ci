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

package config

import (
	"bytes"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"

	"github.com/pkg/errors"

	languageUtil "golang.org/x/text/language"
	"gopkg.in/ini.v1"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
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
	KeyLogsKeepHours     = "devops.agent.logs.keep.hours"
	// KeyJdkDirPath 这个key不会预先出现在配置文件中，因为workdir未知，需要第一次动态获取
	KeyJdkDirPath = "devops.agent.jdk.dir.path"
	// KeyJdk17DirPath 最新的 jdk 路径，因为需要一段时间的兼容所以和 KeyJdkDirPath 共存
	KeyJdk17DirPath        = "devops.agent.jdk17.dir.path"
	KeyDockerTaskCount     = "devops.docker.parallel.task.count"
	keyEnableDockerBuild   = "devops.docker.enable"
	KeyLanguage            = "devops.language"
	KeyImageDebugPortRange = "devops.imagedebug.portrange"
	KeyEnablePipeline      = "devops.pipeline.enable"
)

// AgentConfig Agent 配置
type AgentConfig struct {
	Gateway                 string
	FileGateway             string
	BuildType               string
	ProjectId               string
	AgentId                 string
	SecretKey               string
	ParallelTaskCount       int
	EnvType                 string
	SlaveUser               string
	CollectorOn             bool
	TimeoutSec              int64
	DetectShell             bool
	IgnoreLocalIps          string
	BatchInstallKey         string
	LogsKeepHours           int
	JdkDirPath              string
	Jdk17DirPath            string
	DockerParallelTaskCount int
	EnableDockerBuild       bool
	Language                string
	ImageDebugPortRange     string
	EnablePipeline          bool
}

// AgentEnv Agent 环境配置
type AgentEnv struct {
	OsName           string
	agentIp          string
	HostName         string
	AgentVersion     string
	AgentInstallPath string
	// WinTask 启动windows进程的组件如 服务/执行计划
	WinTask string
	// OsVersion 系统版本信息
	OsVersion string
}

func (e *AgentEnv) GetAgentIp() string {
	return e.agentIp
}

func (e *AgentEnv) SetAgentIp(ip string) {
	if e.agentIp == ip {
		return
	}
	// IP变更时发送事件
	if e.agentIp != "" && ip != "127.0.0.1" {
		EBus.Publish(IpEvent, ip)
	}
	e.agentIp = ip
}

var GAgentEnv *AgentEnv
var GAgentConfig *AgentConfig
var UseCert bool
var IsDebug = false

// Init 加载和初始化配置
func Init(isDebug bool) {
	IsDebug = isDebug
	err := LoadAgentConfig()
	if err != nil {
		logs.WithError(err).Error("load agent config err")
		systemutil.ExitProcess(1)
	}
	initCert()
	LoadAgentEnv()

	GApiEnvVars = &GEnvVarsT{
		envs: make(map[string]string),
		lock: sync.RWMutex{},
	}
}

// LoadAgentEnv 加载Agent环境
func LoadAgentEnv() {
	GAgentEnv = new(AgentEnv)
	LoadAgentIp()
	GAgentEnv.HostName = systemutil.GetHostName()
	GAgentEnv.OsName = systemutil.GetOsName()
	GAgentEnv.AgentVersion = DetectAgentVersion()
	GAgentEnv.WinTask = GetWinTaskType()
	if osVersion, err := GetOsVersion(); err != nil {
		logs.WithError(err).Warn("get os version err")
		GAgentEnv.OsVersion = ""
	} else {
		GAgentEnv.OsVersion = osVersion
	}
}

// LoadAgentIp 忽略一些在Windows机器上VPN代理软件所产生的虚拟网卡（有Mac地址）的IP，一般这类IP
// 更像是一些路由器的192开头的IP，属于干扰IP，安装了这类软件的windows机器IP都会变成相同，所以需要忽略掉
func LoadAgentIp() {
	var splitIps []string
	if len(GAgentConfig.IgnoreLocalIps) > 0 {
		splitIps = util.SplitAndTrimSpace(GAgentConfig.IgnoreLocalIps, ",")
	}
	GAgentEnv.SetAgentIp(systemutil.GetAgentIp(splitIps))
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
			logs.WithError(err).Warn("chmod agent file failed")
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

// BuildAgentJarPath 生成jar寻址路径
func BuildAgentJarPath() string {
	return fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), WorkAgentFile)
}

// LoadAgentConfig 加载 .agent.properties文件信息
func LoadAgentConfig() error {
	GAgentConfig = new(AgentConfig)

	conf, err := ini.Load(filepath.Join(systemutil.GetWorkDir(), ".agent.properties"))
	if err != nil {
		logs.Error("load agent config failed, ", err)
		return errors.New("load agent config failed")
	}

	parallelTaskCount, err := conf.Section("").Key(KeyTaskCount).Int()
	if err != nil || parallelTaskCount < 0 {
		return errors.New("invalid parallelTaskCount")
	}

	projectId := strings.TrimSpace(conf.Section("").Key(KeyProjectId).String())
	if len(projectId) == 0 {
		return errors.New("invalid projectId")
	}

	agentId := conf.Section("").Key(KeyAgentId).String()
	if len(agentId) == 0 {
		return errors.New("invalid agentId")
	}

	secretKey := strings.TrimSpace(conf.Section("").Key(KeySecretKey).String())
	if len(secretKey) == 0 {
		return errors.New("invalid secretKey")
	}

	landunGateway := strings.TrimSpace(conf.Section("").Key(KeyDevopsGateway).String())
	if len(landunGateway) == 0 {
		return errors.New("invalid landunGateway")
	}

	landunFileGateway := strings.TrimSpace(conf.Section("").Key(KeyDevopsFileGateway).String())
	if len(landunFileGateway) == 0 {
		logs.Warn("fileGateway is empty")
	}

	envType := strings.TrimSpace(conf.Section("").Key(KeyEnvType).String())
	if len(envType) == 0 {
		return errors.New("invalid envType")
	}

	slaveUser := strings.TrimSpace(conf.Section("").Key(KeySlaveUser).String())
	if len(slaveUser) == 0 {
		slaveUser = systemutil.GetCurrentUser().Username
	}

	collectorOn, err := conf.Section("").Key(KeyCollectorOn).Bool()
	if err != nil {
		collectorOn = true
	}
	timeout, err := conf.Section("").Key(KeyRequestTimeoutSec).Int64()
	if err != nil {
		timeout = 5
	}
	detectShell := conf.Section("").Key(KeyDetectShell).MustBool(false)

	ignoreLocalIps := strings.TrimSpace(conf.Section("").Key(KeyIgnoreLocalIps).String())
	if len(ignoreLocalIps) == 0 {
		ignoreLocalIps = "127.0.0.1"
	}

	logsKeepHours, err := conf.Section("").Key(KeyLogsKeepHours).Int()
	if err != nil {
		logsKeepHours = 96
	}

	jdkDirPath := conf.Section("").Key(KeyJdkDirPath).String()
	// 如果路径为空，是第一次，需要主动去拿一次
	if jdkDirPath == "" {
		workDir := systemutil.GetWorkDir()
		if _, err := os.Stat(workDir + "/jdk"); err != nil && !os.IsExist(err) {
			jdkDirPath = workDir + "/jre"
		}
		jdkDirPath = workDir + "/jdk"
	}
	jdk17DirPath := conf.Section("").Key(KeyJdk17DirPath).String()
	if jdk17DirPath == "" {
		jdk17DirPath = systemutil.GetWorkDir() + "/jdk17"
	}

	// 兼容旧版本 .agent.properties 没有这个键
	dockerParallelTaskCount := 4
	if conf.Section("").HasKey(KeyDockerTaskCount) {
		dockerParallelTaskCount, err = conf.Section("").Key(KeyDockerTaskCount).Int()
		if err != nil || dockerParallelTaskCount < 0 {
			return errors.New("invalid dockerParallelTaskCount")
		}
	}

	language := DEFAULT_LANGUAGE_TYPE
	if conf.Section("").HasKey(KeyLanguage) {
		language = conf.Section("").Key(KeyLanguage).String()
		_, err := languageUtil.Parse(language)
		if err != nil {
			logs.Errorf("not support language %s", language)
			language = DEFAULT_LANGUAGE_TYPE
		}
	}

	enableDocker := conf.Section("").Key(keyEnableDockerBuild).MustBool(false)

	imageDebugPortRange := conf.Section("").Key(KeyImageDebugPortRange).MustString(DEFAULT_IMAGE_DEBUG_PORT_RANGE)

	enablePipeline := conf.Section("").Key(KeyEnablePipeline).MustBool(false)

	// -----------

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

	GAgentConfig.BatchInstallKey = strings.TrimSpace(conf.Section("").Key(KeyBatchInstall).String())
	logs.Info("BatchInstallKey: ", GAgentConfig.BatchInstallKey)

	GAgentConfig.LogsKeepHours = logsKeepHours
	logs.Info("logsKeepHours: ", GAgentConfig.LogsKeepHours)

	GAgentConfig.JdkDirPath = jdkDirPath
	logs.Info("jdkDirPath: ", GAgentConfig.JdkDirPath)

	GAgentConfig.Jdk17DirPath = jdk17DirPath
	logs.Info("jdk17DirPath: ", GAgentConfig.Jdk17DirPath)

	GAgentConfig.DockerParallelTaskCount = dockerParallelTaskCount
	logs.Info("DockerParallelTaskCount: ", GAgentConfig.DockerParallelTaskCount)

	GAgentConfig.EnableDockerBuild = enableDocker
	logs.Info("EnableDockerBuild: ", GAgentConfig.EnableDockerBuild)

	GAgentConfig.Language = language
	logs.Info("Language:", GAgentConfig.Language)

	GAgentConfig.ImageDebugPortRange = imageDebugPortRange
	logs.Info("ImageDebugPortRange: ", GAgentConfig.ImageDebugPortRange)

	GAgentConfig.EnablePipeline = enablePipeline
	logs.Info("EnablePipeline: ", GAgentConfig.EnablePipeline)
	// 初始化 GAgentConfig 写入一次配置, 往文件中写入一次程序中新添加的 key
	return GAgentConfig.SaveConfig()
}

// 可能存在不同协诚写入文件的操作，加上锁保险些
var saveConfigLock = sync.Mutex{}

// SaveConfig 将配置回写到agent.properties文件保存
func (a *AgentConfig) SaveConfig() error {
	saveConfigLock.Lock()
	defer func() {
		saveConfigLock.Unlock()
	}()

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
	content.WriteString(KeyLogsKeepHours + "=" + strconv.Itoa(GAgentConfig.LogsKeepHours) + "\n")
	content.WriteString(KeyJdkDirPath + "=" + GAgentConfig.JdkDirPath + "\n")
	content.WriteString(KeyJdk17DirPath + "=" + GAgentConfig.Jdk17DirPath + "\n")
	content.WriteString(KeyDockerTaskCount + "=" + strconv.Itoa(GAgentConfig.DockerParallelTaskCount) + "\n")
	content.WriteString(keyEnableDockerBuild + "=" + strconv.FormatBool(GAgentConfig.EnableDockerBuild) + "\n")
	content.WriteString(KeyLanguage + "=" + GAgentConfig.Language + "\n")
	content.WriteString(KeyImageDebugPortRange + "=" + GAgentConfig.ImageDebugPortRange + "\n")
	content.WriteString(KeyEnablePipeline + "=" + strconv.FormatBool(GAgentConfig.EnablePipeline) + "\n")

	err := exitcode.WriteFileWithCheck(filePath, []byte(content.String()), 0666)
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

func GetDockerInitFilePath() string {
	return systemutil.GetWorkDir() + "/" + DockerInitFile
}

func GetGateWay() string {
	if strings.HasPrefix(GAgentConfig.Gateway, "http") || strings.HasPrefix(GAgentConfig.Gateway, "https") {
		return GAgentConfig.Gateway
	} else {
		return "http://" + GAgentConfig.Gateway
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
	caCert, err := os.ReadFile(AbsCertFilePath)
	if err != nil {
		logs.Warnf("Reading server certificate: %s", err)
		return
	}
	logs.Infof("Cert content is: %s", string(caCert))
	caCertPool, err := x509.SystemCertPool()
	// Windows 下 SystemCertPool 返回 nil
	if err != nil || caCertPool == nil {
		logs.Warnf("get system cert pool fail: %s or system cert pool is nil, use new cert pool", err.Error())
		caCertPool = x509.NewCertPool()
	}
	caCertPool.AppendCertsFromPEM(caCert)
	tlsConfig := &tls.Config{RootCAs: caCertPool}
	http.DefaultTransport.(*http.Transport).TLSClientConfig = tlsConfig
	logs.Info("load cert success")
	UseCert = true
}
