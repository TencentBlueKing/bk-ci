package imagedebug

import (
	"context"
	"fmt"
	"io"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/job_docker"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/mount"
	"github.com/docker/docker/api/types/network"
	"github.com/docker/docker/client"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/sync/errgroup"
)

const (
	imageDebugIntervalInSeconds = 5
	entryPointCmd               = "while true; do sleep 5m; done"
	ImageDebugMaxHoldHour       = 24
	DebugContainerHeader        = "bkcidebug-"
)

var imageDebugLogs *logrus.Entry

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

func DoPullAndDebug() {
	if !systemutil.IsLinux() {
		return
	}

	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent imagedebug panic: ", err)
		}
	}()

	// 区分模块初始化日志，方便查询
	imageDebugLogs = logs.Logs.WithField("module", "ImageDebug")

	for {
		time.Sleep(imageDebugIntervalInSeconds * time.Second)

		if !config.GAgentConfig.EnableDockerBuild {
			continue
		}

		// 接受登录调试任务
		debugInfo, err := getDebugTask()
		if err != nil {
			imageDebugLogs.WithError(err).Warn("get image deubg failed, retry, err")
			continue
		}
		if debugInfo == nil {
			continue
		}

		// 启动登录调试
		go doImageDebug(debugInfo)
	}
}

// getDebugTask 从服务器认领要登录调试的信息
func getDebugTask() (*api.ImageDebug, error) {
	result, err := api.PullDockerDebugTask()
	if err != nil {
		return nil, err
	}

	if result.IsNotOk() {
		logs.Error("get debug info failed, message", result.Message)
		return nil, errors.New("get debug info failed")
	}

	if result.Data == nil {
		return nil, nil
	}

	debugInfo := new(api.ImageDebug)
	err = util.ParseJsonToData(result.Data, debugInfo)
	if err != nil {
		return nil, err
	}

	return debugInfo, nil
}

