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

package upgrade

import (
	"os"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/upgrade/download"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/command"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

var JdkVersion = &JdkVersionType{}

// JdkVersion jdk版本信息缓存
type JdkVersionType struct {
	JdkFileModTime time.Time
	// 版本信息，原子级的 []string
	version atomic.Value
}

func (j *JdkVersionType) GetVersion() []string {
	data := j.version.Load()
	if data == nil {
		return []string{}
	} else {
		return j.version.Load().([]string)
	}
}

func (j *JdkVersionType) SetVersion(version []string) {
	if version == nil {
		version = []string{}
	}
	j.version.Swap(version)
}

// DockerFileMd5 缓存，用来计算md5
var DockerFileMd5 struct {
	// 目前非linux机器不支持，以及一些机器不使用docker就不用计算md5
	NeedUpgrade bool
	FileModTime time.Time
	Lock        sync.Mutex
	Md5         string
}

type upgradeChangeItem struct {
	AgentChanged     bool
	WorkAgentChanged bool
	JdkChanged       bool
	DockerInitFile   bool
}

func (u upgradeChangeItem) checkNoChange() bool {
	if !u.AgentChanged && !u.WorkAgentChanged && !u.JdkChanged && !u.DockerInitFile {
		return true
	}

	return false
}

// DoPollAndUpgradeAgent 循环，每20s一次执行升级
func DoPollAndUpgradeAgent() {
	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent upgrade panic: ", err)
		}
	}()

	for {
		time.Sleep(20 * time.Second)
		logs.Info("try upgrade")
		// debug模式下关闭升级，方便调试问题
		if config.IsDebug {
			logs.Debug("debug no upgrade")
			continue
		}
		agentUpgrade()
		logs.Info("upgrade done")
	}
}

// agentUpgrade 升级主逻辑
func agentUpgrade() {
	// #5806 #5172 升级失败，保证重置回状态，防止一直处于升级状态
	ack := false
	success := false
	defer func() {
		if ack {
			_, _ = api.FinishUpgrade(success)
			logs.Info("[agentUpgrade]|report upgrade finish: ", success)
		}
	}()

	jdkVersion, err := SyncJdkVersion()
	if err != nil {
		logs.Error("[agentUpgrade]|sync jdk version err: ", err.Error())
		return
	}

	err = SyncDockerInitFileMd5()
	if err != nil {
		logs.Error("[agentUpgrade]|sync docker file md5 err: ", err.Error())
		return
	}

	checkResult, err := api.CheckUpgrade(jdkVersion, api.DockerInitFileInfo{
		FileMd5:     DockerFileMd5.Md5,
		NeedUpgrade: DockerFileMd5.NeedUpgrade,
	})
	if err != nil {
		ack = true
		logs.Error("[agentUpgrade]|check upgrade err: ", err.Error())
		return
	}
	if !checkResult.IsOk() {
		logs.Error("[agentUpgrade]|check upgrade failed: ", checkResult.Message)
		return
	}

	if checkResult.IsAgentDelete() {
		logs.Info("[agentUpgrade]|agent is deleted, skip")
		return
	}

	upgradeItem := new(api.UpgradeItem)
	err = util.ParseJsonToData(checkResult.Data, &upgradeItem)
	if !upgradeItem.Agent && !upgradeItem.Worker && !upgradeItem.Jdk && !upgradeItem.DockerInitFile {
		logs.Info("[agentUpgrade]|no need to upgrade agent, skip")
		return
	}

	ack = true
	logs.Info("[agentUpgrade]|download upgrade files start")
	changeItems := downloadUpgradeFiles(upgradeItem)
	if changeItems.checkNoChange() {
		return
	}

	logs.Info("[agentUpgrade]|download upgrade files done")
	err = DoUpgradeOperation(changeItems)
	if err != nil {
		logs.Error("[agentUpgrade]|do upgrade operation failed", err)
	} else {
		success = true
	}
}

