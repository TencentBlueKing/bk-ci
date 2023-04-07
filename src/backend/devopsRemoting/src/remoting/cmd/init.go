package main

import (
	"common/logs"
	"common/process"
	"context"
	"fmt"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"

	"github.com/prometheus/procfs"
	"github.com/ramr/go-reaper"
	"github.com/spf13/cobra"
)

const initDesc = `
init 用来初始化并执行DevopsRemoting服务进程

如：devopsRemoting init
`

func newCommandInit() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "init",
		Short: "init the DevopsRemoting",
		Long:  initDesc,
		Run: func(_ *cobra.Command, _ []string) {
			logs.Init(Service, Version, true, false)

			sigInput := make(chan os.Signal, 1)
			signal.Notify(sigInput, os.Interrupt, syscall.SIGTERM)

			cmd, err := runRemoting()
			if err != nil {
				logs.WithError(err).Error("devopsRemoting run start error")
				return
			}
			done := make(chan struct{})
			go func() {
				defer close(done)

				err := cmd.Wait()
				if err != nil && !(strings.Contains(err.Error(), "signal: interrupt") || strings.Contains(err.Error(), "no child processes")) {
					logs.WithError(err).Error("devopsRemoting run error")
					return
				}
			}()
			// 清理僵尸进程
			reaper.Reap()

			select {
			case <-done:
				// devopsRemoting 全部在这里结束
				return
			case <-sigInput:
				// 收到终止信号后传递给devopsRemoting并等待他结束
				logs.Debug("recevied siginput, terminal all process")
				ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
				defer cancel()
				shutDownLog := newShutdownLogger()
				defer shutDownLog.Close()
				shutDownLog.write("shut down all processes")

				terminationDone := make(chan struct{})
				go func() {
					defer close(terminationDone)
					shutDownLog.terminate(ctx, cmd.Process.Pid)
					terminateAllOtherProcesses(ctx, shutDownLog)
				}()

				select {
				case <-ctx.Done():
					// 多余提供一些时间给被终止进程
					time.Sleep(time.Millisecond * 500)
				case <-terminationDone:
				}
				shutDownLog.write("shut down all processes done")
			}
		},
	}

	return cmd
}

func runRemoting() (*exec.Cmd, error) {
	remoting, err := os.Executable()
	if err != nil {
		remoting = "/.devopsRemoting/devopsRemoting"
	}
	cmd := exec.Command(remoting, "run")
	cmd.Args[0] = "devopsRemoting"
	cmd.Stdin = os.Stdin
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Env = os.Environ()
	err = cmd.Start()
	if err != nil {
		return nil, err
	}

	return cmd, nil
}

func newShutdownLogger() *shutdownLogger {
	file := "/data/landun/workspace/.devopsRemoting/devopsRemoting-termination.log"
	os.MkdirAll(filepath.Dir(file), os.ModePerm)
	fs, err := os.Create(file)
	if err != nil {
		logs.WithError(err).WithField("file", file).Error("create shutdown log err")
	}
	result := shutdownLogger{
		file:      fs,
		startTime: time.Now(),
	}
	return &result
}

type shutdownLogger struct {
	file      *os.File
	startTime time.Time
}

func (s *shutdownLogger) write(content string) {
	if s.file != nil {
		_, err := s.file.WriteString(fmt.Sprintf("[%s] %s \n", time.Since(s.startTime), content))
		if err != nil {
			logs.WithError(err).Error("shutdownLogger write error")
		}
	} else {
		logs.Debug(content)
	}
}
func (s *shutdownLogger) Close() error {
	return s.file.Close()
}
func (s *shutdownLogger) terminate(ctx context.Context, pid int) {
	proc, err := procfs.NewProc(pid)
	if err != nil {
		s.write(fmt.Sprintf("can not get process info from pid %d.", pid))
		return
	}
	stat, err := proc.Stat()
	if err != nil {
		s.write(fmt.Sprintf("can ot get process info from for pid %d.", pid))
	} else if stat.State == "Z" {
		// 僵尸进程
		return
	} else {
		s.write(fmt.Sprintf("terminate process %s with pid %d (state: %s, cmdlind: %s). at %s", stat.Comm, pid, stat.State, fmt.Sprint(proc.CmdLine()), time.Now().Format("2006-01-02 15:04:05")))
	}
	err = process.Terminate(ctx, pid)
	if err != nil {
		if err == process.ErrKilled {
			s.write(fmt.Sprintf("terminate process %s with pid %d not finish killed", stat.Comm, pid))
		} else {
			s.write(fmt.Sprintf("terminate main process error: %s", err.Error()))
		}
	}
}

// terminateAllProcesses 终止除remoting的进程之外的所有进程，直到不再有进程或上下文被取消
// 在上下文取消时，任何仍在运行的进程都会收到一个 SIGKILL
func terminateAllOtherProcesses(ctx context.Context, shutDownLog *shutdownLogger) {
	for {
		processes, err := procfs.AllProcs()
		if err != nil {
			logs.WithError(err).Error("can not list processes")
			shutDownLog.write(fmt.Sprintf("can not list processes: %s", err))
			return
		}
		// 只有一个肯定是remoting
		if len(processes) == 1 {
			return
		}

		var wg sync.WaitGroup
		for _, proc := range processes {
			if proc.PID == os.Getpid() {
				continue
			}
			p := proc
			wg.Add(1)
			go func() {
				defer wg.Done()
				shutDownLog.terminate(ctx, p.PID)
			}()
		}
		wg.Wait()
	}
}
