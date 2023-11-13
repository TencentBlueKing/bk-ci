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
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"sync"

	languageUtil "golang.org/x/text/language"
	"gopkg.in/ini.v1"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
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
	KeyJdkDirPath          = "devops.agent.jdk.dir.path"
	KeyDockerTaskCount     = "devops.docker.parallel.task.count"
	keyEnableDockerBuild   = "devops.docker.enable"
	KeyLanguage            = "devops.language"
	KeyImageDebugPortRange = "devops.imagedebug.portrange"
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
	DockerParallelTaskCount int
	EnableDockerBuild       bool
	Language                string
	ImageDebugPortRange     string
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
var GEnvVars map[string]string
var UseCert bool

var IsDebug bool = false

// Init 加载和初始化配置
func Init(isDebug bool) {
	IsDebug = isDebug
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
	tmpDir, _ := systemutil.MkBuildTmpDir()
	output, err := command.RunCommand(GetJava(),
		[]string{"-Djava.io.tmpdir=" + tmpDir, "-Xmx256m", "-cp", jar, "com.tencent.devops.agent.AgentVersionKt"},
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
	// 用函数匹配正确的版本信息, 主要解决tmp空间不足的情况下，jvm会打印出提示信息，导致识别不到worker版本号
	// 兼容旧版本，防止新agent发布后无限升级
	versionRegexp := regexp.MustCompile(`^v(\d+\.)(\d+\.)(\d+)((-RELEASE)|(-SNAPSHOT)?)$`)
	lines := strings.Split(output, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if !(line == "") && !strings.Contains(line, " ") && !strings.Contains(line, "OPTIONS") {
			if len(line) > 64 {
				line = line[:64]
			}
			// 先使用新版本的匹配逻辑匹配，匹配不通则使用旧版本
			if matchWorkerVersion(line) {
				logs.Info("match worker version: ", line)
				return line
			} else {
				if versionRegexp != nil {
					if versionRegexp.MatchString(line) {
						logs.Info("regexp worker version: ", line)
						return line
					} else {
						continue
					}
				} else {
					// 当正则式出错时(versionRegexp = nil)，继续使用原逻辑
					logs.Info("regexp nil worker version: ", line)
					return line
				}
			}
		}
	}
	return ""
}

// matchWorkerVersion 匹配worker版本信息
// 版本号为 v数字.数字.数字 || v数字.数字.数字-字符.数字
// 只匹配以v开头的数字版本即可
func matchWorkerVersion(line string) bool {
	if !strings.HasPrefix(line, "v") {
		logs.Warnf("line %s matchWorkerVersion no start 'v'", line)
		return false
	}

	// 去掉v方便后面计算
	subline := strings.Split(strings.TrimPrefix(line, "v"), ".")
	sublen := len(subline)
	if sublen < 3 || sublen > 4 {
		logs.Warnf("line %s matchWorkerVersion len no match", line)
		return false
	}

	// v数字.数字.数字 这种去掉v后应该全是数字
	if sublen == 3 {
		return checkNumb(subline, line)
	}

	// v数字.数字.数字-字符.数字，按照 - 分隔，前面的与len 3一致，后面的两个分别判断，不是数字的是字符，不是字符的是数字
	fSubline := strings.Split(strings.TrimPrefix(line, "v"), "-")
	if len(fSubline) != 2 {
		logs.Warnf("line %s matchWorkerVersion len no match", line)
		return false
	}

	if !checkNumb(strings.Split(fSubline[0], "."), line) {
		return false
	}

	fSubline2 := strings.Split(fSubline[1], ".")
	if checkNumb([]string{fSubline2[0]}, line) {
		logs.Warnf("line %s matchWorkerVersion not char", line)
		return false
	}

	if !checkNumb([]string{fSubline2[1]}, line) {
		return false
	}

	return true
}

func checkNumb(subs []string, line string) bool {
	for _, s := range subs {
		_, err := strconv.ParseInt(s, 10, 64)
		if err != nil {
			logs.Warnf("line %s matchWorkerVersion not numb", line)
			return false
		}
	}
	return true
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
		jdkDirPath = getJavaDir()
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

	GAgentConfig.LogsKeepHours = logsKeepHours

	GAgentConfig.BatchInstallKey = strings.TrimSpace(conf.Section("").Key(KeyBatchInstall).String())

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
	logs.Info("logsKeepHours: ", GAgentConfig.LogsKeepHours)
	GAgentConfig.JdkDirPath = jdkDirPath
	logs.Info("jdkDirPath: ", GAgentConfig.JdkDirPath)
	GAgentConfig.DockerParallelTaskCount = dockerParallelTaskCount
	logs.Info("DockerParallelTaskCount: ", GAgentConfig.DockerParallelTaskCount)
	GAgentConfig.EnableDockerBuild = enableDocker
	logs.Info("EnableDockerBuild: ", GAgentConfig.EnableDockerBuild)
	GAgentConfig.Language = language
	logs.Info("Language:", GAgentConfig.Language)
	GAgentConfig.ImageDebugPortRange = imageDebugPortRange
	logs.Info("ImageDebugPortRange: ", GAgentConfig.ImageDebugPortRange)
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
	content.WriteString(KeyDockerTaskCount + "=" + strconv.Itoa(GAgentConfig.DockerParallelTaskCount) + "\n")
	content.WriteString(keyEnableDockerBuild + "=" + strconv.FormatBool(GAgentConfig.EnableDockerBuild) + "\n")
	content.WriteString(KeyLanguage + "=" + GAgentConfig.Language + "\n")
	content.WriteString(KeyImageDebugPortRange + "=" + GAgentConfig.ImageDebugPortRange + "\n")

	err := os.WriteFile(filePath, []byte(content.String()), 0666)
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
	if systemutil.IsMacos() {
		return GAgentConfig.JdkDirPath + "/Contents/Home/bin/java"
	} else {
		return GAgentConfig.JdkDirPath + "/bin/java"
	}
}

func SaveJdkDir(dir string) {
	if dir == GAgentConfig.JdkDirPath {
		return
	}
	GAgentConfig.JdkDirPath = dir
	err := GAgentConfig.SaveConfig()
	if err != nil {
		logs.Errorf("config.go|SaveJdkDir(dir=%s) failed: %s", dir, err.Error())
		return
	}
}

// getJavaDir 获取本地java文件夹
func getJavaDir() string {
	workDir := systemutil.GetWorkDir()
	if _, err := os.Stat(workDir + "/jdk"); err != nil && !os.IsExist(err) {
		return workDir + "/jre"
	}
	return workDir + "/jdk"
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
