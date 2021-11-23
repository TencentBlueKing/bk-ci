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

func DoPollAndUpgradeAgent() {
	for {
		time.Sleep(20 * time.Second)
		logs.Info("try upgrade")
		agentUpgrade()
		logs.Info("upgrade done")
	}
}

func agentUpgrade() {
	checkResult, err := api.CheckUpgrade()
	if err != nil {
		logs.Error("check upgrade err: ", err.Error())
		return
	}
	if !checkResult.IsOk() {
		logs.Error("check upgrade failed: ", checkResult.Message)
		return
	}

	if checkResult.IsAgentDelete() {
		logs.Info("agent is deleted, skip")
		return
	}

	if !(checkResult.Data).(bool) {
		logs.Info("no need to upgrade agent, skip")
		return
	}

	logs.Info("download upgrade files start")
	agentChanged, workerChanged, err := downloadUpgradeFiles()
	if err != nil {
		logs.Error("download upgrade files failed", err.Error())
		return
	}
	logs.Info("download upgrade files done")

	err = DoUpgradeOperation(agentChanged, workerChanged)
	if err != nil {
		logs.Error("do upgrade operation failed", err)
	}
}

func downloadUpgradeFiles() (agentChanged bool, workAgentChanged bool, err error) {
	workDir := systemutil.GetWorkDir()
	upgradeDir := systemutil.GetUpgradeDir()
	os.MkdirAll(upgradeDir, os.ModePerm)

	logs.Info("download upgrader start")
	_, err = api.DownloadUpgradeFile("upgrade/"+config.GetServerUpgraderFile(), upgradeDir+"/"+config.GetClientUpgraderFile())
	if err != nil {
		logs.Error("download upgrader failed", err)
		return false, false, errors.New("download upgrader failed")
	}
	logs.Info("download upgrader done")

	logs.Info("download daemon start")
	newDaemonMd5, err := api.DownloadUpgradeFile("upgrade/"+config.GetServerDaemonFile(), upgradeDir+"/"+config.GetClientDaemonFile())
	if err != nil {
		logs.Error("download daemon failed", err)
		return false, false, errors.New("download daemon failed")
	}
	logs.Info("download daemon done")

	logs.Info("download agent start")
	newAgentMd5, err := api.DownloadUpgradeFile("upgrade/"+config.GetServerAgentFile(), upgradeDir+"/"+config.GetClienAgentFile())
	if err != nil {
		logs.Error("download agent failed", err)
		return false, false, errors.New("download agent failed")
	}
	logs.Info("download agent done")

	logs.Info("download worker start")
	newWorkerMd5, err := api.DownloadUpgradeFile("jar/"+config.WorkAgentFile, upgradeDir+"/"+config.WorkAgentFile)
	if err != nil {
		logs.Error("download worker failed", err)
		return false, false, errors.New("download worker failed")
	}
	logs.Info("download worker done")

	daemonMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClientDaemonFile())
	if err != nil {
		logs.Error("check daemon md5 failed", err)
		return false, false, errors.New("check daemon md5 failed")
	}
	agentMd5, err := fileutil.GetFileMd5(workDir + "/" + config.GetClienAgentFile())
	if err != nil {
		logs.Error("check agent md5 failed", err)
		return false, false, errors.New("check agent md5 failed")
	}
	workerMd5, err := fileutil.GetFileMd5(workDir + "/" + config.WorkAgentFile)
	if err != nil {
		logs.Error("check worker md5 failed", err)
		return false, false, errors.New("check agent md5 failed")
	}

	return agentMd5 != newAgentMd5 || daemonMd5 != newDaemonMd5, workerMd5 != newWorkerMd5,nil
}
