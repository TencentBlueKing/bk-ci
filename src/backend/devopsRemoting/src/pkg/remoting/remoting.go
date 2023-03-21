package remoting

import (
	"context"
	"devopsRemoting/common/logs"
	"devopsRemoting/src/pkg/remoting/api"
	"devopsRemoting/src/pkg/remoting/config"
	"devopsRemoting/src/pkg/remoting/constant"
	"devopsRemoting/src/pkg/remoting/modules/dotfiles"
	"devopsRemoting/src/pkg/remoting/modules/git"
	"devopsRemoting/src/pkg/remoting/modules/ide"
	"devopsRemoting/src/pkg/remoting/modules/ports"
	"devopsRemoting/src/pkg/remoting/modules/preci"
	"devopsRemoting/src/pkg/remoting/modules/ssh"
	"devopsRemoting/src/pkg/remoting/modules/terminal"
	"devopsRemoting/src/pkg/remoting/service"
	"devopsRemoting/src/pkg/remoting/thirdpartapi"
	"devopsRemoting/src/pkg/remoting/types"
	"fmt"
	"os"
	"os/signal"
	"path/filepath"

	"strings"
	"sync"
	"syscall"
	"time"
)

type ShutdownReason int16

// Run 启动devopsRemoting进程
func Run() {
	logs.Debug("run devopsremoting")

	remotingConfig, err := config.GetConfig()
	if err != nil {
		logs.WithError(err).Error("run command load config error")
		exit(1)
		return
	}

	var thirdApi = thirdpartapi.InitThirdpartApi(remotingConfig)

	// 当不存在指定的devfile时使用默认的，先只设置环境变量，方便后续子进程使用
	if remotingConfig.WorkSpace.DevopsRemotingYaml == "" {
		// 重新设置下环境变量
		os.Setenv("DEVOPS_REMOTING_YAML_NAME", filepath.Join(constant.DevfileDir, constant.DefaultDevFileName))
		logs.Debug("use default devfile")
	}

	// 拼接子进程环境变量
	childProcEnvvars := buildChildProcEnv(remotingConfig)

	// 配置GIT
	git.ConfigGit(remotingConfig, childProcEnvvars)

	tunneledPortsService := ports.NewTunneledPortsService(remotingConfig.WorkSpace.DebugEnable)
	_, err = tunneledPortsService.Tunnel(context.Background(),
		&ports.TunnelOptions{
			SkipIfExists: false,
		},
		&ports.PortTunnelDescription{
			LocalPort:  uint32(remotingConfig.Config.APIServerPort),
			TargetPort: uint32(remotingConfig.Config.APIServerPort),
			Visibility: types.TunnelVisiblityHost,
		},
		&ports.PortTunnelDescription{
			LocalPort:  uint32(remotingConfig.Config.SSHPort),
			TargetPort: uint32(remotingConfig.Config.SSHPort),
			Visibility: types.TunnelVisiblityHost,
		},
	)
	if err != nil {
		logs.WithError(err).Warn("cannot tunnel internal ports")
	}

	ctx, cancel := context.WithCancel(context.Background())

	// 内部应用端口
	internalPorts := []uint32{uint32(ide.DesktopIDEPort), uint32(remotingConfig.Config.APIServerPort), uint32(remotingConfig.Config.SSHPort)}
	if remotingConfig.IDE != nil {
		internalPorts = append(internalPorts, ide.DesktopIDEPort)
	}

	var (
		ideReady        *service.IdeReadyState = nil
		desktopIdeReady                        = &service.IdeReadyState{Cond: sync.NewCond(&sync.Mutex{})}

		cstate = service.NewInMemoryContentState(remotingConfig.WorkSpace.GitRepoRootPath)
	)
	if remotingConfig.IDE != nil {
		ideReady = &service.IdeReadyState{Cond: sync.NewCond(&sync.Mutex{})}
	}

	// 兼容使用默认devfile的情况，没有devfile的情况
	devfilepath := filepath.Join(remotingConfig.WorkSpace.GitRepoRootPath, remotingConfig.WorkSpace.DevopsRemotingYaml)
	if remotingConfig.WorkSpace.DevopsRemotingYaml == "" {
		devfilepath = filepath.Join(remotingConfig.WorkSpace.GitRepoRootPath, constant.DevfileDir, config.WathchEmpty)
	} else if remotingConfig.WorkSpace.DevopsRemotingYaml == constant.BlankDevfileName {
		devfilepath = ""
	}
	devfileService := config.NewDevfileConfigService(
		devfilepath,
		cstate.ContentReady(),
		logs.Logs,
	)
	go devfileService.Watch(ctx)

	portMgmt := ports.NewPortsManager(
		ports.NewRemotingExposedPorts(remotingConfig.WorkSpace.WorkspaceId, "", thirdApi),
		&ports.PollingServedPortsObserver{
			RefreshInterval: 2 * time.Second,
		},
		ports.NewConfigService(devfileService),
		tunneledPortsService,
		internalPorts...,
	)

	// 启动伪终端用来执行command
	termMux := terminal.NewMux()
	termMuxSrv := terminal.NewMuxTerminalService(termMux)
	termMuxSrv.DefaultWorkdir = remotingConfig.WorkSpace.GitRepoRootPath
	termMuxSrv.Env = childProcEnvvars

	commandManager := service.NewCommandManger(remotingConfig, termMuxSrv, cstate, devfileService)

	service.ApiService = &service.ApiServiceType{
		CommandManager: commandManager,
		TermSrv:        termMuxSrv,
		Ports:          portMgmt,
		SSH:            &ssh.SSHService{},
		Token:          service.InitTokenService(),
	}

	logs.Debug("start remote module process")

	// 在启动ide前先安装dotfiles，因为dotfiles中的脚本可能影响ide路径，这样方便排查问题
	dotfiles.InstallDotfiles(ctx, remotingConfig, thirdApi, childProcEnvvars)

	// 创建进程组，所有进程退出时才可以退出
	var ideWG sync.WaitGroup
	ideWG.Add(1)
	// 启动并监管IDE DESKTOP进程
	go ide.StartAndWatchIDE(ctx, remotingConfig, &remotingConfig.DesktopIDE, childProcEnvvars, &ideWG, cstate, desktopIdeReady, ide.DesktopIDE, devfileService)
	if remotingConfig.IDE != nil {
		ideWG.Add(1)
		go ide.StartAndWatchIDE(ctx, remotingConfig, remotingConfig.IDE, childProcEnvvars, &ideWG, cstate, ideReady, ide.WebIDE, devfileService)
	}

	var (
		wg sync.WaitGroup
	)
	wg.Add(1)
	go service.StartContentInit(ctx, remotingConfig, &wg, cstate, thirdApi, childProcEnvvars)
	wg.Add(1)
	go api.StartAPIServer(ctx, remotingConfig, &wg)
	wg.Add(1)
	go ssh.StartSSHServer(ctx, remotingConfig, &wg, childProcEnvvars)
	wg.Add(1)
	go preci.StartPreci(ctx, cstate, remotingConfig, service.ApiService.Token, childProcEnvvars)
	wg.Add(1)
	go commandManager.Run(ctx, &wg)
	wg.Add(1)
	go portMgmt.Run(ctx, &wg)

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
	select {
	case <-sigChan:
	}

	logs.Info("received SIGTERM (or shutdown) - tearing down")
	cancel()
	ideWG.Wait()

	wg.Wait()
}

