package job

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"os/user"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/i18n"
)

// DoPollAndBuild 获取构建，如果达到最大并发或者是处于升级中，则不执行
func DoPollAndBuild() {
	for {
		if normalCanRun := checkWorkerCount(); !normalCanRun {
			time.Sleep(constant.BuildIntervalInSeconds * time.Second)
			continue
		}

		// 接构建任务
		buildInfo, err := api.StartUp()
		if err != nil {
			logs.WithError(err).Error("get build failed, retry")
			time.Sleep(constant.BuildIntervalInSeconds * time.Second)
			continue
		}

		if buildInfo == nil {
			logs.Info("no build to run, skip")
			time.Sleep(constant.BuildIntervalInSeconds * time.Second)
			continue
		}

		logs.Infof("build info %v", buildInfo)

		err = runBuild(buildInfo)
		if err != nil {
			logs.WithError(err).Error("start build failed")
		}
	}
}

// runBuild 启动构建
func runBuild(buildInfo *api.PersistenceBuildInfo) error {
	// 需要删除的文件
	toDelTmpFiles := make([]string, 0)

	workDir := config.Config.WorkDir
	agentJarPath := config.Config.WorkerPath
	if !fileutil.Exists(agentJarPath) {
		errorMsg := i18n.Localize("ExecutableFileMissing", map[string]interface{}{"filename": agentJarPath, "dir": workDir})
		logs.Error(errorMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errorMsg, api.LoseRunFileErrorEnum), toDelTmpFiles)
	}

	runUser := config.Config.WorkerUser

	goEnv := map[string]string{
		"DEVOPS_AGENT_VERSION":    config.AgentVersion,
		"DEVOPS_PROJECT_ID":       buildInfo.ProjectId,
		"DEVOPS_BUILD_ID":         buildInfo.BuildId,
		"DEVOPS_VM_SEQ_ID":        buildInfo.VmSeqId,
		"DEVOPS_FILE_GATEWAY":     config.Config.FileGateWay,
		"DEVOPS_GATEWAY":          config.Config.GateWay,
		"BK_CI_LOCALE_LANGUAGE":   config.Config.Language,
		"devops_project_id":       buildInfo.BuildId,
		"devops_agent_id":         buildInfo.AgentId,
		"devops_agent_secret_key": buildInfo.SecretKey,
		"devops_gateway":          config.Config.GateWay,
	}

	// 定义临时目录
	tmpDir, tmpMkErr := mkBuildTmpDir()
	if tmpMkErr != nil {
		errMsg := i18n.Localize("CreateTmpDirectoryFailed", map[string]interface{}{"err": tmpMkErr.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.MakeTmpDirErrorEnum), toDelTmpFiles)
		return tmpMkErr
	}

	startScriptFile, err := writeStartBuildAgentScript(buildInfo, tmpDir, toDelTmpFiles)
	if err != nil {
		errMsg := i18n.Localize("CreateStartScriptFailed", map[string]interface{}{"err": err.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.PrepareScriptCreateErrorEnum), toDelTmpFiles)
		return err
	}
	pid, err := startProcess(startScriptFile, []string{}, workDir, goEnv, runUser)
	if err != nil {
		errMsg := i18n.Localize("StartWorkerProcessFailed", map[string]interface{}{"err": err.Error()})
		logs.Error(errMsg)
		workerBuildFinish(buildInfo.ToFinish(false, errMsg, api.BuildProcessStartErrorEnum), toDelTmpFiles)
		return err
	}
	GBuildManager.AddBuild(pid, buildInfo, toDelTmpFiles)
	logs.Info(fmt.Sprintf("[%s]|Job#_%s|Build started, pid:%d ", buildInfo.BuildId, buildInfo.VmSeqId, pid))

	return nil
}

func writeStartBuildAgentScript(buildInfo *api.PersistenceBuildInfo, tmpDir string, toDelTmpFiles []string) (string, error) {
	logs.Info("write start build agent script to file")
	// 套娃，多加一层脚本，使用exec新起进程，这样才会读取 .bash_profile
	prepareScriptFile := fmt.Sprintf(
		"%s/devops_agent_prepare_start_%s_%s_%s.sh",
		config.Config.WorkDir,
		buildInfo.ProjectId,
		buildInfo.BuildId,
		buildInfo.VmSeqId,
	)
	scriptFile := fmt.Sprintf(
		"%s/devops_agent_start_%s_%s_%s.sh",
		config.Config.WorkDir,
		buildInfo.ProjectId,
		buildInfo.BuildId,
		buildInfo.VmSeqId,
	)

	errorMsgFile := getWorkerErrorMsgFile(buildInfo.BuildId, buildInfo.VmSeqId)
	toDelTmpFiles = append(toDelTmpFiles, scriptFile, prepareScriptFile, errorMsgFile)

	logs.Info("start agent script: ", scriptFile)
	agentLogPrefix := fmt.Sprintf("%s_%s_agent", buildInfo.BuildId, buildInfo.VmSeqId)
	lines := []string{
		"#!" + getCurrentShell(),
		fmt.Sprintf("cd %s", config.Config.WorkDir),
		fmt.Sprintf("%s -Ddevops.slave.agent.start.file=%s -Ddevops.slave.agent.prepare.start.file=%s "+
			"-Ddevops.agent.error.file=%s "+
			"-Dbuild.type=DOCKER -DLOG_PATH=%s -DAGENT_LOG_PREFIX=%s -Dsun.zip.disableMemoryMapping=true -Xmx1024m -Xms128m "+
			"-Djava.io.tmpdir=%s -jar %s",
			config.Config.JavaPath, scriptFile, prepareScriptFile,
			errorMsgFile,
			config.Config.LogDir, agentLogPrefix,
			tmpDir, config.Config.WorkerPath),
	}
	scriptContent := strings.Join(lines, "\n")

	err := os.WriteFile(scriptFile, []byte(scriptContent), os.ModePerm)
	defer func() {
		_ = os.Chmod(scriptFile, os.ModePerm)
		_ = os.Chmod(prepareScriptFile, os.ModePerm)
	}()
	if err != nil {
		return "", err
	} else {
		prepareScriptContent := strings.Join(getShellLines(scriptFile), "\n")
		err := os.WriteFile(prepareScriptFile, []byte(prepareScriptContent), os.ModePerm)
		if err != nil {
			return "", err
		} else {
			return prepareScriptFile, nil
		}
	}
}

