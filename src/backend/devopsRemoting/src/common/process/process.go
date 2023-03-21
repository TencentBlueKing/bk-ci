package process

import (
	"context"
	"errors"
	"os"

	"golang.org/x/sys/unix"
)

// IsNotChildProcess 检查命令是否返回错误
// 执行是与没有子进程运行相关的错误
func IsNotChildProcess(err error) bool {
	if err == nil {
		return false
	}

	return (err.Error() == "wait: no child processes" || err.Error() == "waitid: no child processes")
}

var ErrForceKilled = errors.New("Process didn't terminate, so we sent SIGKILL")

// TerminateSync 向给定进程发送 SIGTERM 并在进程终止或上下文被取消时返回。
// 当上下文被取消时，此函数向进程发送一个 SIGKILL 并立即返回 ErrForceKilled。
func TerminateSync(ctx context.Context, pid int) error {
	process, err := os.FindProcess(pid)
	if err != nil { // never happens on UNIX
		return err
	}
	err = process.Signal(unix.SIGTERM)
	if err != nil {
		if err == os.ErrProcessDone {
			return nil
		}
		return err
	}
	terminated := make(chan error, 1)
	go func() {
		_, err := process.Wait()
		terminated <- err
	}()
	select {
	case err := <-terminated:
		return err
	case <-ctx.Done():
		err = process.Kill()
		if err != nil {
			return err
		}
		return ErrForceKilled
	}
}
