package terminal

import (
	"bytes"
	"common/logs"
	"common/process"
	"context"
	"fmt"
	"io"
	"os"
	"os/exec"
	"sync"
	"time"

	"github.com/creack/pty"
	"github.com/google/uuid"
	"github.com/pkg/errors"
	"golang.org/x/sync/errgroup"
	"golang.org/x/sys/unix"
)

func NewMux() *Mux {
	return &Mux{
		terms: make(map[string]*Term),
	}
}

// Mux 用来混合伪终端
type Mux struct {
	aliases []string
	terms   map[string]*Term
	mu      sync.RWMutex
}

// Start 启动一个伪终端并返回其别名
func (m *Mux) Start(cmd *exec.Cmd, options TermOptions) (alias string, err error) {
	m.mu.Lock()
	defer m.mu.Unlock()

	pty, err := pty.StartWithSize(cmd, options.Size)
	if err != nil {
		return "", errors.Errorf("cannot start PTY: %s", err.Error())
	}

	uid, err := uuid.NewRandom()
	if err != nil {
		return "", errors.Errorf("cannot produce alias: %s", err.Error())
	}
	alias = uid.String()

	term, err := newTerm(alias, pty, cmd, options)
	if err != nil {
		pty.Close()
		return "", err
	}
	m.aliases = append(m.aliases, alias)
	m.terms[alias] = term

	logs.WithField("alias", alias).WithField("cmd", cmd.Path).Info("started new terminal")

	go func() {
		term.waitErr = cmd.Wait()
		close(term.waitDone)
		_ = m.CloseTerminal(context.Background(), alias)
	}()

	return alias, nil
}

// Close 关闭所有终端。
// 当context被取消时强制杀死它的进程
func (m *Mux) Close(ctx context.Context) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	g := new(errgroup.Group)
	for term := range m.terms {
		k := term
		g.Go(func() error {
			cerr := m.doClose(ctx, k)
			if cerr != nil {
				logs.WithError(cerr).WithField("alias", k).Warn("cannot properly close terminal")
				return cerr
			}
			return nil
		})
	}
	return g.Wait()
}

// CloseTerminal 关闭终端并结束在其中运行的进程
func (m *Mux) CloseTerminal(ctx context.Context, alias string) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	return m.doClose(ctx, alias)
}

var NoTimeout time.Duration = 1<<63 - 1

// terminalBacklogSize 是我们将为每个终端存储在 RAM 中的输出字节数。
// 这个数字越大，UX越好，但是对资源的要求也越高。
// 现在我们假设每个工作区平均有五个终端，这会消耗 1MiB 的 RAM。
const terminalBacklogSize = 256 << 10

func newTerm(alias string, pty *os.File, cmd *exec.Cmd, options TermOptions) (*Term, error) {
	token, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	recorder, err := NewRingBuffer(terminalBacklogSize)
	if err != nil {
		return nil, err
	}

	timeout := options.ReadTimeout
	if timeout == 0 {
		timeout = NoTimeout
	}
	res := &Term{
		PTY:     pty,
		Command: cmd,
		Stdout: &multiWriter{
			timeout:   timeout,
			listener:  make(map[*multiWriterListener]struct{}),
			recorder:  recorder,
			logStdout: options.LogToStdout,
			logLabel:  alias,
		},
		annotations:  options.Annotations,
		defaultTitle: options.Title,

		StarterToken: token.String(),

		waitDone: make(chan struct{}),
	}
	if res.annotations == nil {
		res.annotations = make(map[string]string)
	}

	rawConn, err := pty.SyscallConn()
	if err != nil {
		return nil, err
	}

	err = rawConn.Control(func(fileFd uintptr) {
		res.fd = int(fileFd)
	})
	if err != nil {
		return nil, err
	}

	//nolint:errcheck
	go io.Copy(res.Stdout, pty)
	return res, nil
}

