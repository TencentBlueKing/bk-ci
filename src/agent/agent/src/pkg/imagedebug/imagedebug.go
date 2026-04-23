package imagedebug

import (
	"context"
	"fmt"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/sync/errgroup"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/dockercli"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job_docker"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

const (
	entryPointCmd         = "while true; do sleep 5m; done"
	ImageDebugMaxHoldHour = 24
	DebugContainerHeader  = "bkcidebug-"
)

var imageDebugLogs *logrus.Entry

func Init() {
	// 区分模块初始化日志，方便查询
	imageDebugLogs = logs.Logs.WithField("module", "ImageDebug")
}

type OnceChan[T any] struct {
	C    chan T
	once sync.Once
}

func NewOnceChan[T any]() *OnceChan[T] {
	return &OnceChan[T]{C: make(chan T)}
}

func (c *OnceChan[T]) SafeClose() {
	c.once.Do(func() {
		close(c.C)
	})
}

func DoImageDebug(debugInfo *api.ImageDebug) {
	// 容器已经准备就绪，会向其中写入containerid
	containerReady := NewOnceChan[string]()
	// 登录调试结束
	debugDone := NewOnceChan[struct{}]()
	// 登录调试最大等待结束时间
	c, cancel := context.WithTimeout(context.Background(), ImageDebugMaxHoldHour*time.Hour)
	defer cancel()

	group, ctx := errgroup.WithContext(c)

	filedLog := imageDebugLogs.WithField("buildId", debugInfo.BuildId).WithField("vmseqId", debugInfo.VmSeqId).WithField("userId", debugInfo.DebugUserId)

	// 新建docker容器
	group.Go(func() error {
		err := CreateDebugContainer(ctx, debugInfo, containerReady, debugDone)
		filedLog.Info("CreateDebugContainer done")
		return err
	})

	// 启动登录调试
	group.Go(func() error {
		err := CreateExecServer(ctx, debugInfo, containerReady, debugDone)
		filedLog.Info("CreateExecServer done")
		return err
	})

	// 轮训任务状态
	group.Go(func() error {
		err := checkDebugStatus(ctx, debugInfo.DebugId, debugDone)
		filedLog.Info("checkDebugStatus done")
		return err
	})

	// 上报结束并附带错误信息
	if err := group.Wait(); err != nil {
		_, err2 := api.FinishDockerDebug(debugInfo, false, "", &api.Error{
			ErrorType:    api.DockerImageDebugErrorEnum.Type,
			ErrorMessage: i18n.Localize("DockerImageDebugError", map[string]interface{}{"err": err.Error()}),
			ErrorCode:    api.DockerImageDebugErrorEnum.Code,
		})
		if err2 != nil {
			filedLog.WithError(err).Error("post image debug url error")
		}
	}
}

func checkDebugStatus(
	ctx context.Context,
	debugId int64,
	debugDone *OnceChan[struct{}],
) error {
	for {
		select {
		case <-debugDone.C:
			return nil
		case <-ctx.Done():
			return nil
		default:
			time.Sleep(5 * time.Minute)
			result, err := api.FetchDockerDebugStatus(debugId)
			if err != nil {
				imageDebugLogs.WithError(err).Error("request FetchDockerDebugStatus error")
				continue
			}
			if result.IsNotOk() {
				imageDebugLogs.Error("FetchDockerDebugStatus info failed, message", result.Message)
				continue
			}

			if result.Data == nil {
				// 数据为空说明任务不存在直接结束
				imageDebugLogs.Error("FetchDockerDebugStatus data nil")
				debugDone.SafeClose()
				return nil
			}

			status := new(string)
			err = util.ParseJsonToData(result.Data, status)
			if err != nil {
				imageDebugLogs.WithError(err).Error("FetchDockerDebugStatus parse data error")
				continue
			}

			if status == nil {
				continue
			}

			if *status == "FAILURE" {
				// 任务失败则直接结束
				imageDebugLogs.Error("FetchDockerDebugStatus FAILURE")
				debugDone.SafeClose()
				return nil
			}

			if *status == "SUCCESS" {
				return nil
			}
		}
	}
}

func CreateDebugContainer(
	ctx context.Context,
	debugInfo *api.ImageDebug,
	containerReady *OnceChan[string],
	debugDone *OnceChan[struct{}],
) error {
	runner := dockercli.NewRunner(systemutil.GetWorkDir(), func(format string, args ...interface{}) {
		imageDebugLogs.Infof(format, args...)
	})

	// 先判断本地是否存在已经运行的容器
	containerId, ok, err := checkLoclRunningContainer(ctx, runner, debugInfo.BuildId, debugInfo.VmSeqId)
	if err != nil {
		return errors.Wrap(err, "check local running container error")
	}
	if ok {
		imageDebugLogs.Infof("use local exist container %s", containerId)
		containerReady.C <- containerId
		containerReady.SafeClose()
		return nil
	}

	if debugInfo.Credential.ErrMsg != "" {
		imageDebugLogs.Error("get docker cred error ", debugInfo.Credential.ErrMsg)
		return errors.New(i18n.Localize("GetDockerCertError", map[string]interface{}{"err": debugInfo.Credential.ErrMsg}))
	}

	imageName := strings.TrimSpace(debugInfo.Image)

	// 判断本地是否已经有镜像了
	localExist, err := runner.ImageExists(ctx, imageName)
	if err != nil {
		imageDebugLogs.Error("list docker images error ", err)
		return errors.New(i18n.Localize("GetDockerImagesError", map[string]interface{}{"err": err}))
	}
	imageStr := strings.TrimPrefix(strings.TrimPrefix(imageName, "http://"), "https://")

	imageStrSub := strings.Split(imageStr, ":")
	isLatest := false
	// mirrors.tencent.com/ruotiantang/image-test:latest
	// 长度为2说明第二个就是tag
	if len(imageStrSub) == 2 && imageStrSub[1] == "latest" {
		isLatest = true
	} else if len(imageStr) == 1 {
		// 等于1说明没填tag按照docker的规则默认会去拉取最新的为 latest
		isLatest = true
	}

	if job_docker.IfPullImage(localExist, isLatest, api.ImagePullPolicyIfNotPresent.String()) {
		pullOut, err := runner.PullImage(ctx, imageName, debugInfo.Credential.User, debugInfo.Credential.Password)
		if err != nil {
			imageDebugLogs.WithError(err).Errorf("pull new image %s error", imageName)
			return errors.New(i18n.Localize("PullImageError", map[string]interface{}{"name": imageName, "err": err.Error()}))
		}
		if strings.TrimSpace(pullOut) != "" {
			imageDebugLogs.Info(pullOut)
		}
	}

	// 创建docker构建机运行准备空间，拉取docker构建机初始化文件
	tmpDir := fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), job_docker.LocalDockerBuildTmpDirName)
	err = systemutil.MkDir(tmpDir)
	if err != nil {
		errMsg := i18n.Localize("CreateDockerTmpDirError", map[string]interface{}{"err": err.Error()})
		imageDebugLogs.Error(errMsg)
		return errors.New(errMsg)
	}

	// 创建容器
	containerName := fmt.Sprintf("%s%s-%s-%s", DebugContainerHeader, debugInfo.BuildId, debugInfo.VmSeqId, util.RandStringRunes(8))
	createArgs, err := buildDebugCreateArgs(containerName, imageStr, debugInfo)
	if err != nil {
		errMsg := i18n.Localize("CreateContainerError", map[string]interface{}{"name": containerName, "err": err.Error()})
		imageDebugLogs.Error(err)
		return errors.New(errMsg)
	}
	creatID, err := runner.CreateContainer(ctx, createArgs)
	if err != nil {
		imageDebugLogs.WithError(err).Errorf("create container %s error", containerName)
		return errors.New(i18n.Localize("CreateContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}))
	}

	defer func() {
		// 登录调试结束或者登录调试错误都关闭容器，为了保证停止和删除不复用context
		if err := runner.StopContainer(context.Background(), creatID); err != nil {
			imageDebugLogs.Warnf("stop container %s error %s", creatID, err.Error())
		}
		if err = runner.RemoveContainer(context.Background(), creatID); err != nil {
			imageDebugLogs.Errorf("remove container %s error %s", creatID, err.Error())
		}
		containerReady.SafeClose()
	}()

	// 启动容器
	if err := runner.StartContainer(ctx, creatID); err != nil {
		imageDebugLogs.Error(fmt.Sprintf("start container %s error ", creatID), err)
		return errors.New(i18n.Localize("StartContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}))
	}

	// 等待容器结束，处理错误信息
	// docker 容器已经准备就绪了
	containerReady.C <- creatID

	waitDone := make(chan struct{})
	var waitCode int64
	var waitErr error
	go func() {
		defer close(waitDone)
		waitCode, waitErr = runner.WaitContainer(ctx, creatID)
	}()

	select {
	case <-waitDone:
		if waitErr != nil {
			imageDebugLogs.Error(fmt.Sprintf("wait container %s over error ", creatID), waitErr)
			return errors.New(i18n.Localize("WaitContainerError", map[string]interface{}{"name": containerName, "err": waitErr.Error()}))
		}
		if waitCode != 0 {
			imageDebugLogs.Warn(fmt.Sprintf("wait container %s over status not 0, exit code %d", creatID, waitCode))
			containerLog, err := runner.ContainerLogs(context.Background(), creatID)
			if err != nil {
				imageDebugLogs.Error("get container error", err)
			}
			return errors.New(i18n.Localize("ContainerExitCodeNotZero", map[string]interface{}{"name": containerName, "code": waitCode, "msg": containerLog}))
		}
	case <-debugDone.C:
		return nil
	case <-ctx.Done():
		return nil
	}
	return nil
}

func checkLoclRunningContainer(ctx context.Context, runner *dockercli.Runner, buildId string, vmId string) (string, bool, error) {
	conList, err := runner.ListContainers(ctx, false)
	if err != nil {
		return "", false, err
	}

	findName := fmt.Sprintf("dispatch-%s-%s-", buildId, vmId)
	for _, c := range conList {
		if strings.Contains(c.Name, findName) {
			return c.ID, true, nil
		}
	}

	return "", false, nil
}

func buildDebugCreateArgs(containerName, image string, debugInfo *api.ImageDebug) ([]string, error) {
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
	for _, e := range parseDebugContainerEnv(debugInfo) {
		args = append(args, "-e", e)
	}
	if v, ok := envs.FetchEnv(constant.DevopsAgentDockerCapAdd); ok && strings.TrimSpace(v) != "" {
		args = append(args, "--cap-add", strings.TrimSpace(v))
	}
	args = append(args, mountArgs...)
	args = append(args, "--entrypoint", "/bin/sh", image, "-c", entryPointCmd)
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
	err := systemutil.MkDir(dataDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local data dir %s error", dataDir)
	}
	args = append(args, "--mount", fmt.Sprintf("type=bind,source=%s,target=%s", dataDir, constant.DockerDataDir))

	logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, debugInfo.BuildId, debugInfo.VmSeqId)
	err = systemutil.MkDir(logsDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local logs dir %s error", logsDir)
	}
	args = append(args, "--mount", fmt.Sprintf("type=bind,source=%s,target=%s", logsDir, job_docker.DockerLogDir))
	return args, nil
}

func parseDebugContainerEnv(_ *api.ImageDebug) []string {
	return []string{
		"devops_project_id=" + config.GAgentConfig.ProjectId,
		"devops_gateway=" + config.GetGateWay(),
		"agent_build_env=DOCKER",
	}
}

func CreateExecServer(
	ctx context.Context,
	debugInfo *api.ImageDebug,
	containerReady *OnceChan[string],
	debugDone *OnceChan[struct{}],
) error {
	port, err := NewPortAllocator().AllocateNodePort()
	if err != nil {
		imageDebugLogs.WithError(err).Error("allocator port error")
		return err
	}

	conf := &ConsoleProxyConfig{
		Address:        "0.0.0.0",
		Port:           port,
		ServCert:       &CertConfig{},
		DockerEndpoint: dockercli.RuntimeSocketFromBinary(dockercli.RuntimeBinary()),
		Privilege:      false,
		Cmd:            []string{"/bin/bash"},
		Tty:            true,
		Ips:            []string{},
		IsAuth:         false,
		IsOneSeesion:   true,
	}

	backend := NewManager(conf, debugDone)

	err = backend.Start()
	if err != nil {
		imageDebugLogs.Errorf("exec server start manager error %s", err.Error())
		return err
	}

	imageDebugLogs.WithField("port", port).Info("debug server start")
	errChan := make(chan error)
	server := InitRouter(ctx, backend, conf, errChan)
	defer func() {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		server.Shutdown(ctx)
		imageDebugLogs.WithField("port", port).Info("debug server stop")
		cancel()
	}()

	// 等待容器启动后创建登录调试链接
	containerId, ok := <-containerReady.C
	if !ok {
		imageDebugLogs.Warn("CreateExecServer containerReady chan is closed")
		return nil
	}

	// 检查下httpserver 是否有报错有报错直接退出，没有则继续
	if len(errChan) > 0 {
		err = <-errChan
		imageDebugLogs.WithError(err).Error("start exec server error")
		return errors.Wrap(err, "start exec server error")
	}

	exec, err := backend.CreateExecNoHttp(&WebSocketConfig{
		ContainerID: containerId,
		User:        "root",
		Cmd:         conf.Cmd,
	})

	url := fmt.Sprintf("ws://%s:%d/start_exec?exec_id=%s&container_id=%s", config.GAgentEnv.GetAgentIp(), conf.Port, exec.ID, containerId)

	// 上报结束并附带 url
	imageDebugLogs.Infof("ws url: %s", url)
	_, err = api.FinishDockerDebug(debugInfo, true, url, nil)
	if err != nil {
		imageDebugLogs.WithError(err).Error("post image debug url error")
	}

	select {
	case <-debugDone.C:
		return nil
	case <-ctx.Done():
		return nil
	}
}
