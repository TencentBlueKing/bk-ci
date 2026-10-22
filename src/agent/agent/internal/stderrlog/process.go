package stderrlog

import (
	"errors"
	"io"
	"os"
	"os/exec"
	"time"
)

const (
	TailSize     = 64 * 1024
	drainTimeout = time.Second
)

// Report describes stderr captured up to EOF or the post-exit drain deadline.
type Report struct {
	Tail          string
	TotalBytes    uint64
	ReadError     error
	DrainTimedOut bool
}

// Process owns the stderr pipe for one child. Wait must be called exactly once
// after a successful Start, just like exec.Cmd.Wait.
type Process struct {
	cmd    *exec.Cmd
	reader *os.File
	done   chan struct{}
	tail   tailBuffer
	report Report // Written by drain; read only after done is closed.
}

// Start replaces cmd.Stderr and begins draining before starting the child.
// A nil sink retains only the diagnostic tail. Other Cmd fields are unchanged.
func Start(cmd *exec.Cmd, sink *Sink) (*Process, error) {
	reader, writer, err := os.Pipe()
	if err != nil {
		return nil, err
	}
	p := &Process{
		cmd:    cmd,
		reader: reader,
		done:   make(chan struct{}),
	}
	// Passing an *os.File avoids exec.Cmd owning/closing the read end or
	// waiting for an internal copy goroutine. Unlike StderrPipe, this lets us
	// reap the child first, then drain with a bound on inherited descriptors.
	cmd.Stderr = writer
	go p.drain(sink)
	err = cmd.Start()
	// Only the child may retain the write end, including on Start failure.
	_ = writer.Close()
	if err != nil {
		_ = reader.Close()
		<-p.done
		return nil, err
	}
	return p, nil
}

// Wait reaps the child and drains its buffered stderr before returning. A
// descendant may inherit stderr and keep the pipe open after the child exits;
// allow a short grace period, then close our reader so no drain goroutine leaks.
// This timeout is not a timeout for cmd.Start or for a still-running child.
func (p *Process) Wait() (Report, error) {
	err := p.cmd.Wait()
	timer := time.NewTimer(drainTimeout)
	defer timer.Stop()
	timedOut := false
	select {
	case <-p.done:
	case <-timer.C:
		timedOut = true
		_ = p.reader.Close()
		<-p.done
	}
	report := p.report
	report.Tail = p.tail.String()
	report.DrainTimedOut = timedOut
	return report, err
}

func (p *Process) drain(sink *Sink) {
	defer close(p.done)
	defer p.reader.Close()
	buf := make([]byte, chunkSize)
	for {
		n, err := p.reader.Read(buf)
		if n > 0 {
			p.tail.Write(buf[:n])
			p.report.TotalBytes += uint64(n)
			if sink != nil {
				_, _ = sink.Write(buf[:n])
			}
		}
		if err != nil {
			if err != io.EOF && !errors.Is(err, os.ErrClosed) {
				p.report.ReadError = err
			}
			return
		}
	}
}

// tailBuffer retains bytes, not lines: a long line or binary stderr cannot
// overflow Scanner's token limit or allocate an unbounded crash buffer.
// Only the drain goroutine writes it; Wait reads it after drain has finished.
type tailBuffer struct {
	buf  [TailSize]byte
	next int
	size int
}

func (b *tailBuffer) Write(p []byte) {
	if len(p) >= len(b.buf) {
		copy(b.buf[:], p[len(p)-len(b.buf):])
		b.next, b.size = 0, len(b.buf)
		return
	}
	n := copy(b.buf[b.next:], p)
	copy(b.buf[:], p[n:])
	b.next = (b.next + len(p)) % len(b.buf)
	b.size += len(p)
	if b.size > len(b.buf) {
		b.size = len(b.buf)
	}
}

func (b *tailBuffer) String() string {
	out := make([]byte, b.size)
	start := (b.next - b.size + len(b.buf)) % len(b.buf)
	n := copy(out, b.buf[start:])
	copy(out[n:], b.buf[:])
	return string(out)
}
