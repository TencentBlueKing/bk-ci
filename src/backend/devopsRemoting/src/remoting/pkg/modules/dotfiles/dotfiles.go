package dotfiles

import (
	"common/logs"
	"context"
	"fmt"
	"io"
	"io/fs"
	"os"
	"os/exec"
	"path/filepath"
	"remoting/pkg/config"
	"remoting/pkg/constant"
	"remoting/pkg/modules/git"
	"remoting/pkg/thirdpartapi"
	"strings"
	"syscall"
	"time"

	"github.com/pkg/errors"
)

func InstallDotfiles(ctx context.Context, cfg *config.Config, thirdApi *thirdpartapi.ThirdPartApi, childProcEnvvars []string) {
	repo := cfg.WorkSpace.DotfileRepo
	if repo == "" {
		logs.Warn("dotfile repo is null")
		return
	}

	var dotfilePath = filepath.Join(constant.RemotingUserHome, ".dotfiles")
	if _, err := os.Stat(dotfilePath); err == nil {
		// dotfile path exists already - nothing to do here
		return
	}

	prep := func(_ *config.Config, out io.Writer, name string, args ...string) *exec.Cmd {
		cmd := exec.Command(name, args...)
		cmd.Dir = constant.RemotingUserHome
		cmd.Env = childProcEnvvars
		cmd.SysProcAttr = &syscall.SysProcAttr{
			// TODO: 目前直接使用Root
			// 所有DevopsRemoting的子进程都用devopsRemoting这个账户
			// Credential: &syscall.Credential{
			// 	Uid: devopsRemotingUID,
			// 	Gid: devopsRemotingGID,
			// },
		}
		cmd.Stdout = out
		cmd.Stderr = out
		return cmd
	}

	err := func() (err error) {
		out, err := os.OpenFile(filepath.Join(constant.RemotingUserHome, ".dotfiles.log"), os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0644)
		if err != nil {
			return err
		}
		defer out.Close()

		defer func() {
			if err != nil {
				out.WriteString(fmt.Sprintf("# dotfile init failed: %s\n", err.Error()))
			}
		}()

		done := make(chan error, 1)
		go func() {
			authProvider := func() (username string, password string, err error) {
				cred, _, err := thirdApi.Server.GetUserGitCred(ctx, cfg.WorkSpace.WorkspaceId, cfg.WorkSpace.GitUsername)
				if err != nil {
					return
				}
				username = constant.GitOAuthUser
				password = cred
				return
			}
			client := &git.Client{
				AuthProvider: authProvider,
				AuthMethod:   git.BasicAuth,
				Location:     dotfilePath,
				RemoteURI:    repo,
			}
			done <- client.Clone(ctx)
			close(done)
		}()
		select {
		case err := <-done:
			if err != nil {
				return err
			}
		case <-time.After(120 * time.Second):
			return errors.Errorf("dotfiles repo clone did not finish within two minutes")
		}

		filepath.Walk(dotfilePath, func(_ string, _ os.FileInfo, err error) error {
			if err == nil {
				// TODO: 暂时使用root
				// err = os.Chown(name, remotingUID, remotingGID)
			}
			return err
		})

		var candidates = []string{
			"install.sh",
			"install",
			"bootstrap.sh",
			"bootstrap",
			"script/bootstrap",
			"setup.sh",
			"setup",
			"script/setup",
		}
		for _, c := range candidates {
			fn := filepath.Join(dotfilePath, c)
			stat, err := os.Stat(fn)
			if err != nil {
				_, _ = out.WriteString(fmt.Sprintf("# installation script candidate %s is not available\n", fn))
				continue
			}
			if stat.IsDir() {
				_, _ = out.WriteString(fmt.Sprintf("# installation script candidate %s is a directory\n", fn))
				continue
			}
			if stat.Mode()&0111 == 0 {
				_, _ = out.WriteString(fmt.Sprintf("# installation script candidate %s is not executable\n", fn))
				continue
			}

			_, _ = out.WriteString(fmt.Sprintf("# executing installation script candidate %s\n", fn))

			// 找到了可执行的则开始运行
			cmd := prep(cfg, out, "/bin/sh", "-c", "exec "+fn)
			err = cmd.Start()
			if err != nil {
				return err
			}
			done := make(chan error, 1)
			go func() {
				done <- cmd.Wait()
				close(done)
			}()

			select {
			case err = <-done:
				return err
			case <-time.After(120 * time.Second):
				cmd.Process.Kill()
				return errors.Errorf("installation process %s tool longer than 120 seconds", fn)
			}
		}

		// 没有找到候选安装脚本，则尝试符号链接这些东西
		err = filepath.Walk(dotfilePath, func(path string, info fs.FileInfo, err error) error {
			if strings.Contains(path, "/.git") {
				// 不要链接 .git 目录或其任何内容
				return nil
			}

			homeFN := filepath.Join(constant.RemotingUserHome, strings.TrimPrefix(path, dotfilePath))
			if _, err := os.Stat(homeFN); err == nil {
				return nil
			}

			if info.IsDir() {
				err = os.MkdirAll(homeFN, info.Mode().Perm())
				if err != nil {
					return err
				}
				return nil
			}

			// 向终端写一些反馈
			out.WriteString(fmt.Sprintf("# echo linking %s -> %s\n", path, homeFN))

			return os.Symlink(path, homeFN)
		})

		return nil
	}()
	if err != nil {
		// TODO: 安装失败应该告知用户
		logs.WithError(err).Warn("installing dotfiles failed")
	}
}
