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
	"github.com/Tencent/bk-ci/src/agent/src/pkg/upgrade/download"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/command"
	"github.com/pkg/errors"
	"os"
	"strings"
	"sync/atomic"
	"time"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/api"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/logs"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
)

//JdkVersion jdk版本信息缓存
var JdkVersion struct {
	JdkFileModTime time.Time
	// 版本信息，原子级的 []string
	Version atomic.Value
}

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

	jdkVersion, err := syncJdkVersion()
	if err != nil {
		logs.Error("[agentUpgrade]|sync jdk version err: ", err.Error())
		return
	}
	checkResult, err := api.CheckUpgrade(jdkVersion)
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
	if !upgradeItem.Agent && !upgradeItem.Worker && !upgradeItem.Jdk {
		logs.Info("[agentUpgrade]|no need to upgrade agent, skip")
		return
	}

	ack = true
	logs.Info("[agentUpgrade]|download upgrade files start")
	agentChanged, workerChanged, jdkChanged := downloadUpgradeFiles(upgradeItem)
	if !agentChanged && !workerChanged && !jdkChanged {
		return
	}

	logs.Info("[agentUpgrade]|download upgrade files done")
	err = DoUpgradeOperation(agentChanged, workerChanged, jdkChanged)
	if err != nil {
		logs.Error("[agentUpgrade]|do upgrade operation failed", err)
	} else {
		success = true
	}
}

//syncJdkVersion 同步jdk版本信息
func syncJdkVersion() ([]string, error) {
	// 获取jdk文件状态以及时间
	stat, err := os.Stat(config.GetJavaDir())
	if err != nil {
		return nil, errors.Wrap(err, "agent check jdk dir error")
	}
	nowModTime := stat.ModTime()

	// 如果为空则必获取
	if JdkVersion.Version.Load() == nil {
		version, err := getJdkVersion()
		if err != nil {
			return nil, err
		}
		JdkVersion.Version.Swap(version)
		JdkVersion.JdkFileModTime = nowModTime
		return version, nil
	}

	// 判断文件夹最后修改时间，不一致时不用更改
	if nowModTime == JdkVersion.JdkFileModTime {
		return JdkVersion.Version.Load().([]string), nil
	}

	version, err := getJdkVersion()
	if err != nil {
		return nil, err
	}
	JdkVersion.Version.Swap(version)
	JdkVersion.JdkFileModTime = nowModTime
	return version, nil
}

func getJdkVersion() ([]string, error) {
	jdkVersion, err := command.RunCommand(config.GetJava(), []string{"-version"}, "", nil)
	if err != nil {
		logs.Error("agent get jdk version failed: ", err.Error())
		return nil, errors.Wrap(err, "agent get jdk version failed")
	}
	var jdkV []string
	if jdkVersion != nil {
		jdkV = strings.Split(strings.TrimSuffix(strings.TrimSpace(string(jdkVersion)), "\n"), "\n")
		for i, j := range jdkV {
			jdkV[i] = strings.TrimSpace(j)
		}
	}

	return jdkV, nil
}

// downloadUpgradeFiles 下载升级文件
func downloadUpgradeFiles(item *api.UpgradeItem) (agentChanged, workAgentChanged, jdkChanged bool) {
	workDir := systemutil.GetWorkDir()
	upgradeDir := systemutil.GetUpgradeDir()
	_ = os.MkdirAll(upgradeDir, os.ModePerm)

	if !item.Agent {
		agentChanged = false
	} else {
		agentChanged = downloadUpgradeAgent(workDir, upgradeDir)
	}

	if !item.Worker {
		workAgentChanged = false
	} else {
		workAgentChanged = downloadUpgradeWorker(workDir, upgradeDir)
	}

	if !item.Jdk {
		jdkChanged = false
	} else {
		jdkChanged = downloadUpgradeJdk(upgradeDir)
	}

	return agentChanged, workAgentChanged, jdkChanged
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
