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
	"errors"
	"os"
	"time"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/api"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/astaxie/beego/logs"
)

// DoPollAndUpgradeAgent 循环，每20s一次执行升级
func DoPollAndUpgradeAgent() {
	for {
		time.Sleep(20 * time.Second)
		logs.Info("try upgrade")
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
			config.GIsAgentUpgrading = false
			_, _ = api.FinishUpgrade(success)
			logs.Info("[agentUpgrade]|report upgrade finish: ", success)
		}
	}()
	checkResult, err := api.CheckUpgrade()
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

	if !(checkResult.Data).(bool) {
		logs.Info("[agentUpgrade]|no need to upgrade agent, skip")
		return
	}
	ack = true
	logs.Info("[agentUpgrade]|download upgrade files start")
	agentChanged, workerChanged, err := downloadUpgradeFiles()
	if err != nil {
		logs.Error("[agentUpgrade]|download upgrade files failed", err.Error())
		return
	}
	logs.Info("[agentUpgrade]|download upgrade files done")

	err = DoUpgradeOperation(agentChanged, workerChanged)
	if err != nil {
		logs.Error("[agentUpgrade]|do upgrade operation failed", err)
	} else {
		success = true
	}
}
// downloadUpgradeFiles 下载升级文件
func downloadUpgradeFiles() (agentChanged bool, workAgentChanged bool, err error) {
	workDir := systemutil.GetWorkDir()
	upgradeDir := systemutil.GetUpgradeDir()
	_ = os.MkdirAll(upgradeDir, os.ModePerm)

	logs.Info("[agentUpgrade]|download upgrader start")
	_, err = api.DownloadUpgradeFile(
		"upgrade/"+config.GetServerUpgraderFile(), upgradeDir+"/"+config.GetClientUpgraderFile())
	if err != nil {
		logs.Error("[agentUpgrade]|download upgrader failed", err)
		return false, false, errors.New("download upgrader failed")
	}
	logs.Info("[agentUpgrade]|download upgrader done")

	logs.Info("[agentUpgrade]|download daemon start")
	newDaemonMd5, err := api.DownloadUpgradeFile(
		"upgrade/"+config.GetServerDaemonFile(), upgradeDir+"/"+config.GetClientDaemonFile())
	if err != nil {
		logs.Error("[agentUpgrade]|download daemon failed", err)
		return false, false, errors.New("download daemon failed")
	}
	logs.Info("[agentUpgrade]|download daemon done")

	logs.Info("[agentUpgrade]|download agent start")
	newAgentMd5, err := api.DownloadUpgradeFile(
		"upgrade/"+config.GetServerAgentFile(), upgradeDir+"/"+config.GetClienAgentFile())
	if err != nil {
		logs.Error("[agentUpgrade]|download agent failed", err)
		return false, false, errors.New("download agent failed")
	}
	logs.Info("[agentUpgrade]|download agent done")

	logs.Info("[agentUpgrade]|download worker start")
	newWorkerMd5, err := api.DownloadUpgradeFile("jar/"+config.WorkAgentFile, upgradeDir+"/"+config.WorkAgentFile)
	if err != nil {
		logs.Error("[agentUpgrade]|download worker failed", err)
		return false, false, errors.New("download worker failed")
	}
	logs.Info("[agentUpgrade]|download worker done")

	daemonMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClientDaemonFile())
	if err != nil {
		logs.Error("[agentUpgrade]|check daemon md5 failed", err)
		return false, false, errors.New("check daemon md5 failed")
	}
	agentMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClienAgentFile())
	if err != nil {
		logs.Error("[agentUpgrade]|check agent md5 failed", err)
		return false, false, errors.New("check agent md5 failed")
	}
	workerMd5, err := fileutil.GetFileMd5(workDir + "/" + config.WorkAgentFile)
	if err != nil {
		logs.Error("[agentUpgrade]|check worker md5 failed", err)
		return false, false, errors.New("check agent md5 failed")
	}

	logs.Info("newDaemonMd5=" + newDaemonMd5 + ",daemonMd5=" + daemonMd5)
	logs.Info("newAgentMd5=" + newAgentMd5 + ",agentMd5=" + agentMd5)
	logs.Info("newWorkerMd5=" + newWorkerMd5 + ",workerMd5=" + workerMd5)

	// #5806 增强检测版本，防止下载出错的情况下，意外被替换
	agentVersion := config.DetectAgentVersionByDir(systemutil.GetUpgradeDir())
	agentChanged = false
	if len(agentVersion) > 0 {
		agentChanged = agentMd5 != newAgentMd5
	}

	workerVersion := config.DetectWorkerVersionByDir(systemutil.GetUpgradeDir())
	workAgentChanged = false
	if len(workerVersion) > 0 {
		workAgentChanged = workerMd5 != newWorkerMd5
	}
	// #4686 devopsDaemon 暂时不考虑单独的替换升级，windows 无法自动升级，仅当devopsAgent有变化时升级。
	return agentChanged, workAgentChanged, nil
}
