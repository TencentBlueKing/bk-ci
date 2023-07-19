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
	"io/ioutil"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/job"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

const DAEMON_EXIT_CODE = 88

// UninstallAgent 卸载
func UninstallAgent() {
	logs.Info("start uninstall agent")

	err := runUninstallUpgrader(config.ActionUninstall)
	if err != nil {
		logs.Error("start upgrader failed")
		// 错误了也不退出，最少也要干掉daemon
	}
	logs.Warn("agent process exiting")
	systemutil.ExitProcess(DAEMON_EXIT_CODE)
}

// runUninstallUpgrader 卸载的区分开，方便进行退出处理
func runUninstallUpgrader(action string) error {
	logs.Info("[agentUpgrade]|start uninstall upgrader process")

	scripPath := systemutil.GetUpgradeDir() + "/" + config.GetClientUpgraderFile()

	if !systemutil.IsWindows() {
		err := systemutil.Chmod(scripPath, 0777)
		if err != nil {
			logs.Error("[agentUpgrade]|chmod failed: ", err.Error())
			return errors.New("chmod failed: ")
		}
	}

	args := []string{"-action=" + action}

	pid, err := command.StartProcess(scripPath, args, systemutil.GetWorkDir(), nil, "")
	if err != nil {
		logs.Error("[agentUpgrade]|run uninstall upgrader failed: ", err.Error())
		return errors.New("run uninstall upgrader failed")
	}
	logs.Info("[agentUpgrade]|start uninstall process success, pid: ", pid)

	logs.Warn("[agentUpgrade]|agent uninstall process exiting")
	systemutil.ExitProcess(DAEMON_EXIT_CODE)
	return nil
}

// runUpgrader 执行升级器
func runUpgrader(action string) error {
	logs.Info("[agentUpgrade]|start upgrader process")

	scripPath := systemutil.GetUpgradeDir() + "/" + config.GetClientUpgraderFile()

	if !systemutil.IsWindows() {
		err := systemutil.Chmod(scripPath, 0777)
		if err != nil {
			logs.Error("[agentUpgrade]|chmod failed: ", err.Error())
			return errors.New("chmod failed: ")
		}
	}

	if action != config.ActionUninstall {
		action = config.ActionUpgrade
	}
	args := []string{"-action=" + action}

	pid, err := command.StartProcess(scripPath, args, systemutil.GetWorkDir(), nil, "")
	if err != nil {
		logs.Error("[agentUpgrade]|run upgrader failed: ", err.Error())
		return errors.New("run upgrader failed")
	}
	logs.Info("[agentUpgrade]|start process success, pid: ", pid)

	logs.Warn("[agentUpgrade]|agent process exiting")
	systemutil.ExitProcess(0)
	return nil
}