// doClose closes a terminal and ends the process that runs in it.
// First, the process receives SIGTERM and is given gracePeriod time
// to stop. If it still runs after that time, it receives SIGKILL.
//
// Callers are expected to hold mu.
func (m *Mux) doClose(ctx context.Context, alias string) error {
	term, ok := m.terms[alias]
	if !ok {
		return ErrNotFound
	}

	logs.WithField("alias", alias).Info("closing terminal")
	if term.Command.Process != nil {
		err := process.Terminate(ctx, term.Command.Process.Pid)
		if err != nil {
			logs.WithError(err).Errorf("cannot terminate process %s.", fmt.Sprint(term.Command.Args))
		}
	}

	err := term.Stdout.Close()
	if err != nil {
		logs.WithError(err).Warn("cannot close connection to terminal clients")
	}
	err = term.PTY.Close()
	if err != nil {
		logs.WithError(err).Warn("cannot close pseudo-terminal")
	}
	i := 0
	for i < len(m.aliases) && m.aliases[i] != alias {
		i++
	}
	if i != len(m.aliases) {
		m.aliases = append(m.aliases[:i], m.aliases[i+1:]...)
	}
	delete(m.terms, alias)

	return nil
}

func (m *Mux) Get(alias string) (*Term, bool) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	term, ok := m.terms[alias]
	return term, ok
}

// Term 伪终端
type Term struct {
	PTY          *os.File
	Command      *exec.Cmd
	StarterToken string

	mu           sync.RWMutex
	annotations  map[string]string
	defaultTitle string
	title        string

	Stdout *multiWriter

	waitErr  error
	waitDone chan struct{}

	fd int
}

func (term *Term) GetTitle() (string, error) {
	term.mu.RLock()
	title := term.title
	term.mu.RUnlock()
	if title != "" {
		return title, nil
	}
	var b bytes.Buffer
	defaultTitle := term.defaultTitle
	b.WriteString(defaultTitle)
	command, err := term.resolveForegroundCommand()
	if defaultTitle != "" && command != "" {
		b.WriteString(": ")
	}
	b.WriteString(command)
	return b.String(), err
}

func (term *Term) resolveForegroundCommand() (string, error) {
	pgrp, err := unix.IoctlGetInt(term.fd, unix.TIOCGPGRP)
	if err != nil {
		return "", err
	}
	content, err := os.ReadFile(fmt.Sprintf("/proc/%d/cmdline", pgrp))
	if err != nil {
		return "", err
	}
	end := bytes.Index(content, []byte{0})
	if end != -1 {
		content = content[:end]
	}
	start := bytes.LastIndex(content, []byte{os.PathSeparator})
	if start != -1 {
		content = content[(start + 1):]
	}
	return string(content), nil
}

// Wait waits for the terminal to exit and returns the resulted process state.
func (term *Term) Wait() (*os.ProcessState, error) {
	<-term.waitDone
	return term.Command.ProcessState, term.waitErr
}

var (
	// ErrNotFound means the terminal was not found.
	ErrNotFound = errors.New("not found")
	// ErrReadTimeout happens when a listener takes too long to read.
	ErrReadTimeout = errors.New("read timeout")
)

// multiWriter 增加了运行时监听逻辑的 io.MultiWriter
type multiWriter struct {
	timeout  time.Duration
	closed   bool
	mu       sync.RWMutex
	listener map[*multiWriterListener]struct{}
	// ring buffer to record last 256kb of pty output
	// new listener is initialized with the latest recodring first
	recorder *RingBuffer

	logStdout bool
	logLabel  string
}

// override writer interface
func (mw *multiWriter) Write(p []byte) (n int, err error) {
	mw.mu.Lock()
	defer mw.mu.Unlock()

	mw.recorder.Write(p)
	if mw.logStdout {
		logs.WithField("terminalOutput", true).WithField("label", mw.logLabel).Info(string(p))
	}

	for lstr := range mw.listener {
		if lstr.closed {
			continue
		}

		select {
		case lstr.cchan <- p:
		case <-time.After(lstr.timeout):
			lstr.CloseWithError(ErrReadTimeout)
		}

		select {
		case <-lstr.done:
		case <-time.After(lstr.timeout):
			lstr.CloseWithError(ErrReadTimeout)
		}
	}
	return len(p), nil
}

