package process

import (
	"context"
	"errors"
	"os"

	"golang.org/x/sys/unix"
)

var ErrKilled = errors.New("process not term, kill")

// Terminate 向给定进程发送 SIGTERM 并在进程终止或上下文被取消时返回。
// 当上下文被取消时，此函数向进程发送一个 SIGKILL 并立即返回 ErrKilled
func Terminate(ctx context.Context, pid int) error {
	process, err := os.FindProcess(pid)
	if err != nil {
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
		if err := process.Kill(); err != nil {
			return err
		}
		return ErrKilled
	}
}
