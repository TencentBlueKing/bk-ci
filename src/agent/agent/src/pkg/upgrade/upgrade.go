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
	"github.com/pkg/errors"
	"os"
	"strings"
	"sync/atomic"
	"time"

	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/upgrade/download"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
)

var JdkVersion = &JdkVersionType{}

// JdkVersionType jdk版本信息缓存
type JdkVersionType struct {
	JdkFileModTime time.Time
	// 版本信息，原子级的 []string
	version atomic.Value
}

func (j *JdkVersionType) GetVersion() []string {
	data := j.version.Load()
	if data == nil {
		return make([]string, 0)
	} else {
		return data.([]string)
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

// AgentUpgrade 升级主逻辑
func AgentUpgrade(upgradeItem *api.UpgradeItem, hasBuild bool) {
	// #5806 #5172 升级失败，保证重置回状态，防止一直处于升级状态
	ack := false
	success := false
	defer func() {
		if ack {
			_, _ = api.FinishUpgrade(success)
			logs.Info("agentUpgrade|report upgrade finish: ", success)
		}
	}()

	if !upgradeItem.Agent && !upgradeItem.Worker && !upgradeItem.Jdk && !upgradeItem.DockerInitFile {
		return
	} else {
		// 如果同时还领取了构建任务那么这次的升级取消
		if hasBuild {
			ack = true
			logs.Info("agentUpgrade|has build, skip")
			return
		}
	}

	ack = true

	// 进入升级逻辑时防止agent接构建任务，同时确保无任何构建任务在进行
	// 放到下载文件前，这样就不会因为有长时间构建任务重复下载文件
	// 使用 trylock 防止有频繁的任务时长时间阻塞住
	if !job.BuildTotalManager.Lock.TryLock() {
		return
	}
	defer func() {
		job.BuildTotalManager.Lock.Unlock()
	}()
	if job.CheckRunningJob() {
		logs.Infof(
			"agentUpgrade|agent has upgrade item, but has job running prejob: %s job: %s dockerJob: %s so skip.",
			job.GBuildManager.GetPreInstancesStr(),
			job.GBuildManager.GetInstanceStr(),
			job.GBuildDockerManager.GetInstanceStr(),
		)
		return
	}

	logs.Info("agentUpgrade|download upgrade files start")
	changeItems := downloadUpgradeFiles(upgradeItem)
	if changeItems.checkNoChange() {
		return
	}

	logs.Info("agentUpgrade|download upgrade files done")
	err := DoUpgradeOperation(changeItems)
	if err != nil {
		logs.WithError(err).Error("agentUpgrade|do upgrade operation failed")
	} else {
		success = true
	}
}

// SyncJdkVersion 同步jdk版本信息
func SyncJdkVersion() error {
	// 获取jdk文件状态以及时间
	stat, err := os.Stat(config.GAgentConfig.JdkDirPath)
	if err != nil {
		if os.IsNotExist(err) {
			logs.WithError(err).Error("syncJdkVersion no jdk dir find")
			// jdk版本置为空，否则会一直保持有版本的状态
			JdkVersion.SetVersion([]string{})
			return nil
		}
		return errors.Wrap(err, "agent check jdk dir error")
	}
	nowModTime := stat.ModTime()

	// 如果为空则必获取
	if len(JdkVersion.GetVersion()) == 0 {
		version, err := getJdkVersion()
		if err != nil {
			// 拿取错误时直接下载新的
			logs.WithError(err).Error("syncJdkVersion getJdkVersion err")
			return nil
		}
		JdkVersion.SetVersion(version)
		JdkVersion.JdkFileModTime = nowModTime
		return nil
	}

	// 判断文件夹最后修改时间，不一致时不用更改
	if nowModTime == JdkVersion.JdkFileModTime {
		return nil
	}

	version, err := getJdkVersion()
	if err != nil {
		// 拿取错误时直接下载新的
		logs.WithError(err).Error("syncJdkVersion getJdkVersion err")
		JdkVersion.SetVersion([]string{})
		return nil
	}
	JdkVersion.SetVersion(version)
	JdkVersion.JdkFileModTime = nowModTime
	return nil
}

func SyncDockerInitFileMd5() error {
	if !systemutil.IsLinux() || !config.GAgentConfig.EnableDockerBuild {
		DockerFileMd5.NeedUpgrade = false
		return nil
	}

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
		logs.WithError(err).Error("agent get jdk version failed")
		exitcode.CheckSignalJdkError(err)
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
	logs.Info("agentUpgrade|download upgrader start")
	_, err := download.DownloadUpgradeFile(upgradeDir)
	if err != nil {
		logs.WithError(err).Error("agentUpgrade|download upgrader failed")
		return false
	}
	logs.Info("agentUpgrade|download upgrader done")

	logs.Info("agentUpgrade|download daemon start")
	newDaemonMd5, err := download.DownloadDaemonFile(upgradeDir)
	if err != nil {
		logs.WithError(err).Error("agentUpgrade|download daemon failed")
		return false
	}
	logs.Info("agentUpgrade|download daemon done")

	logs.Info("agentUpgrade|download agent start")
	newAgentMd5, err := download.DownloadAgentFile(upgradeDir)
	if err != nil {
		logs.Error("agentUpgrade|download agent failed", err)
		return false
	}
	logs.Info("agentUpgrade|download agent done")

	daemonMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClientDaemonFile())
	if err != nil {
		logs.Error("agentUpgrade|check daemon md5 failed", err)
		return false
	}
	agentMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClienAgentFile())
	if err != nil {
		logs.Error("agentUpgrade|check agent md5 failed", err)
		return false
	}

	logs.Info("agentUpgrade|newDaemonMd5=" + newDaemonMd5 + ",daemonMd5=" + daemonMd5)
	logs.Info("agentUpgrade|newAgentMd5=" + newAgentMd5 + ",agentMd5=" + agentMd5)

	// #5806 增强检测版本，防止下载出错的情况下，意外被替换
	agentVersion := config.DetectAgentVersionByDir(systemutil.GetUpgradeDir())
	agentChanged = false
	if len(agentVersion) > 0 {
		agentChanged = agentMd5 != newAgentMd5
	}

	return agentChanged
}