func getCurrentShell() (shell string) {
	if config.Config.DetectShell {
		shell = os.Getenv("SHELL")
		if strings.TrimSpace(shell) == "" {
			shell = "/bin/bash"
		}
	} else {
		shell = "/bin/bash"
	}
	logs.Debug("current shell: ", shell)
	return
}

func encodedBuildInfo(buildInfo *api.PersistenceBuildInfo) string {
	strBuildInfo, _ := json.Marshal(buildInfo)
	logs.Debug("buildInfo: ", string(strBuildInfo))
	codedBuildInfo := base64.StdEncoding.EncodeToString(strBuildInfo)
	logs.Debug("base64: ", codedBuildInfo)
	return codedBuildInfo
}

// getShellLines 根据不同的shell的参数要求，这里可能需要不同的参数或者参数顺序
func getShellLines(scriptFile string) (newLines []string) {
	shell := getCurrentShell()
	switch shell {
	case "/bin/tcsh":
		newLines = []string{
			"#!" + shell,
			"exec " + shell + " " + scriptFile + " -l",
		}
	default:
		newLines = []string{
			"#!" + shell,
			"exec " + shell + " -l " + scriptFile,
		}
	}
	return newLines
}

func startProcess(command string, args []string, workDir string, envMap map[string]string, runUser string) (int, error) {
	cmd := exec.Command(command)

	if len(args) > 0 {
		cmd.Args = append(cmd.Args, args...)
	}

	if workDir != "" {
		cmd.Dir = workDir
	}

	cmd.Env = os.Environ()
	if envMap != nil {
		for k, v := range envMap {
			cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", k, v))
		}
	}

	err := setUser(cmd, runUser)
	if err != nil {
		logs.Error("set user failed: ", err.Error())
		return -1, errors.New(
			fmt.Sprintf("%s, Please check [devops.slave.user] in the {agent_dir}/.agent.properties", err.Error()))
	}

	logs.Info("cmd.Path: ", cmd.Path)
	logs.Info("cmd.Args: ", cmd.Args)
	logs.Info("cmd.workDir: ", cmd.Dir)
	logs.Info("runUser: ", runUser)

	err = cmd.Start()
	if err != nil {
		return -1, err
	}
	return cmd.Process.Pid, nil
}

const (
	envHome    = "HOME"
	envUser    = "USER"
	envLogName = "LOGNAME"
)

func setUser(cmd *exec.Cmd, runUser string) error {

	if len(runUser) == 0 { // 传空则直接返回
		return nil
	}
	// 解决重启构建机后，Linux的 /etc/rc.local 自动启动的agent，读取到HOME等系统变量为空的问题
	if runUser == config.Config.StartUser {
		envHomeFound := false
		envUserFound := false
		envLogNameFound := false
		for i := range cmd.Env {
			splits := strings.Split(cmd.Env[i], "=")
			if splits[0] == envHome && len(splits[1]) > 0 {
				envHomeFound = true
			} else if splits[0] == envUser && len(splits[1]) > 0 {
				envUserFound = true
			} else if splits[0] == envLogName && len(splits[1]) > 0 {
				envLogNameFound = true
			}
		}
		if envHomeFound && envUserFound && envLogNameFound {
			return nil
		}
	}

	logs.Info("set user(linux or darwin): ", runUser)

	rUser, err := user.Lookup(runUser)
	if err != nil {
		logs.Error("user lookup failed, user: -", runUser, "-, error: ", err.Error())
		return errors.New("user lookup failed, user: " + runUser)
	}
	uid, _ := strconv.Atoi(rUser.Uid)
	gid, _ := strconv.Atoi(rUser.Gid)
	cmd.SysProcAttr = &syscall.SysProcAttr{}
	cmd.SysProcAttr.Credential = &syscall.Credential{Uid: uint32(uid), Gid: uint32(gid)}

	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", envHome, rUser.HomeDir))
	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", envUser, runUser))
	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", envLogName, runUser))

	return nil
}
