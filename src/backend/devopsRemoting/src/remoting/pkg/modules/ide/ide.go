package ide

import (
	"common/logs"
	commonTypes "common/types"
	"common/util/fileutil"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"remoting/pkg/config"
	"remoting/pkg/constant"
	"remoting/pkg/dropwriter"
	"remoting/pkg/service"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"syscall"
	"time"

	"github.com/pkg/errors"
)

const (
	DesktopIDEPort = 24000
)

type IDEKind int64

const (
	WebIDE IDEKind = iota
	DesktopIDE
)

func (s IDEKind) String() string {
	switch s {
	case WebIDE:
		return "web"
	case DesktopIDE:
		return "desktop"
	}
	return "unknown"
}

type ideStatus int

const (
	statusNeverRan ideStatus = iota
	statusShouldRun
	statusShouldShutdown
)

const (
	timeBudgetIDEShutdown = 15 * time.Second
)

func StartAndWatchIDE(
	ctx context.Context,
	cfg *config.Config,
	ideConfig *config.IDEConfig,
	childProcEnvvars []string,
	wg *sync.WaitGroup,
	cstate *service.InMemoryContentState,
	ideReady *service.IdeReadyState,
	ide IDEKind,
	devfileSrv *config.DevfileConfigService,
) {
	defer wg.Done()
	defer logs.Debug("startAndWatchIDE shutdown", logs.String("ide", ide.String()))

	// 等到工作空间准备好
	<-cstate.ContentReady()

	// 等待devfile就绪，目前只需要workspaceFolder 这个字段，内置ide需要，ssh不需要
	var devfile *commonTypes.Devfile
	if ide == WebIDE {
		devfile = <-devfileSrv.Observe(ctx)
	} else {
		devfile = nil
	}
	if devfile == nil && ide == WebIDE {
		// devfile 不存在目前不能影响ide正常打开
		logs.Warn("webIde devfile is null")
	}

	// 对于ssh插件因为工作空间可能清空的问题，需要主动copy下插件
	if ide == DesktopIDE && cfg.WorkSpace.WorkspaceFirstCreate == "true" {
		exclude := []string{
			filepath.Join(constant.RemotingUserHome, ".vscode-server", "extensions", ".obsolete"),
		}
		logs.Debugf("copy vscode ssh extensions exclude %+v", exclude)
		if err := fileutil.CopyDir(
			filepath.Join(constant.RemotingUserHome, ".vscode-server", "extensions"),
			filepath.Join(cfg.WorkSpace.WorkspaceRootPath, ".vscode-server", "extensions"),
			[]string{},
		); err != nil {
			logs.Error("copy vscode ssh extensions fail", logs.Err(err))
		}
	}

	ideStatus := statusNeverRan

	var (
		cmd        *exec.Cmd
		ideStopped chan struct{}
	)
loop:
	for {
		if ideStatus == statusShouldShutdown {
			break
		}

		ideStopped = make(chan struct{}, 1)
		cmd = prepareIDELaunch(cfg, ideConfig, childProcEnvvars, devfile)
		launchIDE(cfg, ideConfig, cmd, ideStopped, ideReady, &ideStatus, ide)

		select {
		case <-ideStopped:
			// 杀掉在同一个进程组的所有进程
			_ = syscall.Kill(-1*cmd.Process.Pid, syscall.SIGKILL)
			// ide停止了稍后重启（以防 IDE 根本没有启动）
			if ideStatus == statusShouldShutdown {
				break loop
			}
			time.Sleep(1 * time.Second)
		case <-ctx.Done():
			// 被要求停止
			ideStatus = statusShouldShutdown
			if cmd == nil || cmd.Process == nil {
				logs.Error("cmd or cmd.Process is nil, cannot send SIGTERM signal ", logs.String("ide", ide.String()))
			} else {
				_ = cmd.Process.Signal(syscall.SIGTERM)
			}
			break loop
		}
	}

	logs.Info("IDE DevopsRemoting loop ended - waiting for IDE to come down", logs.String("ide", ide.String()))
	select {
	case <-ideStopped:
		logs.Info("IDE has been stopped in time", logs.String("ide", ide.String()))
		return
	case <-time.After(timeBudgetIDEShutdown):
		logs.Error("IDE did not stop in time - sending SIGKILL", logs.String("ide", ide.String()))
		if cmd == nil || cmd.Process == nil {
			logs.Error("cmd or cmd.Process is nil, cannot send SIGKILL", logs.String("ide", ide.String()))
		} else {
			_ = cmd.Process.Signal(syscall.SIGKILL)
		}
	}
}