func downloadUpgradeWorker(workDir, upgradeDir string) (workAgentChanged bool) {
	logs.Info("agentUpgrade|download worker start")
	newWorkerMd5, err := api.DownloadUpgradeFile("jar/"+config.WorkAgentFile, upgradeDir+"/"+config.WorkAgentFile)
	if err != nil {
		logs.Error("agentUpgrade|download worker failed", err)
		return false
	}
	logs.Info("agentUpgrade|download worker done")

	workerMd5, err := fileutil.GetFileMd5(workDir + "/" + config.WorkAgentFile)
	if err != nil {
		logs.Error("agentUpgrade|check worker md5 failed", err)
		return false
	}

	logs.Info("agentUpgrade|newWorkerMd5=" + newWorkerMd5 + ",workerMd5=" + workerMd5)

	workerVersion := config.DetectWorkerVersionByDir(systemutil.GetUpgradeDir())
	workAgentChanged = false
	if len(workerVersion) > 0 {
		workAgentChanged = workerMd5 != newWorkerMd5
	}

	return workAgentChanged
}

func downloadUpgradeJdk(upgradeDir string) (jdkChanged bool) {
	logs.Info("agentUpgrade|download jdk start")
	_, err := download.DownloadJdkFile(upgradeDir)
	if err != nil {
		logs.Error("agentUpgrade|download jdk failed", err)
		return false
	}
	logs.Info("agentUpgrade|download jdk done")

	return true
}

func downloadUpgradeDockerInit(upgradeDir string) bool {
	logs.Info("agentUpgrade|download docker init shell start")
	_, err := download.DownloadDockerInitFile(upgradeDir)
	if err != nil {
		logs.Error("agentUpgrade|download docker init shell failed", err)
		return false
	}
	logs.Info("agentUpgrade|download docker init shell done")

	return true
}
