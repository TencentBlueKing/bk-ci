//go:build linux

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

package upgrader

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	innerFileUtil "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"

	"github.com/gofrs/flock"
)

const (
	agentProcess  = "agent"
	daemonProcess = "daemon"
)

func DoUpgradeAgent() error {
	logs.Info("start upgrade agent")
	config.Init(false)
	if err := third_components.Init(); err != nil {
		logs.WithError(err).Error("init third_components error")
		systemutil.ExitProcess(1)
	}

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	err := totalLock.Lock()
	if err = totalLock.Lock(); err != nil {
		logs.WithError(err).Error("get total lock failed, exit")
		return errors.New("get total lock failed")
	}
	defer func() { totalLock.Unlock() }()

	daemonChange, _ := checkUpgradeFileChange(config.GetClientDaemonFile())
	agentChange, _ := checkUpgradeFileChange(config.GetClienAgentFile())
	if !agentChange && !daemonChange {
		logs.Info("upgrade nothing, exit")
		return nil
	}

	if daemonChange {
		err = replaceAgentFile(config.GetClientDaemonFile())
		if err != nil {
			logs.WithError(err).Error("replace daemon file failed")
		}
		tryKillAgentProcess(daemonProcess)
	}

	if agentChange {
		err = replaceAgentFile(config.GetClienAgentFile())
		if err != nil {
			logs.WithError(err).Error("replace agent file failed")
		}
		tryKillAgentProcess(agentProcess)
	}

	logs.Info("wait 2 seconds for agent to stop")
	time.Sleep(2 * time.Second)

	if daemonChange {
		// 使用 systemd 的使用 systemd 重启
		serviceName := fmt.Sprintf("devops_agent_%s.service", config.GAgentConfig.AgentId)
		if os.Geteuid() == 0 && serviceUnitExists(serviceName) {
			logs.Info("start upgrade agent by systemd")
			// 帮助老的使用私有 tmp 的替换下
			updatePrivateTmp(serviceName)
			err := restartServiceViaSystemctl(serviceName)
			// 启动失败则主动拉起 daemon 兜底
			if err != nil {
				logs.WithError(err).Error("restart service failed")
				if startErr := StartDaemon(); startErr != nil {
					logs.WithError(startErr).Error("start daemon failed")
					return startErr
				}
				logs.Info("agent start done")
			}
		} else {
			if startErr := StartDaemon(); startErr != nil {
				logs.WithError(startErr).Error("start daemon failed")
				return startErr
			}
			logs.Info("agent start done")
		}
	}
	logs.Info("agent upgrade done, upgrade process exiting")
	return nil
}

func tryKillAgentProcess(processName string) {
	logs.Info(fmt.Sprintf("try kill %s process", processName))
	pidFile := fmt.Sprintf("%s/%s.pid", systemutil.GetRuntimeDir(), processName)
	agentPid, err := fileutil.GetString(pidFile)
	if err != nil {
		logs.Warn(fmt.Sprintf("parse %s pid failed: %s", processName, err))
		return
	}
	intPid, err := strconv.Atoi(agentPid)
	if err != nil {
		logs.Warn(fmt.Sprintf("parse %s pid: %s failed", processName, agentPid))
		return
	}
	process, err := os.FindProcess(intPid)
	if err != nil || process == nil {
		logs.Warn(fmt.Sprintf("find %s process pid: %s failed", processName, agentPid))
		return
	} else {
		logs.Info(fmt.Sprintf("kill %s process, pid: %s", processName, agentPid))
		err = process.Kill()
		if err != nil {
			logs.Warn(fmt.Sprintf("kill %s pid: %s failed: %s", processName, agentPid, err))
			return
		}
	}
}