func prepareIDELaunch(cfg *config.Config, ideConfig *config.IDEConfig, childProcEnvvars []string, devfile *commonTypes.Devfile) *exec.Cmd {
	args := ideConfig.EntrypointArgs

	defaultOpen := cfg.WorkSpace.GitRepoRootPath
	if devfile != nil && devfile.WorkspaceFolder != "" {
		defaultOpen = filepath.Join(cfg.WorkSpace.GitRepoRootPath, devfile.WorkspaceFolder)
	}

	for i := range args {
		// TODO: web版默认打开的位置，看后续是否可以使用插件代替
		args[i] = strings.ReplaceAll(args[i], "{DEFAULT_OPEN_FOLDER}", defaultOpen)
		args[i] = strings.ReplaceAll(args[i], "{IDEPORT}", strconv.Itoa(cfg.WorkSpace.IDEPort))
		args[i] = strings.ReplaceAll(args[i], "{DESKTOPIDEPORT}", strconv.Itoa(DesktopIDEPort))
	}
	logs.Info("preparing IDE launch", logs.Strings("args", args), logs.String("entrypoint", ideConfig.Entrypoint))

	cmd := exec.Command(ideConfig.Entrypoint, args...)
	cmd.SysProcAttr = &syscall.SysProcAttr{
		// 我们需要子进程运行在它自己的进程组中，便于暂停和恢复
		// IDE 和它的子进程
		Setpgid:   true,
		Pdeathsig: syscall.SIGKILL,

		// TODO: 目前直接使用Root
		// 所有DevopsRemoting的子进程都用devopsRemoting这个账户
		// Credential: &syscall.Credential{
		// 	Uid: devopsRemotingUID,
		// 	Gid: devopsRemotingGID,
		// },
	}
	cmd.Env = childProcEnvvars

	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	if lrr := cfg.IDELogRateLimit(ideConfig); lrr > 0 {
		limit := int64(lrr)
		cmd.Stdout = dropwriter.Writer(cmd.Stdout, dropwriter.NewBucket(limit*1024*3, limit*1024))
		cmd.Stderr = dropwriter.Writer(cmd.Stderr, dropwriter.NewBucket(limit*1024*3, limit*1024))
		logs.Info("rate limiting IDE log output", logs.Int64("limit_kb_per_sec", limit))
	}

	return cmd
}

var (
	errSignalTerminated = errors.New("signal: terminated")
)

func launchIDE(cfg *config.Config, ideConfig *config.IDEConfig, cmd *exec.Cmd, ideStopped chan struct{}, ideReady *service.IdeReadyState, s *ideStatus, ide IDEKind) {
	go func() {
		// prepareIDELaunch 设置 了Pdeathsig
		// 在 Linux 上，pdeathsig 会在线程死亡时杀死子进程，
		// 不是当进程结束时。 runtime.LockOSThread 确保只要
		// 因为这个函数正在执行，所以操作系统线程仍然存在
		// 参见 https://github.com/golang/go/issues/27505#issuecomment-713706104
		runtime.LockOSThread()
		defer runtime.UnlockOSThread()

		logs.Info("start launchIDE")
		err := cmd.Start()
		if err != nil {
			logs.Error("IDE failed to start", logs.String("ide", ide.String()), logs.Err(err))

			return
		}
		s = func() *ideStatus { i := statusShouldRun; return &i }()

		go func() {
			IDEStatus := runIDEReadinessProbe(cfg, ideConfig, ide)
			ideReady.Set(true, IDEStatus)
		}()

		err = cmd.Wait()
		if err != nil {
			if errSignalTerminated.Error() != err.Error() {
				logs.Warn("IDE was stopped", logs.String("ide", ide.String()), logs.Err(err))
			}

			ideWasReady, _ := ideReady.Get()
			if !ideWasReady {
				logs.Error("IDE failed to start", logs.String("ide", ide.String()), logs.Err(err))
				return
			}
		}

		ideReady.Set(false, nil)
		close(ideStopped)
	}()
}