func (mw *multiWriter) Close() error {
	mw.mu.Lock()
	defer mw.mu.Unlock()

	mw.closed = true

	var err error
	for w := range mw.listener {
		cerr := w.Close()
		if cerr != nil {
			err = cerr
		}
	}
	return err
}

// TermListenOptions is a configuration to listen to the pseudo-terminal .
type TermListenOptions struct {
	// timeout after which a listener is dropped. Use 0 for default timeout.
	ReadTimeout time.Duration
}

var closedListener = io.NopCloser(closedTerminalListener{})

type closedTerminalListener struct{}

func (closedTerminalListener) Read(p []byte) (n int, err error) {
	return 0, io.EOF
}

// Listen listens in on the multi-writer stream.
func (mw *multiWriter) Listen() io.ReadCloser {
	return mw.ListenWithOptions(TermListenOptions{
		ReadTimeout: 0,
	})
}

// Listen listens in on the multi-writer stream with given options.
func (mw *multiWriter) ListenWithOptions(options TermListenOptions) io.ReadCloser {
	mw.mu.Lock()
	defer mw.mu.Unlock()

	if mw.closed {
		return closedListener
	}

	timeout := options.ReadTimeout
	if timeout == 0 {
		timeout = mw.timeout
	}
	r, w := io.Pipe()
	cchan, done, closeChan := make(chan []byte), make(chan struct{}, 1), make(chan struct{}, 1)
	res := &multiWriterListener{
		Reader:    r,
		cchan:     cchan,
		done:      done,
		closeChan: closeChan,
		timeout:   timeout,
	}

	recording := mw.recorder.Bytes()
	go func() {
		_, _ = w.Write(recording)

		// copy bytes from channel to writer.
		// Note: we close the writer independently of the write operation s.t. we don't
		//       block the closing because the write's blocking.
		for b := range cchan {
			n, err := w.Write(b)
			done <- struct{}{}
			if err == nil && n != len(b) {
				err = io.ErrShortWrite
			}
			if err != nil {
				_ = res.CloseWithError(err)
			}
		}
	}()
	go func() {
		// listener cleanup on close
		<-closeChan
		if res.closeErr != nil {
			logs.WithError(res.closeErr).Error("terminal listener droped out")
			w.CloseWithError(res.closeErr)
		} else {
			w.Close()
		}
		close(cchan)

		mw.mu.Lock()
		delete(mw.listener, res)
		mw.mu.Unlock()
	}()

	mw.listener[res] = struct{}{}

	return res
}

type multiWriterListener struct {
	io.Reader
	timeout time.Duration

	closed    bool
	once      sync.Once
	closeErr  error
	closeChan chan struct{}
	cchan     chan []byte
	done      chan struct{}
}

func (l *multiWriterListener) Close() error {
	return l.CloseWithError(nil)
}

func (l *multiWriterListener) CloseWithError(err error) error {
	l.once.Do(func() {
		if err != nil {
			l.closeErr = err
		}
		close(l.closeChan)
		l.closed = true

		// actual cleanup happens in a go routine started by Listen()
	})
	return nil
}

// TermOptions 伪终端配置
type TermOptions struct {
	// timeout after which a listener is dropped. Use 0 for no timeout.
	ReadTimeout time.Duration

	// Annotations are user-defined metadata that's attached to a terminal
	Annotations map[string]string

	// Size describes the terminal size.
	Size *pty.Winsize

	// Title describes the terminal title.
	Title string

	// LogToStdout forwards the terminal's stdout to devopsRemoting's stdout
	LogToStdout bool
}

// NoTimeout means that listener can block read forever
