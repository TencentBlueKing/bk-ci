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
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/utils/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/codesign"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
)

// UninstallAgent 卸载
func UninstallAgent() {
	logs.Info("start uninstall agent")

	err := runUninstallUpgrader(config.ActionUninstall)
	if err != nil {
		logs.Error("start upgrader failed")
		// 错误了也不退出，最少也要干掉daemon
	}
	logs.Warn("agent process exiting")
	systemutil.ExitProcess(constant.DaemonExitCode)
}

// verifyUpgradeBinaries 校验升级目录下 upgrader / agent / daemon 三个可执行文件
// 的代码签名。任一失败则整体拒绝拉起 upgrader。
//
// 若对应平台的信任锚（WinCertOrgName / MacosTeamId）为空，codesign.Verify
// 内部会直接返回 nil，因此此函数在未启用签名校验时是零成本的。
//
// 三个路径的计算复用 config 的跨平台 helper；Linux 下 daemon/agent 文件名相同，
// 通过 map 去重避免重复日志。
func verifyUpgradeBinaries() error {
	upgradeDir := systemutil.GetUpgradeDir()
	candidates := []string{
		upgradeDir + "/" + config.GetClientUpgraderFile(),
		upgradeDir + "/" + config.GetClienAgentFile(),
		upgradeDir + "/" + config.GetClientDaemonFile(),
	}
	seen := make(map[string]struct{}, len(candidates))
	for _, p := range candidates {
		if _, ok := seen[p]; ok {
			continue
		}
		seen[p] = struct{}{}
		if err := codesign.Verify(p); err != nil {
			return errors.Wrapf(err, "verify %s", p)
		}
	}
	return nil
}

// runUninstallUpgrader 卸载的区分开，方便进行退出处理
func runUninstallUpgrader(action string) error {
	logs.Info("agentUpgrade|start uninstall upgrader process")

	scripPath := systemutil.GetUpgradeDir() + "/" + config.GetClientUpgraderFile()

	if !systemutil.IsWindows() {
		err := systemutil.Chmod(scripPath, 0777)
		if err != nil {
			logs.WithError(err).Error("agentUpgrade|chmod failed")
			return errors.New("chmod failed: ")
		}
	}

	if err := verifyUpgradeBinaries(); err != nil {
		logs.WithError(err).Error("agentUpgrade|signature verify failed, refuse to run uninstall upgrader")
		return errors.Wrap(err, "signature verify failed")
	}

	args := []string{"-action=" + action}

	pid, err := command.StartProcess(scripPath, args, systemutil.GetWorkDir(), nil, "")
	if err != nil {
		logs.WithError(err).Error("agentUpgrade|run uninstall upgrader failed")
		return errors.New("run uninstall upgrader failed")
	}
	logs.Info("agentUpgrade|start uninstall process success, pid: ", pid)

	logs.Warn("agentUpgrade|agent uninstall process exiting")
	systemutil.ExitProcess(constant.DaemonExitCode)
	return nil
}

// runUpgrader 执行升级器
func runUpgrader(action string) error {
	logs.Info("agentUpgrade|start upgrader process")

	scripPath := systemutil.GetUpgradeDir() + "/" + config.GetClientUpgraderFile()

	if !systemutil.IsWindows() {
		err := systemutil.Chmod(scripPath, 0777)
		if err != nil {
			logs.WithError(err).Error("agentUpgrade|chmod failed")
			return errors.New("chmod failed: ")
		}
	}

	if err := verifyUpgradeBinaries(); err != nil {
		logs.WithError(err).Error("agentUpgrade|signature verify failed, refuse to run upgrader")
		return errors.Wrap(err, "signature verify failed")
	}

	if action != config.ActionUninstall {
		action = config.ActionUpgrade
	}
	args := []string{"-action=" + action}

	pid, err := command.StartProcess(scripPath, args, systemutil.GetWorkDir(), nil, "")
	if err != nil {
		logs.WithError(err).Error("agentUpgrade|run upgrader failed")
		return errors.New("run upgrader failed")
	}
	logs.Info("agentUpgrade|start process success, pid: ", pid)

	logs.Warn("agentUpgrade|agent process exiting")
	systemutil.ExitProcess(0)
	return nil
}

