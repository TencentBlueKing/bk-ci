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

package upgrade

import (
	"os"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/upgrade/download"
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
)

// DockerFileMd5 缓存，用来计算md5
var DockerFileMd5 struct {
	// 目前非linux机器不支持，以及一些机器不使用docker就不用计算md5
	NeedUpgrade bool
	FileModTime time.Time
	Md5         string
}

// 升级分为两个阶段，下载和升级，每个阶段都可能存在不升级的情况，这里用一个对象保证生命周期统一
type upgradeItems struct {
	Agent          bool
	Worker         bool
	Jdk            bool
	DockerInitFile bool
}

func (u *upgradeItems) NoChange() bool {
	return !u.Agent && !u.Worker && !u.Jdk && !u.DockerInitFile
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

	upItems := &upgradeItems{
		Agent:          upgradeItem.Agent,
		Worker:         upgradeItem.Worker,
		Jdk:            upgradeItem.Jdk,
		DockerInitFile: upgradeItem.DockerInitFile,
	}

	if upItems.NoChange() {
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

	// 下载升级文件
	logs.Infof("agentUpgrade|download upgrade files start %+v", upItems)
	downloadUpgradeFiles(upItems)
	if upItems.NoChange() {
		return
	}

	// 升级逻辑
	logs.Infof("agentUpgrade|download upgrade files done %+v", upItems)
	err := DoUpgradeOperation(upItems)
	if err != nil {
		logs.WithError(err).Error("agentUpgrade|do upgrade operation failed")
	} else {
		success = true
	}
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

// downloadUpgradeFiles 下载升级文件
func downloadUpgradeFiles(item *upgradeItems) {
	workDir := systemutil.GetWorkDir()
	upgradeDir := systemutil.GetUpgradeDir()
	_ = os.MkdirAll(upgradeDir, os.ModePerm)

	if item.Agent {
		item.Agent = downloadUpgradeAgent(workDir, upgradeDir)
	}

	if item.Worker {
		item.Worker = downloadUpgradeWorker(workDir, upgradeDir)
	}

	if item.Jdk {
		item.Jdk = downloadUpgradeJdk(upgradeDir)
	}

	if item.DockerInitFile {
		item.DockerInitFile = downloadUpgradeDockerInit(upgradeDir)
	}
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

	workerVersion := third_components.Worker.DetectWorkerVersionByDir(systemutil.GetUpgradeDir())
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
