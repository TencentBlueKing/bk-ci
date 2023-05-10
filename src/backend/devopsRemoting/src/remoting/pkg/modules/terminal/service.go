package terminal

import (
	"common/logs"
	"context"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"remoting/pkg/types"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/pkg/errors"
)

// NewMuxTerminalService creates a new terminal service.
func NewMuxTerminalService(m *Mux) *MuxTerminalService {
	shell := os.Getenv("SHELL")
	if shell == "" {
		shell = "/bin/bash"
	}
	return &MuxTerminalService{
		Mux:            m,
		DefaultWorkdir: "/data/landun/workspace",
		DefaultShell:   shell,
		Env:            os.Environ(),
	}
}

// MuxTerminalService 伪终端服务实现
type MuxTerminalService struct {
	Mux *Mux

	DefaultWorkdir string

	DefaultShell string
	Env          []string
	// TODO: 暂时使用root
	// DefaultCreds *syscall.Credential
}

func (srv *MuxTerminalService) Open(ctx context.Context) (*types.OpenTerminalResponse, error) {
	return srv.OpenWithOptions(ctx, TermOptions{
		ReadTimeout: 5 * time.Second,
	})
}

func (srv *MuxTerminalService) OpenWithOptions(ctx context.Context, options TermOptions) (*types.OpenTerminalResponse, error) {

	shell := srv.DefaultShell

	cmd := exec.Command(shell)
	// if srv.DefaultCreds != nil {
	// 	cmd.SysProcAttr = &syscall.SysProcAttr{
	// 		Credential: srv.DefaultCreds,
	// 	}
	// }

	cmd.Dir = srv.DefaultWorkdir
	// add describes Xterm with support for 256 colors enabled
	cmd.Env = append(srv.Env, "TERM=xterm-256color")

	alias, err := srv.Mux.Start(cmd, options)
	if err != nil {
		return nil, err
	}

	// starterToken is just relevant for the service, hence it's not exposed at the Start() call
	var starterToken string
	term := srv.Mux.terms[alias]
	if term != nil {
		starterToken = term.StarterToken
	}

	terminal, found := srv.get(alias)
	if !found {
		return nil, errors.Wrap(err, "terminal not found")
	}
	return &types.OpenTerminalResponse{
		Terminal:     terminal,
		StarterToken: starterToken,
	}, nil
}

func (srv *MuxTerminalService) get(alias string) (*types.Terminal, bool) {
	term, ok := srv.Mux.terms[alias]
	if !ok {
		return nil, false
	}

	var (
		pid int64
		cwd string
		err error
	)
	if proc := term.Command.Process; proc != nil {
		pid = int64(proc.Pid)
		cwd, err = filepath.EvalSymlinks(fmt.Sprintf("/proc/%d/cwd", pid))
		if err != nil {
			logs.WithField("pid", pid).WithError(err).Warn("unable to resolve terminal's current working dir")
			cwd = term.Command.Dir
		}
	}

	title, err := term.GetTitle()
	if err != nil {
		logs.WithField("pid", pid).WithError(err).Warn("unable to resolve terminal's title")
	}

	return &types.Terminal{
		Alias:          alias,
		Command:        term.Command.Args,
		Pid:            pid,
		InitialWorkdir: term.Command.Dir,
		CurrentWorkdir: cwd,
		Title:          title,
		Annotations:    term.annotations,
	}, true
}

func (srv *MuxTerminalService) List() []*types.Terminal {
	srv.Mux.mu.RLock()
	defer srv.Mux.mu.RUnlock()

	res := make([]*types.Terminal, 0, len(srv.Mux.terms))
	for _, alias := range srv.Mux.aliases {
		term, ok := srv.get(alias)
		if !ok {
			continue
		}
		res = append(res, term)
	}

	return res
}

func (srv *MuxTerminalService) Listen(c *gin.Context, alias string) error {
	srv.Mux.mu.RLock()
	term, ok := srv.Mux.terms[alias]
	srv.Mux.mu.RUnlock()
	if !ok {
		return errors.New("terminal not found")
	}
	stdout := term.Stdout.Listen()

	defer stdout.Close()

	logs.WithField("alias", alias).Infof("new terminal client")
	defer logs.WithField("alias", alias).Infof("terminal client left")

	errchan := make(chan error, 1)
	messages := make(chan *types.ListenTerminalResponse, 1)
	go func() {
		for {
			buf := make([]byte, 4096)
			n, err := stdout.Read(buf)
			if err == io.EOF {
				break
			}
			if err != nil {
				errchan <- err
				return
			}
			messages <- &types.ListenTerminalResponse{Output: &types.ListenTerminalResponseOutputData{Data: buf[:n]}}
		}

		state, err := term.Wait()
		if err != nil {
			errchan <- err
			return
		}

		messages <- &types.ListenTerminalResponse{Output: &types.ListenTerminalResponseOutputExitCode{ExitCode: int32(state.ExitCode())}}
		errchan <- io.EOF
	}()
	go func() {
		title, _ := term.GetTitle()
		messages <- &types.ListenTerminalResponse{Output: &types.ListenTerminalResponseOutputTitle{Title: title}}

		t := time.NewTicker(200 * time.Millisecond)
		defer t.Stop()
		for {
			select {
			case <-c.Done():
				return
			case <-t.C:
				newTitle, _ := term.GetTitle()
				if title == newTitle {
					continue
				}
				title = newTitle
				messages <- &types.ListenTerminalResponse{Output: &types.ListenTerminalResponseOutputTitle{Title: title}}
			}
		}
	}()
	var newErr error = nil
	c.Stream(func(_ io.Writer) bool {
		for {
			var err error
			select {
			case message := <-messages:
				c.SSEvent("message", message)
				return true
			case err = <-errchan:
			case <-c.Done():
				newErr = c.Err()
				return false
			}
			if err == io.EOF {
				// EOF isn't really an error here
				return false
			}
			if err != nil {
				newErr = err
				return false
			}
		}
	})
	return newErr
}

func (srv *MuxTerminalService) Write(ctx context.Context, alias string, in []byte) (*types.WriteTerminalResponse, error) {
	srv.Mux.mu.RLock()
	term, ok := srv.Mux.terms[alias]
	srv.Mux.mu.RUnlock()
	if !ok {
		return nil, errors.New("terminal not found")
	}

	n, err := term.PTY.Write(in)
	if err != nil {
		return nil, err
	}
	return &types.WriteTerminalResponse{BytesWritten: uint32(n)}, nil
}

func (srv *MuxTerminalService) Close(ctx context.Context, alias string) error {
	err := srv.Mux.CloseTerminal(ctx, alias)
	if err == ErrNotFound {
		return errors.New("terminal not found")
	}
	if err != nil {
		return errors.Wrap(err, "termainal close error")
	}

	return nil
}