func runIDEReadinessProbe(cfg *config.Config, ideConfig *config.IDEConfig, ide IDEKind) (desktopIDEStatus *service.DesktopIDEStatus) {
	defer logs.Info("IDE is ready", logs.String("ide", ide.String()))

	defaultIfEmpty := func(value, defaultValue string) string {
		if len(value) == 0 {
			return defaultValue
		}
		return value
	}

	defaultIfZero := func(value, defaultValue int) int {
		if value == 0 {
			return defaultValue
		}
		return value
	}

	defaultProbePort := cfg.WorkSpace.IDEPort
	if ide == DesktopIDE {
		defaultProbePort = DesktopIDEPort
	}

	switch ideConfig.ReadinessProbe.Type {
	case config.ReadinessProcessProbe:
		return

	case config.ReadinessHTTPProbe:
		var (
			schema = defaultIfEmpty(ideConfig.ReadinessProbe.HTTPProbe.Schema, "http")
			host   = defaultIfEmpty(ideConfig.ReadinessProbe.HTTPProbe.Host, "localhost")
			port   = defaultIfZero(ideConfig.ReadinessProbe.HTTPProbe.Port, defaultProbePort)
			url    = fmt.Sprintf("%s://%s:%d/%s", schema, host, port, strings.TrimPrefix(ideConfig.ReadinessProbe.HTTPProbe.Path, "/"))
		)

		t0 := time.Now()

		var body []byte
		for range time.Tick(250 * time.Millisecond) {
			var err error
			body, err = ideStatusRequest(url)
			if err != nil {
				logs.Debug("Error running IDE readiness probe", logs.Err(err))
				continue
			}

			break
		}

		logs.Infof("IDE readiness took %.3f seconds", time.Since(t0).Seconds(), logs.String("ide", ide.String()))

		if ide != DesktopIDE {
			return
		}

		err := json.Unmarshal(body, &desktopIDEStatus)
		if err != nil {
			logs.Debug("Error parsing JSON body from IDE status probe.", logs.String("ide", ide.String()), logs.String("body", string(body)), logs.Err(err))
			return
		}

		logs.Infof("Desktop IDE status: %s", desktopIDEStatus, logs.String("ide", ide.String()))
		return
	}

	return
}

func ideStatusRequest(url string) ([]byte, error) {
	client := http.Client{Timeout: 1 * time.Second}

	resp, err := client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, errors.Errorf("IDE readiness probe came back with non-200 status code (%v)", resp.StatusCode)
	}

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	return body, nil
}

func exit(exitCode int) {
	logs.Debugf("devopsRemoting exit", logs.Int("exitCode", exitCode))
	os.Exit(exitCode)
}

func WaitForIde(parent context.Context, ideReady *service.IdeReadyState, desktopIdeReady *service.IdeReadyState, timeout time.Duration) {
	if ideReady == nil {
		return
	}
	ctx, cancel := context.WithTimeout(parent, timeout)
	defer cancel()
	select {
	case <-ctx.Done():
		return
	case <-ideReady.Wait():
	}

	if desktopIdeReady == nil {
		return
	}
	select {
	case <-ctx.Done():
		return
	case <-desktopIdeReady.Wait():
	}
}
