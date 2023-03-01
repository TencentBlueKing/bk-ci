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
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/upgrade/download"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/upgrade/item"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

type upgradeChangeItem struct {
	AgentChanged     bool
	WorkAgentChanged bool
	JdkChanged       bool
	DockerInitFile   bool
	TelegrafConf     bool
}

func (u upgradeChangeItem) checkNoChange() bool {
	if !u.AgentChanged && !u.WorkAgentChanged && !u.JdkChanged && !u.DockerInitFile && !u.TelegrafConf {
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

	jdkVersion, err := item.SyncJdkVersion()
	if err != nil {
		logs.Error("[agentUpgrade]|sync jdk version err: ", err.Error())
		return
	}

	err = item.DockerFileMd5.Sync()
	if err != nil {
		logs.Error("[agentUpgrade]|sync docker file md5 err: ", err.Error())
		return
	}
	dockerMd5, needUpgrade := item.DockerFileMd5.Read()

	err = item.TelegrafConf.Sync()
	if err != nil {
		logs.Error("[agentUpgrade]|sync telegraf conf file md5 err: ", err.Error())
		return
	}
	telegrafMd5 := item.TelegrafConf.Read()

	checkResult, err := api.CheckUpgrade(jdkVersion, api.DockerInitFileInfo{
		FileMd5:     dockerMd5,
		NeedUpgrade: needUpgrade,
	}, api.TelegrafConfInfo{FileMd5: telegrafMd5})
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

	if !item.TelegrafConf {
		result.TelegrafConf = false
	} else {
		result.TelegrafConf = downloadUpgradeTelegrafConf(upgradeDir)
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

func downloadUpgradeTelegrafConf(upgradeDir string) bool {
	logs.Info("[agentUpgrade]|download telegraf config file start")
	_, err := download.DownloadTelegrafConfFile(upgradeDir)
	if err != nil {
		logs.Error("[agentUpgrade]|download telegraf config failed", err)
		return false
	}
	logs.Info("[agentUpgrade]|download telegraf config done")

	return true
}