// SyncJdkVersion 同步jdk版本信息
func SyncJdkVersion() ([]string, error) {
	// 获取jdk文件状态以及时间
	stat, err := os.Stat(config.GAgentConfig.JdkDirPath)
	if err != nil {
		if os.IsNotExist(err) {
			logs.Error("syncJdkVersion no jdk dir find", err)
			// jdk版本置为空，否则会一直保持有版本的状态
			JdkVersion.SetVersion([]string{})
			return nil, nil
		}
		return nil, errors.Wrap(err, "agent check jdk dir error")
	}
	nowModTime := stat.ModTime()

	// 如果为空则必获取
	if len(JdkVersion.GetVersion()) == 0 {
		version, err := getJdkVersion()
		if err != nil {
			// 拿取错误时直接下载新的
			logs.Error("syncJdkVersion getJdkVersion err", err)
			return nil, nil
		}
		JdkVersion.SetVersion(version)
		JdkVersion.JdkFileModTime = nowModTime
		return version, nil
	}

	// 判断文件夹最后修改时间，不一致时不用更改
	if nowModTime == JdkVersion.JdkFileModTime {
		return JdkVersion.GetVersion(), nil
	}

	version, err := getJdkVersion()
	if err != nil {
		// 拿取错误时直接下载新的
		logs.Error("syncJdkVersion getJdkVersion err", err)
		JdkVersion.SetVersion([]string{})
		return nil, nil
	}
	JdkVersion.SetVersion(version)
	JdkVersion.JdkFileModTime = nowModTime
	return version, nil
}

func SyncDockerInitFileMd5() error {
	if !systemutil.IsLinux() || !config.GAgentConfig.EnableDockerBuild {
		DockerFileMd5.NeedUpgrade = false
		return nil
	}
	DockerFileMd5.Lock.Lock()
	defer func() {
		DockerFileMd5.Lock.Unlock()
	}()
	DockerFileMd5.NeedUpgrade = true

	filePath := config.GetDockerInitFilePath()

	stat, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			logs.Warn("syncDockerInitFileMd5 no docker init file find", err)
			DockerFileMd5.Md5 = ""
			return nil
		}
		return errors.Wrap(err, "agent check docker init file error")
	}
	nowModTime := stat.ModTime()

	if DockerFileMd5.Md5 == "" {
		DockerFileMd5.Md5, err = fileutil.GetFileMd5(filePath)
		if err != nil {
			DockerFileMd5.Md5 = ""
			return errors.Wrapf(err, "agent get docker init file %s md5 error", filePath)
		}
		DockerFileMd5.FileModTime = nowModTime
		return nil
	}

	if nowModTime == DockerFileMd5.FileModTime {
		return nil
	}

	DockerFileMd5.Md5, err = fileutil.GetFileMd5(filePath)
	if err != nil {
		DockerFileMd5.Md5 = ""
		return errors.Wrapf(err, "agent get docker init file %s md5 error", filePath)
	}
	DockerFileMd5.FileModTime = nowModTime
	return nil
}

func getJdkVersion() ([]string, error) {
	jdkVersion, err := command.RunCommand(config.GetJava(), []string{"-version"}, "", nil)
	if err != nil {
		logs.Error("agent get jdk version failed: ", err.Error())
		return nil, errors.Wrap(err, "agent get jdk version failed")
	}
	var jdkV []string
	if jdkVersion != nil {
		versionOutputString := strings.TrimSpace(string(jdkVersion))
		jdkV = trimJdkVersionList(versionOutputString)
	}

	return jdkV, nil
}

// parseJdkVersionList 清洗在解析一些版本信息的干扰信息,避免因tmp空间满等导致识别不准确造成重复不断的升级
func trimJdkVersionList(versionOutputString string) []string {
	/*
		OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:
		   32490
		Try using the -Djava.io.tmpdir= option to select an alternate temp location.

		openjdk version "1.8.0_352"
		OpenJDK Runtime Environment (Tencent Kona 8.0.12) (build 1.8.0_352-b1)
		OpenJDK 64-Bit Server VM (Tencent Kona 8.0.12) (build 25.352-b1, mixed mode)
		Picked up _JAVA_OPTIONS: -Xmx8192m -Xms256m -Xss8m
	*/
	// 一个JVM版本只需要识别3行。
	var jdkV = make([]string, 3)

	var sep = "\n"
	if strings.HasSuffix(versionOutputString, "\r\n") {
		sep = "\r\n"
	}

	lines := strings.Split(strings.TrimSuffix(versionOutputString, sep), sep)

	var pos = 0
	for i := range lines {

		if pos == 0 {
			if strings.Contains(lines[i], " version ") {
				jdkV[pos] = lines[i]
				pos++
			}
		} else if pos == 1 {
			if strings.Contains(lines[i], " Runtime Environment ") {
				jdkV[pos] = lines[i]
				pos++
			}
		} else if pos == 2 {
			if strings.Contains(lines[i], " Server VM ") {
				jdkV[pos] = lines[i]
				break
			}
		}
	}

	return jdkV
}

