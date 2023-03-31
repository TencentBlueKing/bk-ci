package service

import (
	"common/logs"
	"context"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
	"remoting/pkg/config"
	"remoting/pkg/constant"
	"remoting/pkg/modules/git"
	"remoting/pkg/modules/user"
	"remoting/pkg/thirdpartapi"
	"strings"
	"sync"
	"time"
)

// ContentState 工作区状态同步
type ContentState interface {
	MarkContentReady()
	ContentReady() <-chan struct{}
	ContentSource() (ok bool)
}

// NewInMemoryContentState creates a new InMemoryContentState.
func NewInMemoryContentState(gitRepoPath string) *InMemoryContentState {
	return &InMemoryContentState{
		gitRepoPath:      gitRepoPath,
		contentReadyChan: make(chan struct{}),
	}
}

// InMemoryContentState 内存中的工作空间内容状态同步内存实现
type InMemoryContentState struct {
	gitRepoPath      string
	contentReadyChan chan struct{}
}

// MarkContentReady 标记工作空间可用，只能在主线程使用
func (state *InMemoryContentState) MarkContentReady() {
	close(state.contentReadyChan)
}

// ContentReady 返回一个chan，工作空间可用时当前通道会关闭
func (state *InMemoryContentState) ContentReady() <-chan struct{} {
	return state.contentReadyChan
}

// ContentSource 返回工作空间是否可用，会被chan阻塞
func (state *InMemoryContentState) ContentSource() (ok bool) {
	select {
	case <-state.contentReadyChan:
	default:
		return false
	}
	return true
}

// TODO: 目前只写简易版，未来根据daemon实现补充
// 初始化工作区并确认是否完成
func StartContentInit(
	ctx context.Context,
	cfg *config.Config,
	wg *sync.WaitGroup,
	cst ContentState,
	thirdApi *thirdpartapi.ThirdPartApi,
	childProcEnvvars []string,
) {
	defer wg.Done()
	defer logs.Info("devopsRemoting: workspace content available")

	var err error = nil
	defer func() {
		if err == nil {
			return
		}
		if f, err := os.Stat("/dev"); os.IsNotExist(err) || !f.IsDir() {
			if err = os.MkdirAll("/dev", os.ModePerm); err != nil {
				logs.WithError(err).Error("cannot write termination log")
				return
			}
		}
		ferr := os.WriteFile("/dev/termination-log", []byte(err.Error()), 0o644)
		if ferr != nil {
			logs.WithError(err).Error("cannot write termination log")
			return
		}

		logs.WithError(err).Fatal("content initialization failed")
	}()

	err = func() error {
		// 拉代码
		// 配置Git fetch，同时配置Git密匙
		// TODO: 未来有多个git仓库时应该fetch多次
		start := time.Now()
		// TODO: 未来看时间长短提前目前将请求凭据的放在这里
		cred, host, err := thirdApi.Server.GetUserGitCred(ctx, cfg.WorkSpace.WorkspaceId, cfg.WorkSpace.GitUsername)
		logs.Debugf("get user git cred took %v", time.Since(start))
		if err != nil {
			logs.WithError(err).Errorf("request user %s git cred error", cfg.WorkSpace.GitUsername)
			return err
		}
		if err = git.ConfigGitCred(cfg, host, cred, childProcEnvvars); err != nil {
			logs.WithError(err).Errorf("config git cred error")
			return err
		}

		// 判断是否已经有拉过代码的文件夹了，不重复拉取
		_, err = os.Stat(cfg.WorkSpace.GitRepoRootPath)
		if err == nil || os.IsExist(err) {
			files, err := ioutil.ReadDir(cfg.WorkSpace.GitRepoRootPath)
			if err != nil {
				logs.WithError(err).Errorf("search git repo dir files error")
				return err
			}
			if len(files) != 0 {
				logs.Debug("exist git repo no clone")
				return err
			}
		} else {
			if !os.IsNotExist(err) {
				logs.WithError(err).Error("stat user repo dir error")
				return err
			}
		}

		var cloneCommands []string
		if cfg.WorkSpace.GitRemoteRepoBranch != "" {
			cloneCommands = []string{"clone", "-b", cfg.WorkSpace.GitRemoteRepoBranch, cfg.WorkSpace.GitRemoteRepoUrl}
		} else {
			cloneCommands = []string{"clone", cfg.WorkSpace.GitRemoteRepoUrl}
		}

		cmd := user.RunAsDevopsRemotingUser(exec.Command("git", cloneCommands...))
		cmd.Env = childProcEnvvars
		cmd.Dir = cfg.WorkSpace.WorkspaceRootPath
		res, err := cmd.CombinedOutput()
		if err != nil && !strings.Contains(err.Error(), "no child process") {
			err = git.OpFailedError{
				Args:       []string{cfg.WorkSpace.GitRemoteRepoUrl},
				ExecErr:    err,
				Output:     string(res),
				Subcommand: "clone",
			}
			return err
		}

		logs.Debugf("clone local repository took %v", time.Since(start))

		// 拉完代码后再添加可能的default文件
		if cfg.WorkSpace.DevopsRemotingYaml == "" {
			os.MkdirAll(filepath.Join(cfg.WorkSpace.GitRepoRootPath, constant.DevfileDir), os.ModePerm)
			if err = thirdApi.Server.DownloadDefaultDevfile(
				context.Background(),
				filepath.Join(cfg.WorkSpace.GitRepoRootPath, constant.DevfileDir, constant.DefaultDevFileName),
				cfg.WorkSpace.WorkspaceId,
			); err != nil {
				logs.WithError(err).Error("download default devfile error")
				// 没有devfile不影响主流程
				err = nil
			} else {
				cfg.WorkSpace.DevopsRemotingYaml = constant.DefaultDevFileName
				logs.Debug("use default devfile success")
			}
		}

		return nil
	}()
	if err != nil {
		return
	}

	// 拉完代码后fetch下
	go func() {
		cmd := user.RunAsDevopsRemotingUser(exec.Command("git", "fetch", "--tags"))
		cmd.Env = childProcEnvvars
		cmd.Dir = cfg.WorkSpace.GitRepoRootPath
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		err = cmd.Run()
		if err != nil {
			logs.WithError(err).Error("git fetch error")
		}
	}()

	logs.Info("devopsRemoting: workspace content init finished")
	cst.MarkContentReady()
}
