package main

import (
	"context"
	"devopsRemoting/common/logs"
	"devopsRemoting/common/process"
	"devopsRemoting/src/pkg/remoting/config"
	"fmt"
	"io"
	"os"
	"os/exec"
	"os/signal"
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
			cfg, err := config.GetConfig()
			if err != nil {
				logs.WithError(err).Error("init command cannnot load config")
			}
			var (
				sigInput = make(chan os.Signal, 1)
			)
			signal.Notify(sigInput, os.Interrupt, syscall.SIGTERM)

			runPath, err := os.Executable()
			if err != nil {
				runPath = "/.devopsRemoting/devopsRemoting"
			}
			runCommand := exec.Command(runPath, "run")
			runCommand.Args[0] = "devopsRemoting"
			runCommand.Stdin = os.Stdin
			runCommand.Stdout = os.Stdout
			runCommand.Stderr = os.Stderr
			runCommand.Env = os.Environ()
			err = runCommand.Start()
			if err != nil {
				logs.WithError(err).Error("devopsRemoting run start error")
				return
			}

			done := make(chan struct{})
			go func() {
				defer close(done)

				err := runCommand.Wait()
				if err != nil && !(strings.Contains(err.Error(), "signal: interrupt") || strings.Contains(err.Error(), "no child processes")) {
					logs.WithError(err).Error("devopsRemoting run error")
					return
				}
			}()
			// start the reaper to clean up zombie processes
			reaper.Reap()

			select {
			case <-done:
				// devopsRemoting 全部在这里结束
				return
			case <-sigInput:
				// 收到终止信号后传递给devopsRemoting并等待他结束
				ctx, cancel := context.WithTimeout(context.Background(), cfg.WorkSpace.GetTerminationGracePeriod())
				defer cancel()
				slog := newShutdownLogger()
				defer slog.Close()
				slog.write("Shutting down all processes")

				terminationDone := make(chan struct{})
				go func() {
					defer close(terminationDone)
					slog.TerminateSync(ctx, runCommand.Process.Pid)
					terminateAllOtherProcesses(ctx, slog)
				}()
				// wait for either successful termination or the timeout
				select {
				case <-ctx.Done():
					// Time is up, but we give all the goroutines a bit more time to react to this.
					time.Sleep(time.Millisecond * 500)
				case <-terminationDone:
				}
				slog.write("Finished shutting down all processes.")
			}
		},
	}

	return cmd
}

func newShutdownLogger() shutdownLogger {
	file := "/data/landun/workspace/.devopsRemoting/devopsRemoting-termination.log"
	f, err := os.Create(file)
	if err != nil {
		logs.WithError(err).WithField("file", file).Error("Couldn't create shutdown log")
	}
	result := shutdownLoggerImpl{
		file:      f,
		startTime: time.Now(),
	}
	return &result
}

type shutdownLogger interface {
	write(s string)
	TerminateSync(ctx context.Context, pid int)
	io.Closer
}

type shutdownLoggerImpl struct {
	file      *os.File
	startTime time.Time
}

func (l *shutdownLoggerImpl) write(s string) {
	if l.file != nil {
		_, err := l.file.WriteString(fmt.Sprintf("[%s] %s \n", time.Since(l.startTime), s))
		if err != nil {
			logs.WithError(err).Error("couldn't write to log file")
		}
	} else {
		logs.Debug(s)
	}
}
func (l *shutdownLoggerImpl) Close() error {
	return l.file.Close()
}
func (l *shutdownLoggerImpl) TerminateSync(ctx context.Context, pid int) {
	proc, err := procfs.NewProc(pid)
	if err != nil {
		l.write(fmt.Sprintf("Couldn't obtain process information for PID %d.", pid))
		return
	}
	stat, err := proc.Stat()
	if err != nil {
		l.write(fmt.Sprintf("Couldn't obtain process information for PID %d.", pid))
	} else if stat.State == "Z" {
		return
	} else {
		l.write(fmt.Sprintf("Terminating process %s with PID %d (state: %s, cmdlind: %s).", stat.Comm, pid, stat.State, fmt.Sprint(proc.CmdLine())))
	}
	err = process.TerminateSync(ctx, pid)
	if err != nil {
		if err == process.ErrForceKilled {
			l.write("Terminating process didn't finish, but had to be force killed")
		} else {
			l.write(fmt.Sprintf("Terminating main process errored: %s", err))
		}
	}
}

// terminateAllProcesses 终止除我们的进程之外的所有进程，直到不再有进程或上下文被取消
// 在上下文取消时，任何仍在运行的进程都会收到一个 SIGKILL
func terminateAllOtherProcesses(ctx context.Context, slog shutdownLogger) {
	for {
		processes, err := procfs.AllProcs()
		if err != nil {
			logs.WithError(err).Error("Cannot list processes")
			slog.write(fmt.Sprintf("Cannot list processes: %s", err))
			return
		}
		// only one process (must be us)
		if len(processes) == 1 {
			return
		}
		// terminate all processes but ourself
		var wg sync.WaitGroup
		for _, proc := range processes {
			if proc.PID == os.Getpid() {
				continue
			}
			p := proc
			wg.Add(1)
			go func() {
				defer wg.Done()
				slog.TerminateSync(ctx, p.PID)
			}()
		}
		wg.Wait()
	}
}
