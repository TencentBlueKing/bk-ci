// Package stderrlog drains child stderr without allowing diagnostic log I/O to
// block the child. It is internal to the daemon, not a lossless build log sink.
package stderrlog

import (
	"io"
	"sync"
)

const (
	chunkSize   = 32 * 1024
	queueChunks = 32 // At most 1 MiB queued, plus one chunk being written.
)

// Stats contains cumulative counters for the lifetime of a Sink.
type Stats struct {
	WrittenBytes uint64
	DroppedBytes uint64
	WriteErrors  uint64
	LastError    error
}

// Sink has one disk writer for the entire daemon lifetime, shared by successive
// agent processes. A stuck disk write must not create another goroutine on each
// agent restart. All disk I/O, including Close, happens outside mu.
type Sink struct {
	mu     sync.Mutex
	queue  chan []byte
	done   chan struct{}
	closed bool
	stats  Stats
}

// NewSink starts a single writer. The caller transfers ownership of output.
func NewSink(output io.WriteCloser) *Sink {
	s := &Sink{
		queue: make(chan []byte, queueChunks),
		done:  make(chan struct{}),
	}
	go s.run(output)
	return s
}

// Write copies p into a bounded queue and always reports it consumed. On
// overflow or after Close, diagnostics are counted as dropped instead of
// propagating backpressure into the child's stderr pipe.
func (s *Sink) Write(p []byte) (int, error) {
	n := len(p)
	s.mu.Lock()
	defer s.mu.Unlock()
	for len(p) > 0 {
		if s.closed || len(s.queue) == cap(s.queue) {
			s.stats.DroppedBytes += uint64(len(p))
			break
		}
		size := len(p)
		if size > chunkSize {
			size = chunkSize
		}
		chunk := append([]byte(nil), p[:size]...)
		// All producers and Close hold mu; the consumer can only free space.
		s.queue <- chunk
		p = p[size:]
	}
	return n, nil
}

// Stats returns a snapshot without waiting for disk I/O.
func (s *Sink) Stats() Stats {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.stats
}

// Close stops accepting data. The worker drains queued chunks and closes the
// output asynchronously; even a stuck output.Write/Close cannot delay shutdown.
// Close is idempotent and safe to call concurrently with Write.
func (s *Sink) Close() {
	s.mu.Lock()
	defer s.mu.Unlock()
	if !s.closed {
		s.closed = true
		close(s.queue)
	}
}

func (s *Sink) run(output io.WriteCloser) {
	defer close(s.done)
	for chunk := range s.queue {
		n, err := output.Write(chunk)
		if n < 0 {
			n = 0
		}
		if n > len(chunk) {
			n = len(chunk)
		}
		if n != len(chunk) && err == nil {
			err = io.ErrShortWrite
		}
		s.mu.Lock()
		s.stats.WrittenBytes += uint64(n)
		s.stats.DroppedBytes += uint64(len(chunk) - n)
		if err != nil {
			s.stats.WriteErrors++
			s.stats.LastError = err
		}
		s.mu.Unlock()
	}
	if err := output.Close(); err != nil {
		s.mu.Lock()
		s.stats.WriteErrors++
		s.stats.LastError = err
		s.mu.Unlock()
	}
}
