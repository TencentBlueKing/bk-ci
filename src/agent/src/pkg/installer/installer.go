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

package installer

import (
	"errors"
	"fmt"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"

	"github.com/gofrs/flock"
)

func DoInstallAgent() error {
	logs.Info("start install agent...")
	config.Init(false)

	if len(config.GAgentConfig.BatchInstallKey) == 0 {
		return errors.New("file .agent.properties 's devops.agent.batch.install key is null")
	}

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	err := totalLock.Lock()
	if err = totalLock.Lock(); err != nil {
		logs.Error("get total lock failed, exit", err.Error())
		return errors.New("get total lock failed")
	}
	defer func() { totalLock.Unlock() }()

	workDir := systemutil.GetWorkDir()
	sourceZipFile := workDir + "/batch.zip"
	logs.Info("download batch.zip start")
	err1 := api.DownloadAgentInstallBatchZip(sourceZipFile)
	if err1 != nil {
		logs.Error("download batch.zip failed", err1)
		return errors.New(fmt.Sprintf("download %s fail", sourceZipFile))
	}
	logs.Info(fmt.Sprintf("download %s end", sourceZipFile))

	logs.Info(fmt.Sprintf("unzip %s to %s start", sourceZipFile, workDir))
	errUnzip := fileutil.Unzip(sourceZipFile, workDir)
	if errUnzip != nil {
		logs.Error("unzip batch.zip failed", errUnzip)
		return errors.New(fmt.Sprintf("unzip %s fail", sourceZipFile))
	}
	logs.Info(fmt.Sprintf("unzip %s to %s end", sourceZipFile, workDir))

	logs.Info("uninstall agent start")
	errU := UninstallAgent()
	if errU != nil {
		logs.Error("uninstall agent failed: ", errU.Error())
		return errU
	}

	logs.Info("install agent start")
	errI := InstallAgent()
	if errI != nil {
		logs.Error("install agent failed: ", errI.Error())
		return errI
	}
	logs.Info("install agent done")
	return nil
}

func DoUninstallAgent() error {
	err := UninstallAgent()
	if err != nil {
		logs.Error("uninstall agent failed: ", err.Error())
		return errors.New("uninstall agent failed")
	}
	return nil
}

func UninstallAgent() error {
	logs.Info("start uninstall agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetUninstallScript()
	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run uninstall script failed: ", err.Error())
		logs.Error("output: ", string(output))
		return errors.New("run uninstall script failed")
	}
	logs.Info("output: ", string(output))
	return nil
}

func InstallAgent() error {
	logs.Info("start install agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetInstallScript()

	err := fileutil.SetExecutable(startCmd)
	if err != nil {
		return fmt.Errorf("chmod install script failed: %s", err.Error())
	}

	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run install script failed: ", err.Error())
		logs.Error("output: ", string(output))
		return errors.New("run install script failed")
	}
	logs.Info("output: ", string(output))
	return nil
}