func doImageDebug(debugInfo *api.ImageDebug) {
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
	cli, err := client.NewClientWithOpts(client.FromEnv, client.WithAPIVersionNegotiation())
	if err != nil {
		imageDebugLogs.Error("create docker client error ", err)
		return errors.New(i18n.Localize("LinkDockerError", map[string]interface{}{"err": err}))
	}

	// 先判断本地是否存在已经运行的容器
	containerId, ok, err := checkLoclRunningContainer(ctx, cli, debugInfo.BuildId, debugInfo.VmSeqId)
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
	images, err := cli.ImageList(ctx, types.ImageListOptions{})
	if err != nil {
		imageDebugLogs.Error("list docker images error ", err)
		return errors.New(i18n.Localize("GetDockerImagesError", map[string]interface{}{"err": err}))
	}
	localExist := false
	imageStr := strings.TrimPrefix(strings.TrimPrefix(imageName, "http://"), "https://")
	for _, image := range images {
		for _, tagName := range image.RepoTags {
			if tagName == imageStr {
				localExist = true
			}
		}
	}

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
		auth, err := job_docker.GenerateDockerAuth(debugInfo.Credential.User, debugInfo.Credential.Password)
		if err != nil {
			imageDebugLogs.WithError(err).Errorf("pull new image generateDockerAuth %s error ", imageName)
			return errors.New(i18n.Localize("PullImageError", map[string]interface{}{"name": imageName, "err": err.Error()}))
		}
		reader, err := cli.ImagePull(ctx, imageName, types.ImagePullOptions{
			RegistryAuth: auth,
		})
		if err != nil {
			imageDebugLogs.Errorf("pull new image %s error %s", imageName, err.Error())
			return errors.New(i18n.Localize("PullImageError", map[string]interface{}{"name": imageName, "err": err.Error()}))
		}
		defer reader.Close()
		buf := new(strings.Builder)
		_, _ = io.Copy(buf, reader)
	}

	// 创建docker构建机运行准备空间，拉取docker构建机初始化文件
	tmpDir := fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), job_docker.LocalDockerBuildTmpDirName)
	err = systemutil.MkDir(tmpDir)
	if err != nil {
		errMsg := i18n.Localize("CreateDockerTmpDirError", map[string]interface{}{"err": err.Error()})
		imageDebugLogs.Error(errMsg)
		return errors.New(errMsg)
	}

	// 解析docker options
	dockerConfig, err := job_docker.ParseDockeroptions(cli, debugInfo.Options)
	if err != nil {
		imageDebugLogs.Error(err.Error())
		return err
	}

	// 创建容器
	containerName := fmt.Sprintf("%s%s-%s-%s", DebugContainerHeader, debugInfo.BuildId, debugInfo.VmSeqId, util.RandStringRunes(8))
	mounts, err := parseContainerMounts(debugInfo)
	if err != nil {
		errMsg := i18n.Localize("ReadDockerMountsError", map[string]interface{}{"err": err.Error()})
		imageDebugLogs.Error(err)
		return errors.New(errMsg)
	}

	var confg *container.Config
	var hostConfig *container.HostConfig
	var netConfig *network.NetworkingConfig
	if dockerConfig != nil {
		confg = dockerConfig.Config
		confg.Image = imageStr
		confg.Cmd = []string{}
		confg.Entrypoint = []string{"/bin/sh", "-c", entryPointCmd}
		confg.Env = parseContainerEnv(debugInfo)

		hostConfig = dockerConfig.HostConfig
		hostConfig.CapAdd = append(hostConfig.CapAdd, "SYS_PTRACE")
		hostConfig.Mounts = append(hostConfig.Mounts, mounts...)
		hostConfig.NetworkMode = container.NetworkMode("bridge")

		netConfig = dockerConfig.NetworkingConfig
	} else {
		confg = &container.Config{
			Image:      imageStr,
			Cmd:        []string{},
			Entrypoint: []string{"/bin/sh", "-c", entryPointCmd},
			Env:        parseContainerEnv(debugInfo),
		}

		hostConfig = &container.HostConfig{
			CapAdd:      []string{"SYS_PTRACE"},
			Mounts:      mounts,
			NetworkMode: container.NetworkMode("bridge"),
		}

		netConfig = nil
	}

	creatResp, err := cli.ContainerCreate(ctx, confg, hostConfig, netConfig, nil, containerName)
	if err != nil {
		imageDebugLogs.WithError(err).Errorf("create container %s error", containerName)
		return errors.New(i18n.Localize("CreateContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}))
	}

	defer func() {
		// 登录调试结束或者登录调试错误都关闭容器，为了保证停止和删除不复用context
		containerStopTimeout := 0
		if err := cli.ContainerStop(context.Background(), creatResp.ID, container.StopOptions{Timeout: &containerStopTimeout}); err != nil {
			imageDebugLogs.Warnf("stop container %s error %s", creatResp.ID, err.Error())
		}
		if err = cli.ContainerRemove(context.Background(), creatResp.ID, types.ContainerRemoveOptions{Force: true}); err != nil {
			imageDebugLogs.Errorf("remove container %s error %s", creatResp.ID, err.Error())
		}
		containerReady.SafeClose()
	}()

	// 启动容器
	if err := cli.ContainerStart(ctx, creatResp.ID, types.ContainerStartOptions{}); err != nil {
		imageDebugLogs.Error(fmt.Sprintf("start container %s error ", creatResp.ID), err)
		return errors.New(i18n.Localize("StartContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}))
	}

	// 等待容器结束，处理错误信息
	statusCh, errCh := cli.ContainerWait(ctx, creatResp.ID, container.WaitConditionNotRunning)

	// docker 容器已经准备就绪了
	containerReady.C <- creatResp.ID

	select {
	case err := <-errCh:
		if err != nil {
			imageDebugLogs.Error(fmt.Sprintf("wait container %s over error ", creatResp.ID), err)
			return errors.New(i18n.Localize("WaitContainerError", map[string]interface{}{"name": containerName, "err": err.Error()}))
		}
	case status := <-statusCh:
		if status.Error != nil {
			imageDebugLogs.Error(fmt.Sprintf("wait container %s over error ", creatResp.ID), status.Error)
			return errors.New(i18n.Localize("WaitContainerError", map[string]interface{}{"name": containerName, "err": status.Error.Message}))
		} else {
			if status.StatusCode != 0 {
				imageDebugLogs.Warn(fmt.Sprintf("wait container %s over status not 0, exit code %d", creatResp.ID, status.StatusCode))
				msg := ""

				// 这里可能就是docker最开始执行时报错，拿一下docker log，如果原本有docker.log 则容器日志上传为debug日志，否则上传为结束日志
				containerLogB, err := cli.ContainerLogs(context.Background(), creatResp.ID, types.ContainerLogsOptions{
					ShowStdout: true,
					ShowStderr: true,
				})
				var containerLog = ""
				if err == nil {
					buf := new(strings.Builder)
					_, err := io.Copy(buf, containerLogB)
					if err != nil {
						imageDebugLogs.Error("copy container error", err)
						containerLog = ""
					} else {
						containerLog = buf.String()
					}
				} else {
					imageDebugLogs.Error("get container error", err)
				}

				msg = containerLog
				return errors.New(i18n.Localize("ContainerExitCodeNotZero", map[string]interface{}{"name": containerName, "code": status.StatusCode, "msg": msg}))
			}
		}
	// 登录调试结束或者登录调试错误都关闭容器，为了保证停止和删除不复用context
	case <-debugDone.C:
		return nil
	case <-ctx.Done():
		return nil
	}
	return nil
}

func checkLoclRunningContainer(ctx context.Context, cli *client.Client, buildId string, vmId string) (string, bool, error) {
	conList, err := cli.ContainerList(ctx, types.ContainerListOptions{})
	if err != nil {
		return "", false, err
	}

	findName := fmt.Sprintf("dispatch-%s-%s-", buildId, vmId)
	for _, c := range conList {
		for _, n := range c.Names {
			if strings.Contains(n, findName) {
				return c.ID, true, nil
			}
		}
	}

	return "", false, nil
}

// parseContainerMounts 解析生成容器挂载内容
func parseContainerMounts(debugInfo *api.ImageDebug) ([]mount.Mount, error) {
	var mounts []mount.Mount

	// 默认绑定本机的java用来执行worker，因为仅支持linux容器所以仅限linux构建机绑定
	if systemutil.IsLinux() {
		javaDir := config.GAgentConfig.JdkDirPath
		mounts = append(mounts, mount.Mount{
			Type:     mount.TypeBind,
			Source:   javaDir,
			Target:   "/usr/local/jre",
			ReadOnly: true,
		})
	}

	workDir := systemutil.GetWorkDir()

	// 创建并挂载data和log
	// data目录优先选择用户自定的工作空间
	dataDir := ""
	if debugInfo.Workspace == "" {
		dataDir = fmt.Sprintf("%s/%s/data/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, debugInfo.PipelineId, debugInfo.VmSeqId)
	} else {
		dataDir = debugInfo.Workspace
	}
	err := systemutil.MkDir(dataDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local data dir %s error", dataDir)
	}
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   dataDir,
		Target:   job_docker.DockerDataDir,
		ReadOnly: false,
	})

	logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, job_docker.LocalDockerWorkSpaceDirName, debugInfo.BuildId, debugInfo.VmSeqId)
	err = systemutil.MkDir(logsDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local logs dir %s error", logsDir)
	}
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   logsDir,
		Target:   job_docker.DockerLogDir,
		ReadOnly: false,
	})

	return mounts, nil
}

// parseContainerEnv 解析生成容器环境变量
func parseContainerEnv(_ *api.ImageDebug) []string {
	var envs []string

	// 默认传入环境变量用来构建
	envs = append(envs, "devops_project_id="+config.GAgentConfig.ProjectId)
	envs = append(envs, "devops_gateway="+config.GetGateWay())
	// 通过环境变量区分agent docker
	envs = append(envs, "agent_build_env=DOCKER")

	return envs
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
		DockerEndpoint: "unix:///var/run/docker.sock",
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

	url := fmt.Sprintf("ws://%s:%d/start_exec?exec_id=%s&container_id=%s", config.GAgentEnv.AgentIp, conf.Port, exec.ID, containerId)

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
