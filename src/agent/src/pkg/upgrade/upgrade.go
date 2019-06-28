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
	"github.com/astaxie/beego/logs"
	"os"
	"pkg/api"
	"pkg/config"
	"time"
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

	agentChanged, workAgentChanged, err := downloadFile()
	if err != nil {
		logs.Error("download file err: ", err.Error())
		return
	}

	err = DoUpgradeOperation(agentChanged, workAgentChanged)
	if err != nil {
		logs.Error("do upgrade operation failed", err)
	}
}

func downloadFile() (agentChanged bool, workAgentChanged bool, err error) {
	logs.Info("start download new agent")

	tmpDir := config.GetAgentWorkdir() + "/tmp"
	os.MkdirAll(config.GetAgentWorkdir()+"/tmp", os.ModePerm)

	logs.Info("start download upgrader")
	upgraderChanged, err := api.DownloadUpgradeFile("upgrade/"+config.GetServerUpgraderFile(), tmpDir+"/"+config.GetClientUpgraderFile())
	if err != nil {
		return true, true, err
	}
	logs.Info("upgrader download done")

	logs.Info("start download agent")
	agentChanged, err = api.DownloadUpgradeFile("upgrade/"+config.GetServerAgentFile(), tmpDir+"/"+config.GetClienAgentFile())
	if err != nil {
		return true, true, err
	}
	logs.Info("agent download done")

	logs.Info("start download work agent")
	workAgentChanged, err = api.DownloadUpgradeFile("jar/"+config.WorkAgentFile, tmpDir+"/"+config.WorkAgentFile)
	if err != nil {
		return upgraderChanged || agentChanged, true, err
	}
	logs.Info("work agent download done")

	return upgraderChanged || agentChanged, workAgentChanged, nil
}