// DoUpgradeOperation 调用升级程序
func DoUpgradeOperation(changeItems upgradeChangeItem) error {
	logs.Info("[agentUpgrade]|start upgrade, agent changed: ", changeItems.AgentChanged,
		", work agent changed: ", changeItems.WorkAgentChanged,
		", jdk agent changed: ", changeItems.JdkChanged,
		", docker init file changed: ", changeItems.DockerInitFile,
	)

	if changeItems.checkNoChange() {
		logs.Info("[agentUpgrade]|no change to upgrade, skip")
		return nil
	}

	// 进入升级逻辑时防止agent接构建任务，同时确保无任何构建任务在进行
	job.BuildTotalManager.Lock.Lock()
	defer func() {
		job.BuildTotalManager.Lock.Unlock()
	}()
	if job.GBuildManager.GetPreInstancesCount() > 0 || job.GBuildManager.GetInstanceCount() > 0 ||
		job.GBuildDockerManager.GetInstanceCount() > 0 {
		logs.Infof("agent has upgrade item, but has job running prejob: %d, job: %d, dockerJob: %d. so skip.",
			job.GBuildManager.GetPreInstancesCount(), job.GBuildManager.GetInstanceCount(), job.GBuildDockerManager.GetInstanceCount())
		return nil
	}

	if changeItems.JdkChanged {
		logs.Info("[agentUpgrade]|jdk changed, replace jdk file")

		workDir := systemutil.GetWorkDir()
		// 复制出来jdk.zip
		_, err := fileutil.CopyFile(
			systemutil.GetUpgradeDir()+"/"+config.JdkClientFile,
			workDir+"/"+config.JdkClientFile,
			true,
		)
		if err != nil {
			return errors.Wrap(err, "upgrade jdk copy new jdk file error")
		}

		// 解压缩为一个新文件取代旧文件路径
		jdkTmpName := "jdk" + strconv.FormatInt(time.Now().Unix(), 10)
		err = fileutil.Unzip(workDir+"/"+config.JdkClientFile, workDir+"/"+jdkTmpName)
		if err != nil {
			return errors.Wrap(err, "upgrade jdk unzip error")
		}

		// 删除老的jdk文件，以及之前解压缩或者改名失败残留的，异步删除，删除失败也不影响主进程
		go func() {
			files, err := ioutil.ReadDir(workDir)
			if err != nil {
				logs.Error("upgrade jdk remove old jdk file error", err)
				return
			}
			for _, file := range files {
				if (strings.HasPrefix(file.Name(), "jdk") || strings.HasPrefix(file.Name(), "jre")) &&
					file.Name() != jdkTmpName {
					err = os.RemoveAll(workDir + "/" + file.Name())
					if err != nil {
						logs.Error("upgrade jdk remove old jdk file error", err)
					}
				}
			}
		}()

		// 修改启动worker的jdk路径
		config.SaveJdkDir(workDir + "/" + jdkTmpName)

		logs.Info("[agentUpgrade]|replace jdk file done")
	} else {
		logs.Info("[agentUpgrade]|jdk not changed, skip agent upgrade")
	}

	if changeItems.WorkAgentChanged {
		logs.Info("[agentUpgrade]|work agent changed, replace work agent file")
		_, err := fileutil.CopyFile(
			systemutil.GetUpgradeDir()+"/"+config.WorkAgentFile,
			systemutil.GetWorkDir()+"/"+config.WorkAgentFile,
			true)
		if err != nil {
			logs.Error("[agentUpgrade]|replace work agent file failed: ", err.Error())
			return errors.New("replace work agent file failed")
		}
		logs.Info("[agentUpgrade]|replace agent file done")

		config.GAgentEnv.SlaveVersion = config.DetectWorkerVersion()
	} else {
		logs.Info("[agentUpgrade]|worker not changed, skip agent upgrade")
	}

	if changeItems.AgentChanged {
		logs.Info("[agentUpgrade]|agent changed, start upgrader")
		err := runUpgrader(config.ActionUpgrade)
		if err != nil {
			return err
		}
	} else {
		logs.Info("[agentUpgrade]|agent not changed, skip agent upgrade")
	}

	if changeItems.DockerInitFile {
		logs.Info("[agentUpgrade]|docker init file changed, replace docker init file")
		_, err := fileutil.CopyFile(
			systemutil.GetUpgradeDir()+"/"+config.DockerInitFile,
			config.GetDockerInitFilePath(),
			true)
		if err != nil {
			logs.Error("[agentUpgrade]|replace work docker init file failed: ", err.Error())
			return errors.New("replace work docker init file failed")
		}
		// 授予文件可执行权限，每次升级后赋予权限可以减少直接在启动时赋予的并发赋予的情况
		if err = systemutil.Chmod(config.GetDockerInitFilePath(), os.ModePerm); err != nil {
			logs.Error("[agentUpgrade]|chmod work docker init file failed: ", err.Error())
			return errors.New("chmod work docker init file failed")
		}

		logs.Info("[agentUpgrade]|replace docker init file done")
	} else {
		logs.Info("[agentUpgrade]|docker init file not changed, skip agent upgrade")
	}

	return nil
}