// DoUpgradeOperation 调用升级程序
func DoUpgradeOperation(upItems *upgradeItems) error {
	logs.Info("agentUpgrade|start upgrade, agent changed: ", upItems.Agent,
		", work agent changed: ", upItems.Worker,
		", jdk agent changed: ", upItems.Jdk,
		", docker init file changed: ", upItems.DockerInitFile,
	)

	if upItems.Jdk {
		err := DoUpgradeJdk()
		if err != nil {
			return err
		}
	} else {
		logs.Info("agentUpgrade|jdk not changed, skip agent upgrade")
	}

	if upItems.Worker {
		logs.Info("agentUpgrade|work agent changed, replace work agent file")
		_, err := fileutil.CopyFile(
			systemutil.GetUpgradeDir()+"/"+config.WorkAgentFile,
			systemutil.GetWorkDir()+"/"+config.WorkAgentFile,
			true)
		if err != nil {
			logs.WithError(err).Error("agentUpgrade|replace work agent file failed")
			return errors.New("replace work agent file failed")
		}
		logs.Info("agentUpgrade|replace agent file done")

		third_components.Worker.DetectWorkerVersion()
	} else {
		logs.Info("agentUpgrade|worker not changed, skip agent upgrade")
	}

	if upItems.Agent {
		logs.Info("agentUpgrade|agent changed, start upgrader")
		err := runUpgrader(config.ActionUpgrade)
		if err != nil {
			return err
		}
	} else {
		logs.Info("agentUpgrade|agent not changed, skip agent upgrade")
	}

	if upItems.DockerInitFile {
		logs.Info("agentUpgrade|docker init file changed, replace docker init file")
		_, err := fileutil.CopyFile(
			systemutil.GetUpgradeDir()+"/"+config.DockerInitFile,
			config.GetDockerInitFilePath(),
			true)
		if err != nil {
			logs.WithError(err).Error("agentUpgrade|replace work docker init file failed")
			return errors.New("replace work docker init file failed")
		}
		// 授予文件可执行权限，每次升级后赋予权限可以减少直接在启动时赋予的并发赋予的情况
		if err = systemutil.Chmod(config.GetDockerInitFilePath(), os.ModePerm); err != nil {
			logs.WithError(err).Error("agentUpgrade|chmod work docker init file failed")
			return errors.New("chmod work docker init file failed")
		}

		logs.Info("agentUpgrade|replace docker init file done")
	} else {
		logs.Info("agentUpgrade|docker init file not changed, skip agent upgrade")
	}

	return nil
}

func DoUpgradeJdk() error {
	logs.Info("agentUpgrade|jdk changed, replace jdk file")

	workDir := systemutil.GetWorkDir()

	// 解压缩为一个新文件取代旧文件路径，优先使用标准路径
	jdkTmpName := "jdk17"
	_, err := os.Stat(workDir + "/" + jdkTmpName)
	if !(err != nil && errors.Is(err, os.ErrNotExist)) {
		jdkTmpName = "jdk17-" + strconv.FormatInt(time.Now().Unix(), 10)
	}
	err = fileutil.Unzip(systemutil.GetUpgradeDir()+"/"+config.Jdk17ClientFile, workDir+"/"+jdkTmpName)
	if err != nil {
		return errors.Wrap(err, "upgrade jdk17 unzip error")
	}

	// 删除老的jdk文件，以及之前解压缩或者改名失败残留的，异步删除，删除失败也不影响主进程
	go func() {
		files, err := os.ReadDir(workDir)
		if err != nil {
			logs.WithError(err).Error("agentUpgrade|upgrade jdk17 remove old jdk file error")
			return
		}
		for _, file := range files {
			if file.Name() != jdkTmpName && (file.Name() == "jdk17" || file.Name() == "jdk17.zip" ||
				strings.HasPrefix(file.Name(), "jdk17-")) {
				err = os.RemoveAll(workDir + "/" + file.Name())
				if err != nil {
					logs.WithError(err).Error("agentUpgrade|upgrade jdk17 remove old jdk file error")
				}
			}
		}
	}()

	// 修改启动worker的jdk路径
	third_components.Jdk.Jdk17.SetJavaDir(workDir + "/" + jdkTmpName)

	logs.Info("agentUpgrade|replace jdk file done")

	return nil
}
