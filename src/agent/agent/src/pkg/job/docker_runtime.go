package job

import (
	"context"
	"fmt"
	"os"
	"runtime"
	"strings"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/dockercli"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
	envvars "github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job_docker"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

const debugEntryPointCmd = "while true; do sleep 5m; done"
const (
	targetJreDir  = "/usr/local/jre"
	targetJre8Dir = "/usr/local/jre8"
)

func newBuildDockerRunner(buildInfo *api.ThirdPartyBuildInfo) *dockercli.Runner {
	return dockercli.NewRunnerWithEvent(systemutil.GetWorkDir(), func(entry dockercli.LogEntry) {
		msg := entry.Message
		switch entry.Level {
		case dockercli.LogLevelDebug:
			logs.Debugf("DOCKER_JOB|%s", msg)
		case dockercli.LogLevelWarn:
			logs.Warnf("DOCKER_JOB|%s", msg)
		case dockercli.LogLevelError:
			logs.Errorf("DOCKER_JOB|%s", msg)
		default:
			logs.Infof("DOCKER_JOB|%s", msg)
		}
		postLog(false, "[docker] "+msg, buildInfo, mapDockerRunnerLogLevel(entry.Level))
	})
}

func newPlainDockerRunner(workDir string) *dockercli.Runner {
	return dockercli.NewRunnerWithEvent(workDir, func(entry dockercli.LogEntry) {
		switch entry.Level {
		case dockercli.LogLevelDebug:
			logs.Debug(entry.Message)
		case dockercli.LogLevelWarn:
			logs.Warn(entry.Message)
		case dockercli.LogLevelError:
			logs.Error(entry.Message)
		default:
			logs.Info(entry.Message)
		}
	})
}

func mapDockerRunnerLogLevel(level dockercli.LogLevel) api.LogType {
	switch level {
	case dockercli.LogLevelDebug:
		return api.LogtypeDebug
	case dockercli.LogLevelWarn:
		return api.LogtypeWarn
	case dockercli.LogLevelError:
		return api.LogtypeError
	default:
		return api.LogtypeLog
	}
}

func buildDockerCreateArgs(containerName, image string, buildInfo *api.ThirdPartyBuildInfo) ([]string, error) {
	dockerBuildInfo := buildInfo.DockerBuildInfo
	mountArgs, err := parseContainerMountArgs(buildInfo)
	if err != nil {
		return nil, err
	}
	userArgs, err := job_docker.BuildUserDockerArgs(dockerBuildInfo.Options)
	if err != nil {
		return nil, err
	}

	args := []string{"--name", containerName}
	args = append(args, userArgs...)
	if !job_docker.HasCustomNetwork(dockerBuildInfo.Options) {
		args = append(args, "--network", "bridge")
	}
	for _, e := range parseContainerEnv(dockerBuildInfo) {
		args = append(args, "-e", e)
	}
	if v, ok := envs.FetchEnv(constant.DevopsAgentDockerCapAdd); ok && strings.TrimSpace(v) != "" {
		args = append(args, "--cap-add", strings.TrimSpace(v))
	}
	args = append(args, mountArgs...)
	args = append(args, "--entrypoint", "/bin/sh", image, "-c", entryPointCmd)
	return args, nil
}

func parseContainerMountArgs(buildInfo *api.ThirdPartyBuildInfo) ([]string, error) {
	var args []string

	if hasJdk17Dir() {
		args = append(args,
			"--mount", fmt.Sprintf("type=bind,source=%s,target=%s,readonly", config.GAgentConfig.Jdk17DirPath, targetJreDir),
			"--mount", fmt.Sprintf("type=bind,source=%s,target=%s,readonly", config.GAgentConfig.JdkDirPath, targetJre8Dir),
		)
	} else {
		args = append(args,
			"--mount", fmt.Sprintf("type=bind,source=%s,target=%s,readonly", config.GAgentConfig.JdkDirPath, targetJreDir),
		)
	}

	args = append(args,
		"--mount", fmt.Sprintf("type=bind,source=%s,target=%s,readonly", config.BuildAgentJarPath(), "/data/worker-agent.jar"),
		"--mount", fmt.Sprintf("type=bind,source=%s,target=%s,readonly", config.GetDockerInitFilePath(), entryPointCmd),
	)

	workDir := systemutil.GetWorkDir()
	dataDir := fmt.Sprintf("%s/%s/data/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, buildInfo.PipelineId, buildInfo.VmSeqId)
	targetDir := constant.DockerDataDir
	if buildInfo.Workspace != "" {
		dataDir = buildInfo.Workspace
		targetDir = buildInfo.Workspace
	}
	if err := systemutil.MkDir(dataDir); err != nil {
		return nil, err
	}
	args = append(args, "--mount", fmt.Sprintf("type=bind,source=%s,target=%s", dataDir, targetDir))

	logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, buildInfo.BuildId, buildInfo.VmSeqId)
	if err := systemutil.MkDir(logsDir); err != nil {
		return nil, err
	}
	args = append(args, "--mount", fmt.Sprintf("type=bind,source=%s,target=%s", logsDir, job_docker.DockerLogDir))
	return args, nil
}

func parseContainerEnv(dockerBuildInfo *api.ThirdPartyDockerBuildInfo) []string {
	var vars []string
	vars = append(vars, "devops_project_id="+config.GAgentConfig.ProjectId)
	vars = append(vars, "devops_agent_id="+dockerBuildInfo.AgentId)
	vars = append(vars, "devops_agent_secret_key="+dockerBuildInfo.SecretKey)
	vars = append(vars, "devops_gateway="+config.GetGateWay())
	vars = append(vars, "agent_build_env=DOCKER")
	if hasJdk17Dir() {
		vars = append(vars, "DEVOPS_AGENT_JDK_8_PATH="+(targetJre8Dir+"/bin/java"))
		vars = append(vars, "DEVOPS_AGENT_JDK_17_PATH="+(targetJreDir+"/bin/java"))
	}
	if envvars.GApiEnvVars != nil {
		userEnvs := envvars.GApiEnvVars.GetAll()
		for k, v := range userEnvs {
			vars = append(vars, fmt.Sprintf("%s=%s", k, v))
		}
	}
	return vars
}

func dockerDaemonServerOS(ctx context.Context, runner *dockercli.Runner) (string, error) {
	serverOS, err := runner.ServerOS(ctx)
	if err != nil || strings.TrimSpace(serverOS) == "" {
		return runtime.GOOS, err
	}
	return strings.TrimSpace(serverOS), nil
}

func hasJdk17Dir() bool {
	if config.GAgentConfig == nil || config.GAgentConfig.Jdk17DirPath == "" {
		return false
	}
	info, err := os.Stat(config.GAgentConfig.Jdk17DirPath)
	return err == nil && info.IsDir()
}

func buildDebugContainerArgs(containerName, image string, debugInfo *api.ImageDebug) ([]string, error) {
	userArgs, err := job_docker.BuildUserDockerArgs(debugInfo.Options)
	if err != nil {
		return nil, err
	}
	mountArgs, err := parseDebugContainerMountArgs(debugInfo)
	if err != nil {
		return nil, err
	}
	args := []string{"--name", containerName}
	args = append(args, userArgs...)
	if !job_docker.HasCustomNetwork(debugInfo.Options) {
		args = append(args, "--network", "bridge")
	}
	for _, e := range parseDebugContainerEnv() {
		args = append(args, "-e", e)
	}
	if v, ok := envs.FetchEnv(constant.DevopsAgentDockerCapAdd); ok && strings.TrimSpace(v) != "" {
		args = append(args, "--cap-add", strings.TrimSpace(v))
	}
	args = append(args, mountArgs...)
	args = append(args, "--entrypoint", "/bin/sh", image, "-c", debugEntryPointCmd)
	return args, nil
}

func parseDebugContainerMountArgs(debugInfo *api.ImageDebug) ([]string, error) {
	var args []string
	if config.GAgentConfig.JdkDirPath != "" {
		args = append(args, "--mount", fmt.Sprintf("type=bind,source=%s,target=%s,readonly", config.GAgentConfig.JdkDirPath, "/usr/local/jre"))
	}
	workDir := systemutil.GetWorkDir()
	dataDir := debugInfo.Workspace
	if dataDir == "" {
		dataDir = fmt.Sprintf("%s/%s/data/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, debugInfo.PipelineId, debugInfo.VmSeqId)
	}
	if err := systemutil.MkDir(dataDir); err != nil {
		return nil, err
	}
	args = append(args, "--mount", fmt.Sprintf("type=bind,source=%s,target=%s", dataDir, constant.DockerDataDir))
	logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, debugInfo.BuildId, debugInfo.VmSeqId)
	if err := systemutil.MkDir(logsDir); err != nil {
		return nil, err
	}
	args = append(args, "--mount", fmt.Sprintf("type=bind,source=%s,target=%s", logsDir, job_docker.DockerLogDir))
	return args, nil
}

func parseDebugContainerEnv() []string {
	return []string{
		"devops_project_id=" + config.GAgentConfig.ProjectId,
		"devops_gateway=" + config.GetGateWay(),
		"agent_build_env=DOCKER",
	}
}
