package git

import (
	"bytes"
	"common/logs"
	"context"
	"fmt"
	"os"
	"os/exec"
	"remoting/pkg/config"
	"remoting/pkg/constant"
	"remoting/pkg/modules/user"
	"strconv"
	"strings"

	"github.com/pkg/errors"
)

// ConfigGit 配置Git
func ConfigGit(cfg *config.Config, childProcEnvvars []string) {
	gitcache := "cache"
	if cfg.WorkSpace.DebugEnable {
		gitcache = "store"
	}
	settings := [][]string{
		{"push.default", "simple"},
		{"alias.lg", "log --color --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit"},
		{"credential.helper", gitcache},
		{"safe.directory", "*"},
	}
	if cfg.WorkSpace.GitUsername != "" {
		settings = append(settings, []string{"user.name", cfg.WorkSpace.GitUsername})
	}
	if cfg.WorkSpace.GitEmail != "" {
		settings = append(settings, []string{"user.email", cfg.WorkSpace.GitEmail})
	}

	for _, s := range settings {
		cmd := exec.Command("git", append([]string{"config", "--global"}, s...)...)
		cmd = user.RunAsDevopsRemotingUser(cmd)
		cmd.Env = childProcEnvvars
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		err := cmd.Run()
		if err != nil {
			logs.WithError(err).Warn("git config error")
		}
	}
}

// ConfigGitCred 配置Git凭据
func ConfigGitCred(_ *config.Config, host, cred string, childProcEnvvars []string) error {
	cmd := exec.Command("git", append([]string{"credential", "approve"})...)
	cmd = user.RunAsDevopsRemotingUser(cmd)
	cmd.Env = childProcEnvvars
	inReader := bytes.NewReader([]byte(fmt.Sprintf("protocol=https\nhost=%s\nusername=%s\npassword=%s\n", host, constant.GitOAuthUser, cred)))
	cmd.Stdin = inReader
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err := cmd.Run()
	if err != nil {
		return errors.Wrap(err, "config git cred error")
	}
	return nil
}

func IsShallowRepository(rootDir string, env []string) bool {
	cmd := user.RunAsDevopsRemotingUser(exec.Command("git", "rev-parse", "--is-shallow-repository"))
	cmd.Env = env
	cmd.Dir = rootDir
	out, err := cmd.CombinedOutput()
	if err != nil {
		logs.WithError(err).Error("unexpected error checking if git repository is shallow")
		return true
	}

	isShallow, err := strconv.ParseBool(strings.TrimSpace(string(out)))
	if err != nil {
		logs.WithError(err).WithField("input", string(out)).Error("unexpected error parsing bool")
		return true
	}

	return isShallow
}

// AuthMethod 克隆时使用的认证方式
type AuthMethod string

const (
	// NoAuth 在克隆期间禁用身份验证
	NoAuth AuthMethod = ""

	// BasicAuth 在克隆期间使用 HTTP 基本身份验证
	BasicAuth AuthMethod = "basic-auth"
)

// AuthProvider 提供访问 Git 存储库的身份验证
type AuthProvider func() (username string, password string, err error)

// Client 一个Git配置，基于它我们可以执行git
type Client struct {
	AuthProvider AuthProvider
	AuthMethod   AuthMethod

	// Location 我们将在其中工作的文件系统中的路径（Git 可执行文件的 CWD）
	Location string

	// RemoteURI Git远程源
	RemoteURI string

	// Config git clone 可能用到的配置
	Config map[string]string
}

// Clone git Clone
func (c *Client) Clone(ctx context.Context) (err error) {
	err = os.MkdirAll(c.Location, 0775)
	if err != nil {
		logs.WithError(err).Error("cannot create clone location")
	}

	args := []string{"--depth=1", "--shallow-submodules", c.RemoteURI}

	for key, value := range c.Config {
		args = append(args, "--config")
		args = append(args, strings.TrimSpace(key)+"="+strings.TrimSpace(value))
	}

	args = append(args, ".")

	return c.Git(ctx, "clone", args...)
}

// Git 执行Git
func (c *Client) Git(ctx context.Context, subcommand string, args ...string) (err error) {
	_, err = c.GitWithOutput(ctx, nil, subcommand, args...)
	if err != nil {
		return err
	}
	return nil
}

// GitWithOutput 启动 git 并返回进程的标准输出。 这个函数在 git 启动后返回，不是在它完成之后。 一旦返回的阅读器返回 io.EOF，命令就完成了
func (c *Client) GitWithOutput(ctx context.Context, ignoreErr *string, subcommand string, args ...string) (out []byte, err error) {
	fullArgs := make([]string, 0)
	env := make([]string, 0)
	if c.AuthMethod == BasicAuth {
		if c.AuthProvider == nil {
			return nil, errors.Errorf("basic-auth method requires an auth provider")
		}

		fullArgs = append(fullArgs, "-c", "credential.helper=/bin/sh -c \"echo username=$GIT_AUTH_USER; echo password=$GIT_AUTH_PASSWORD\"")

		user, pwd, err := c.AuthProvider()
		if err != nil {
			return nil, err
		}
		env = append(env, fmt.Sprintf("GIT_AUTH_USER=%s", user))
		env = append(env, fmt.Sprintf("GIT_AUTH_PASSWORD=%s", pwd))
	}

	// TODO: 暂时使用Root
	env = append(env, "HOME="+constant.RemotingUserHome)

	fullArgs = append(fullArgs, subcommand)
	fullArgs = append(fullArgs, args...)

	env = append(env, fmt.Sprintf("PATH=%s", os.Getenv("PATH")))
	if os.Getenv("http_proxy") != "" {
		env = append(env, fmt.Sprintf("http_proxy=%s", os.Getenv("http_proxy")))
	}
	if os.Getenv("https_proxy") != "" {
		env = append(env, fmt.Sprintf("https_proxy=%s", os.Getenv("https_proxy")))
	}
	if v := os.Getenv("GIT_SSL_CAPATH"); v != "" {
		env = append(env, fmt.Sprintf("GIT_SSL_CAPATH=%s", v))
	}

	if v := os.Getenv("GIT_SSL_CAINFO"); v != "" {
		env = append(env, fmt.Sprintf("GIT_SSL_CAINFO=%s", v))
	}

	cmdName := "git"
	cmd := exec.Command(cmdName, fullArgs...)
	cmd.Dir = c.Location
	cmd.Env = env

	res, err := cmd.CombinedOutput()
	if err != nil {
		if strings.Contains(err.Error(), "no child process") {
			return res, nil
		}

		return nil, OpFailedError{
			Args:       args,
			ExecErr:    err,
			Output:     string(res),
			Subcommand: subcommand,
		}
	}

	return res, nil
}

// 如果操作失败，GitWithOutput 会返回 OpFailedError
// 例如 以非零退出代码返回。
type OpFailedError struct {
	Subcommand string
	Args       []string
	ExecErr    error
	Output     string
}

func (e OpFailedError) Error() string {
	return fmt.Sprintf("git %s %s failed (%v): %v", e.Subcommand, strings.Join(e.Args, " "), e.ExecErr, e.Output)
}