func DoUninstallAgent() error {
	err := UninstallAgent()
	if err != nil {
		logs.WithError(err).Error("uninstall agent failed")
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

func checkUpgradeFileChange(fileName string) (change bool, err error) {
	oldMd5, err := fileutil.GetFileMd5(systemutil.GetWorkDir() + "/" + fileName)
	if err != nil {
		logs.Error(fmt.Sprintf("agentUpgrade|check %s md5 failed", fileName), err)
		return false, errors.New("check old md5 failed")
	}

	newMd5, err := fileutil.GetFileMd5(systemutil.GetUpgradeDir() + "/" + fileName)
	if err != nil {
		logs.Error(fmt.Sprintf("agentUpgrade|check %s md5 failed", fileName), err)
		return false, errors.New("check new md5 failed")
	}

	return oldMd5 != newMd5, nil
}

func StartDaemon() error {
	logs.Info("starting ", config.GetClientDaemonFile())

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetClientDaemonFile()

	if err := fileutil.SetExecutable(startCmd); err != nil {
		logs.WithError(err).Warn("chmod daemon file failed")
		return err
	}

	pid, err := command.StartProcess(startCmd, nil, workDir, nil, "")
	logs.Info("pid: ", pid)
	if err != nil {
		logs.Error("run start daemon failed: ", err.Error())
		return err
	}
	return nil
}

func replaceAgentFile(fileName string) error {
	logs.Info("replace agent file: ", fileName)
	src := systemutil.GetUpgradeDir() + "/" + fileName
	dst := systemutil.GetWorkDir() + "/" + fileName

	// 查询 dst 的状态，如果没有的话使用预设权限\
	perm := constant.CommonFileModePerm
	if stat, err := os.Stat(dst); err != nil {
		logs.WithError(err).Warnf("replaceAgentFile %s stat error", dst)
	} else if stat != nil {
		perm = stat.Mode()
	}

	srcFile, err := os.Open(src)
	if err != nil {
		return errors.Wrapf(err, "replaceAgentFile open %s error", src)
	}

	if err := innerFileUtil.AtomicWriteFile(dst, srcFile, perm); err != nil {
		return errors.Wrapf(err, "replaceAgentFile AtomicWriteFile %s error", dst)
	}
	return nil
}

func restartServiceViaSystemctl(serviceName string) error {
	cmd := exec.Command("systemctl", "restart", serviceName)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("failed to restart service %s: %v\nOutput: %s", serviceName, err, string(output))
	}
	fmt.Printf("Successfully issued restart command for %s. Output:\n%s\n", serviceName, string(output))
	return nil
}

func updatePrivateTmp(serviceName string) {
	if os.Geteuid() != 0 {
		logs.Error("updatePrivateTmp must be run as root.")
	}

	// 步骤 1: 查询当前的 PrivateTmp 状态
	isEnabled, err := getServicePrivateTmpState(serviceName)
	if err != nil {
		logs.WithError(err).Error("Could not determine PrivateTmp state")
	}

	// 步骤 2: 根据状态决定是否需要修改
	if !isEnabled {
		logs.Infof("PrivateTmp is already disabled for %s. No action needed.", serviceName)
		return
	}

	logs.Infof("PrivateTmp is enabled for %s. Proceeding to disable it.", serviceName)

	// 步骤 3: 创建 override 文件来禁用 PrivateTmp
	if err := disableServicePrivateTmp(serviceName); err != nil {
		logs.WithError(err).Error("Error disabling PrivateTmp")
	}

	// 步骤 4: 让 systemd 重新加载配置
	if err := reloadSystemdDaemon(); err != nil {
		logs.WithError(err).Error("Error reloading systemd daemon")
	}

	logs.Info("Successfully disabled PrivateTmp and restarted the service.")

	// 修改脚本文件
	if _, err := modifyScriptPrivateTmp(filepath.Join(systemutil.GetWorkDir(), config.ScriptFileInstallLinux)); err != nil {
		logs.WithError(err).Error("modifyScriptPrivateTmp error")
	}
}

func disableServicePrivateTmp(serviceName string) error {
	// 1. 定义 override 目录和文件的路径
	overrideDir := fmt.Sprintf("/etc/systemd/system/%s.d", serviceName)
	overrideFile := filepath.Join(overrideDir, "private_tmp_override.conf")

	// 2. 创建 override 目录 (os.MkdirAll 会在目录已存在时什么都不做)
	logs.Infof("Ensuring override directory exists: %s", overrideDir)
	if err := os.MkdirAll(overrideDir, 0755); err != nil {
		return fmt.Errorf("failed to create override directory: %w", err)
	}

	// 3. 定义要写入的配置内容
	content := []byte("[Service]\nPrivateTmp=no\n")

	// 4. 写入配置文件
	logs.Infof("Writing override configuration to: %s", overrideFile)
	if err := os.WriteFile(overrideFile, content, 0644); err != nil {
		return fmt.Errorf("failed to write override file: %w", err)
	}

	logs.Info("Override file created successfully.")
	return nil
}

// ReloadSystemdDaemon tells systemd to reload its configuration from disk.
func reloadSystemdDaemon() error {
	logs.Info("Running 'systemctl daemon-reload'...")
	cmd := exec.Command("systemctl", "daemon-reload")
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("systemctl daemon-reload failed: %w", err)
	}
	logs.Info("Systemd daemon reloaded.")
	return nil
}