// downloadUpgradeFiles 下载升级文件
func downloadUpgradeFiles(item *api.UpgradeItem) upgradeChangeItem {
	workDir := systemutil.GetWorkDir()
	upgradeDir := systemutil.GetUpgradeDir()
	_ = os.MkdirAll(upgradeDir, os.ModePerm)

	result := upgradeChangeItem{}

	if !item.Agent {
		result.AgentChanged = false
	} else {
		result.AgentChanged = downloadUpgradeAgent(workDir, upgradeDir)
	}

	if !item.Worker {
		result.WorkAgentChanged = false
	} else {
		result.WorkAgentChanged = downloadUpgradeWorker(workDir, upgradeDir)
	}

	if !item.Jdk {
		result.JdkChanged = false
	} else {
		result.JdkChanged = downloadUpgradeJdk(upgradeDir)
	}

	if !item.DockerInitFile {
		result.DockerInitFile = false
	} else {
		result.DockerInitFile = downloadUpgradeDockerInit(upgradeDir)
	}

	return result
}

func downloadUpgradeAgent(workDir, upgradeDir string) (agentChanged bool) {
	// #4686 devopsDaemon 暂时不考虑单独的替换升级，windows 无法自动升级，仅当devopsAgent有变化时升级。
	logs.Info("[agentUpgrade]|download upgrader start")
	_, err := download.DownloadUpgradeFile(upgradeDir)
	if err != nil {
		logs.Error("[agentUpgrade]|download upgrader failed", err)
		return false
	}
	logs.Info("[agentUpgrade]|download upgrader done")

	logs.Info("[agentUpgrade]|download daemon start")
	newDaemonMd5, err := download.DownloadDaemonFile(upgradeDir)
	if err != nil {
		logs.Error("[agentUpgrade]|download daemon failed", err)
		return false
	}
	logs.Info("[agentUpgrade]|download daemon done")

	logs.Info("[agentUpgrade]|download agent start")
	newAgentMd5, err := download.DownloadAgentFile(upgradeDir)
	if err != nil {
		logs.Error("[agentUpgrade]|download agent failed", err)
		return false
	}
	logs.Info("[agentUpgrade]|download agent done")

	daemonMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClientDaemonFile())
	if err != nil {
		logs.Error("[agentUpgrade]|check daemon md5 failed", err)
		return false
	}
	agentMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClienAgentFile())
	if err != nil {
		logs.Error("[agentUpgrade]|check agent md5 failed", err)
		return false
	}

	logs.Info("newDaemonMd5=" + newDaemonMd5 + ",daemonMd5=" + daemonMd5)
	logs.Info("newAgentMd5=" + newAgentMd5 + ",agentMd5=" + agentMd5)

	// #5806 增强检测版本，防止下载出错的情况下，意外被替换
	agentVersion := config.DetectAgentVersionByDir(systemutil.GetUpgradeDir())
	agentChanged = false
	if len(agentVersion) > 0 {
		agentChanged = agentMd5 != newAgentMd5
	}

	return agentChanged
}

func downloadUpgradeWorker(workDir, upgradeDir string) (workAgentChanged bool) {
	logs.Info("[agentUpgrade]|download worker start")
	newWorkerMd5, err := api.DownloadUpgradeFile("jar/"+config.WorkAgentFile, upgradeDir+"/"+config.WorkAgentFile)
	if err != nil {
		logs.Error("[agentUpgrade]|download worker failed", err)
		return false
	}
	logs.Info("[agentUpgrade]|download worker done")

	workerMd5, err := fileutil.GetFileMd5(workDir + "/" + config.WorkAgentFile)
	if err != nil {
		logs.Error("[agentUpgrade]|check worker md5 failed", err)
		return false
	}

	logs.Info("newWorkerMd5=" + newWorkerMd5 + ",workerMd5=" + workerMd5)

	workerVersion := config.DetectWorkerVersionByDir(systemutil.GetUpgradeDir())
	workAgentChanged = false
	if len(workerVersion) > 0 {
		workAgentChanged = workerMd5 != newWorkerMd5
	}

	return workAgentChanged
}

func downloadUpgradeJdk(upgradeDir string) (jdkChanged bool) {
	logs.Info("[agentUpgrade]|download jdk start")
	_, err := download.DownloadJdkFile(upgradeDir)
	if err != nil {
		logs.Error("[agentUpgrade]|download jdk failed", err)
		return false
	}
	logs.Info("[agentUpgrade]|download jdk done")

	return true
}

func downloadUpgradeDockerInit(upgradeDir string) bool {
	logs.Info("[agentUpgrade]|download docker init shell start")
	_, err := download.DownloadDockerInitFile(upgradeDir)
	if err != nil {
		logs.Error("[agentUpgrade]|download docker init shell failed", err)
		return false
	}
	logs.Info("[agentUpgrade]|download docker init shell done")

	return true
}
