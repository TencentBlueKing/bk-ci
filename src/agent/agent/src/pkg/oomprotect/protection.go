// Package oomprotect 管理启动时生效的 Linux OOM 保护。
// 此包不依赖 config/envs/job，daemon 和 agent 可以在初始化业务模块前共用它。
// -1000 只影响内核 OOM 选择，不保证内存分配成功，也不能抵御人工 SIGKILL。
package oomprotect

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

const EnvKey = "DEVOPS_AGENT_OOM_PROTECT"

var state struct {
	sync.RWMutex
	enabled bool
	err     error
}
var pauseUntil atomic.Int64

// Requested 优先读取平台上次下发的配置，其次读取本机环境（调用前先加载 .env）。
// 平台配置只持久化这个开关，重启 daemon/agent 后生效；不在构建中途改变继承策略。
// 配置损坏时返回错误并暂停接单，不能把“配置了保护”静默当成“保护已成功”。
func Requested(workDir string) (bool, error) {
	value := os.Getenv(EnvKey)
	data, err := os.ReadFile(filepath.Join(workDir, ".oom-protection-env.json"))
	if err == nil {
		var server map[string]string
		if err = json.Unmarshal(data, &server); err != nil {
			return true, err
		}
		if v, ok := server[EnvKey]; ok {
			value = v
		}
	} else if !os.IsNotExist(err) {
		return true, err
	}
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "", "false":
		return false, nil
	case "true":
		return true, nil
	default:
		return true, fmt.Errorf("%s must be true or false", EnvKey)
	}
}

func Init(workDir string) error {
	enabled, err := Requested(workDir)
	if runtime.GOOS != "linux" {
		if enabled {
			err = fmt.Errorf("%s is only supported on Linux", EnvKey)
		}
		enabled = false
	} else if enabled && err == nil {
		err = SetScore(os.Getpid(), -1000)
	} else if err == nil && os.Getenv("BK_CI_OOM_PROTECTED_PARENT") == "true" {
		// 只重启 agent 时，旧 daemon 仍可能受到保护；关闭功能必须解除这一继承。
		// 未由本功能设置的外部 systemd OOM 策略不在这里重置。
		err = SetScore(os.Getpid(), 0)
	}
	if enabled && err == nil {
		os.Setenv("BK_CI_OOM_PROTECTED_PARENT", "true")
	} else {
		os.Unsetenv("BK_CI_OOM_PROTECTED_PARENT")
	}
	state.Lock()
	state.enabled, state.err = enabled, err
	state.Unlock()
	return err
}

// UserCommand 给 Go 直接执行的用户脚本提供同样的边界。参数作为独立 argv 传递，
// 不拼接到 shell 程序中；只在子 shell 内解除继承，父 agent 的 -1000 保持不变。
func UserCommand(program string, args []string) (string, []string) {
	if !Enabled() {
		return program, args
	}
	reset := "printf '0' > /proc/self/oom_score_adj || exit 125; " +
		"IFS= read -r score < /proc/self/oom_score_adj || exit 125; " +
		"[ \"$score\" = 0 ] || exit 125; exec \"$@\""
	return "/bin/sh", append([]string{"-c", reset, "bk-ci-user-command", program}, args...)
}

func Enabled() bool { state.RLock(); defer state.RUnlock(); return state.enabled }

func Ready() error {
	state.RLock()
	defer state.RUnlock()
	if state.err != nil {
		return state.err
	}
	if state.enabled && time.Now().UnixNano() < pauseUntil.Load() {
		return fmt.Errorf("resource failure: new builds paused during recovery")
	}
	return nil
}

func Pause() {
	if Enabled() {
		pauseUntil.Store(time.Now().Add(30 * time.Second).UnixNano())
	}
}

// PersistServerEnv 只更新下一次启动配置；当前进程的 Enabled 状态保持不变。
// 删除平台变量也要持久化，避免重启后继续使用已经删除的旧配置。
func PersistServerEnv(workDir string, values map[string]string) (bool, error) {
	selected := make(map[string]string)
	if value, ok := values[EnvKey]; ok {
		selected[EnvKey] = value
	}
	path := filepath.Join(workDir, ".oom-protection-env.json")
	data, err := json.Marshal(selected)
	if err != nil {
		return false, err
	}
	old, err := os.ReadFile(path)
	if err == nil && string(old) == string(data) {
		return false, nil
	}
	if os.IsNotExist(err) && len(selected) == 0 {
		return false, nil
	}
	if err = WriteAtomic(path, data, 0600); err != nil {
		return false, err
	}
	return true, nil
}

// WriteAtomic 用同目录临时文件、fsync 和 rename 提交启动配置。
// 读取方只能看到完整文件；0600 配置权限避免普通用户修改启动策略。
func WriteAtomic(path string, data []byte, mode os.FileMode) error {
	f, err := os.CreateTemp(filepath.Dir(path), ".oom-tmp-")
	if err != nil {
		return err
	}
	name := f.Name()
	defer os.Remove(name)
	if err = f.Chmod(mode); err == nil {
		_, err = f.Write(data)
	}
	if err == nil {
		err = f.Sync()
	}
	closeErr := f.Close()
	if err != nil {
		return err
	}
	if closeErr != nil {
		return closeErr
	}
	if err = os.Rename(name, path); err != nil {
		return err
	}
	return syncDir(filepath.Dir(path))
}