// GetServicePrivateTmpState queries systemd to determine if PrivateTmp is enabled for a service.
// It returns true if enabled, false otherwise.
func getServicePrivateTmpState(serviceName string) (bool, error) {
	logs.Infof("Checking PrivateTmp state for service: %s", serviceName)

	// Use 'systemctl show' which is designed for machine-readable output.
	// The --property flag isolates the exact value we need.
	cmd := exec.Command("systemctl", "show", serviceName, "--property=PrivateTmp")

	// Use CombinedOutput to capture stderr in case of errors (e.g., service not found)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return false, fmt.Errorf("failed to execute 'systemctl show': %w\nOutput: %s", err, string(output))
	}

	// The output will be in the format "PrivateTmp=yes" or "PrivateTmp=no".
	// We parse this to get the value.
	result := strings.TrimSpace(string(output))
	parts := strings.SplitN(result, "=", 2)

	if len(parts) != 2 {
		return false, fmt.Errorf("unexpected output format from 'systemctl show': %s", result)
	}

	value := parts[1]
	logs.Infof("Current PrivateTmp value is: '%s'", value)

	// systemd uses "yes" and "no" for boolean values in its properties.
	return value == "yes", nil
}

func modifyScriptPrivateTmp(filePath string) (bool, error) {
	logs.Infof("Processing file: %s", filePath)

	// 1. 获取原始文件信息，特别是权限
	fileInfo, err := os.Stat(filePath)
	if err != nil {
		// 如果文件不存在，os.Stat 会返回错误
		return false, fmt.Errorf("could not stat file: %w", err)
	}

	// 2. 读取整个文件内容
	content, err := os.ReadFile(filePath)
	if err != nil {
		return false, fmt.Errorf("could not read file: %w", err)
	}

	// 3. 逐行处理
	lines := strings.Split(string(content), "\n")
	var newLines []string
	wasModified := false

	for _, line := range lines {
		// 为了健壮性，我们先去除行首尾的空格再做判断
		trimmedLine := strings.TrimSpace(line)

		// 构造一个灵活的判断条件，忽略等号两边的空格
		// 例如，"PrivateTmp = true" 也能匹配
		if strings.HasPrefix(trimmedLine, "PrivateTmp") && strings.HasSuffix(trimmedLine, "true") {
			// 进一步确认格式是 key=value
			parts := strings.SplitN(trimmedLine, "=", 2)
			if len(parts) == 2 && strings.TrimSpace(parts[0]) == "PrivateTmp" && strings.TrimSpace(parts[1]) == "true" {
				// 找到了！现在进行替换。
				// 为了保留原始的缩进和等号旁边的空格，我们只替换 "true" 部分
				// 找到原始行中 "true" 的位置并替换
				lastIndex := strings.LastIndex(line, "true")
				if lastIndex != -1 {
					newLine := line[:lastIndex] + "false" + line[lastIndex+len("true"):]
					newLines = append(newLines, newLine)
					logs.Infof("Modification found: '%s' -> '%s'", line, newLine)
					wasModified = true
					continue // 继续下一行
				}
			}
		}

		// 如果不是目标行，则原样保留
		newLines = append(newLines, line)
	}

	// 4. 如果内容被修改了，则写回文件
	if !wasModified {
		log.Println("No modification needed. 'PrivateTmp=true' not found.")
		return false, nil
	}

	// 将修改后的行重新组合成文件内容
	output := strings.Join(newLines, "\n")

	// 使用原始文件的权限写回
	logs.Infof("Writing modified content back to %s", filePath)
	err = os.WriteFile(filePath, []byte(output), fileInfo.Mode())
	if err != nil {
		return true, fmt.Errorf("failed to write modified file: %w", err)
	}

	return true, nil
}

func serviceUnitExists(serviceName string) bool {
	// 使用 'systemctl cat'，它的退出码可以明确告诉我们服务单元是否存在。
	// 我们不需要输出，所以将其重定向到 /dev/null。
	cmd := exec.Command("systemctl", "cat", serviceName)
	// 在 Go 1.15+ 中，可以设置 cmd.Stdout = io.Discard 和 cmd.Stderr = io.Discard
	// 为了兼容性，这里使用 os.DevNull
	devNull, _ := os.Open(os.DevNull)
	defer devNull.Close()
	cmd.Stdout = devNull
	cmd.Stderr = devNull

	err := cmd.Run()
	return err == nil // 如果退出码为 0 (nil error)，则文件存在。
}
