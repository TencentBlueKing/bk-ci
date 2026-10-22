package stderrlog

import (
	"bytes"
	"errors"
	"io"
	"os"
	"path/filepath"
	"sync"
	"testing"
	"time"

	lumberjack "gopkg.in/natefinch/lumberjack.v2"
)

type testOutput struct {
	bytes.Buffer
	closed bool
}

func (w *testOutput) Close() error {
	w.closed = true
	return nil
}

type blockedOutput struct {
	testOutput
	entered chan struct{}
	release chan struct{}
	once    sync.Once
}

func newBlockedOutput() *blockedOutput {
	return &blockedOutput{entered: make(chan struct{}), release: make(chan struct{})}
}

func (w *blockedOutput) Write(p []byte) (int, error) {
	w.once.Do(func() { close(w.entered) })
	<-w.release
	return w.testOutput.Write(p)
}

func waitSignal(t *testing.T, ch <-chan struct{}) {
	t.Helper()
	select {
	case <-ch:
	case <-time.After(10 * time.Second):
		t.Fatal("timed out waiting for worker")
	}
}

func TestSinkOverflowAndCloseDoNotWaitForDisk(t *testing.T) {
	out := newBlockedOutput()
	s := NewSink(out)
	t.Cleanup(func() {
		close(out.release)
		s.Close()
		waitSignal(t, s.done)
	})
	_, _ = s.Write([]byte("first"))
	waitSignal(t, out.entered)
	payload := bytes.Repeat([]byte("x"), chunkSize*(queueChunks+3))
	done := make(chan struct{})
	go func() {
		_, _ = s.Write(payload)
		s.Close()
		s.Close()
		_, _ = s.Write([]byte("after-close"))
		close(done)
	}()
	waitSignal(t, done)
	stats := s.Stats()
	wantDropped := uint64(3*chunkSize + len("after-close"))
	if stats.DroppedBytes != wantDropped {
		t.Fatalf("dropped %d, want %d", stats.DroppedBytes, wantDropped)
	}
	if len(s.queue) != queueChunks {
		t.Fatalf("queued %d chunks, want %d", len(s.queue), queueChunks)
	}
}

func TestSinkCopiesQueuedBytes(t *testing.T) {
	out := newBlockedOutput()
	s := NewSink(out)
	_, _ = s.Write([]byte("first"))
	waitSignal(t, out.entered)
	payload := []byte("original")
	_, _ = s.Write(payload)
	copy(payload, "modified")
	close(out.release)
	s.Close()
	waitSignal(t, s.done)
	if got := out.String(); got != "firstoriginal" || !out.closed {
		t.Fatalf("output=%q closed=%v", got, out.closed)
	}
}

type failedOutput struct {
	partial bool
	err     error
}

func (w failedOutput) Write(p []byte) (int, error) {
	if w.partial {
		return len(p) / 2, w.err
	}
	return 0, w.err
}

func (failedOutput) Close() error { return nil }

func TestSinkWriteErrors(t *testing.T) {
	for _, tc := range []struct {
		name    string
		output  failedOutput
		written uint64
		wantErr error
	}{
		{"permission", failedOutput{err: os.ErrPermission}, 0, os.ErrPermission},
		{"partial-error", failedOutput{partial: true, err: os.ErrPermission}, 4, os.ErrPermission},
		{"short-write", failedOutput{partial: true}, 4, io.ErrShortWrite},
	} {
		t.Run(tc.name, func(t *testing.T) {
			s := NewSink(tc.output)
			if n, err := s.Write([]byte("abcdefgh")); n != 8 || err != nil {
				t.Fatalf("Write=%d, %v", n, err)
			}
			s.Close()
			waitSignal(t, s.done)
			stats := s.Stats()
			if stats.WrittenBytes != tc.written || stats.DroppedBytes != 8-tc.written ||
				stats.WriteErrors != 1 || !errors.Is(stats.LastError, tc.wantErr) {
				t.Fatalf("unexpected stats: %+v", stats)
			}
		})
	}
}

func TestSinkConcurrentWriteAndClose(t *testing.T) {
	out := &testOutput{}
	s := NewSink(out)
	var wg sync.WaitGroup
	for i := 0; i < 8; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < 100; j++ {
				_, _ = s.Write([]byte("data"))
				_ = s.Stats()
			}
		}()
	}
	s.Close()
	wg.Wait()
	waitSignal(t, s.done)
	stats := s.Stats()
	if stats.WrittenBytes+stats.DroppedBytes != 8*100*4 || !out.closed {
		t.Fatalf("unexpected accounting: %+v, closed=%v", stats, out.closed)
	}
}

func TestSinkRollingLog(t *testing.T) {
	dir := t.TempDir()
	s := NewSink(&lumberjack.Logger{
		Filename: filepath.Join(dir, "stderr.log"), MaxSize: 1, MaxBackups: 3, MaxAge: 7,
	})
	t.Cleanup(func() { s.Close(); waitSignal(t, s.done) })
	chunk := bytes.Repeat([]byte("x"), chunkSize)
	for i := 1; i <= 36; i++ {
		_, _ = s.Write(chunk)
		deadline := time.Now().Add(10 * time.Second)
		for s.Stats().WrittenBytes != uint64(i*chunkSize) {
			if time.Now().After(deadline) || s.Stats().WriteErrors != 0 {
				t.Fatalf("write did not finish: %+v", s.Stats())
			}
			time.Sleep(time.Millisecond)
		}
	}
	s.Close()
	waitSignal(t, s.done)
	files, err := filepath.Glob(filepath.Join(dir, "*.log"))
	if err != nil || len(files) != 2 {
		t.Fatalf("expected active and rotated log, files=%v err=%v", files, err)
	}
	var total int64
	for _, file := range files {
		info, err := os.Stat(file)
		if err != nil {
			t.Fatal(err)
		}
		if info.Size() > 1024*1024 {
			t.Fatalf("log exceeded size limit: %s (%d)", file, info.Size())
		}
		total += info.Size()
	}
	if total != 36*chunkSize || s.Stats().DroppedBytes != 0 {
		t.Fatalf("lost data: total=%d stats=%+v", total, s.Stats())
	}
}