// buildChildProcEnv 拼接子IDE进程需要的环境变量
func buildChildProcEnv(_ *config.Config) []string {
	envvars := os.Environ()
	// 拼接系统环境变量
	envs := make(map[string]string)
	for _, e := range envvars {
		segs := strings.SplitN(e, "=", 2)
		if len(segs) < 2 {
			logs.Printf("\"%s\" has invalid format, not including in IDE environment", e)
			continue
		}
		name, value := segs[0], segs[1]

		if isBlacklistedEnvvar(name) {
			continue
		}

		envs[name] = value
	}

	// TODO: 是否需要将一些私密的环境变量通过url在线获取，而非传入会更加安全

	// 通过修改基础环境变量模拟登录的过程
	// 通过参考busybox
	//   - https://github.com/mirror/busybox/blob/24198f652f10dca5603df7c704263358ca21f5ce/libbb/setup_environment.c#L32
	//   - https://github.com/mirror/busybox/blob/24198f652f10dca5603df7c704263358ca21f5ce/libbb/login.c#L140-L170
	envs["HOME"] = "/root"
	envs["USER"] = "root"

	var env, envn []string
	for name, value := range envs {
		logs.WithField("envvar", name).Debugf("passing environment variable to IDE")
		env = append(env, fmt.Sprintf("%s=%s", name, value))
		envn = append(envn, name)
	}

	logs.WithField("envvar", envn).Debugf("passing environment variables to IDE")

	return env
}

// isBlacklistedEnvvar 需要过滤掉不传递给子进程的环境变量
func isBlacklistedEnvvar(name string) bool {
	prefixBlacklist := []string{
		// 过滤掉kubernets注入的环境变量
		"KUBERNETES_SERVICE",
		"KUBERNETES_PORT",
		"   ",
	}
	for _, wep := range prefixBlacklist {
		if strings.HasPrefix(name, wep) {
			return true
		}
	}

	return false
}

func exit(exitCode int) {
	logs.WithField("exitCode", exitCode).Debugf("devopsRemoting exit")
	os.Exit(exitCode)
}
