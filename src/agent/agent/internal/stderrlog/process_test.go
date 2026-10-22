package stderrlog

import (
	"bytes"
	"errors"
	"fmt"
	"io"
	"math/rand"
	"os"
	"os/exec"
	"runtime"
	"strconv"
	"strings"
	"testing"
	"time"
)

const (
	helperEnv = "BK_CI_DAEMON_STDERR_TEST_HELPER"
	floodSize = 4 * 1024 * 1024
	footer    = "panic: stderr regression test\nlast stack frame\n"
)

func helperCommand(mode string) *exec.Cmd {
	cmd := exec.Command(os.Args[0], "-test.run=^TestStderrHelperProcess$")
	cmd.Env = append(os.Environ(), helperEnv+"="+mode)
	return cmd
}

// This runs only in explicitly spawned copies of the test executable, never an
// installed agent or daemon. Inherited-stderr helpers also have a finite life.
func TestStderrHelperProcess(t *testing.T) {
	switch os.Getenv(helperEnv) {
	case "":
		return
	case "flood":
		chunk := bytes.Repeat([]byte("x"), chunkSize)
		for i := 0; i < floodSize/chunkSize; i++ {
			if _, err := os.Stderr.Write(chunk); err != nil {
				os.Exit(90)
			}
		}
		_, _ = io.WriteString(os.Stderr, footer)
		os.Exit(23)
	case "success":
		_, _ = io.WriteString(os.Stderr, "normal exit\n")
		os.Exit(0)
	case "empty":
		os.Exit(0)
	case "inherit":
		child := helperCommand("hold")
		child.Stderr = os.Stderr
		if err := child.Start(); err != nil {
			os.Exit(91)
		}
		fmt.Println(child.Process.Pid)
		_, _ = io.WriteString(os.Stderr, "parent exited\n")
		os.Exit(0)
	case "hold":
		time.Sleep(30 * time.Second)
		os.Exit(0)
	default:
		os.Exit(92)
	}
}

func waitProcess(t *testing.T, p *Process) (Report, error) {
	t.Helper()
	type result struct {
		report Report
		err    error
	}
	done := make(chan result, 1)
	go func() {
		report, err := p.Wait()
		done <- result{report, err}
	}()
	select {
	case r := <-done:
		return r.report, r.err
	case <-time.After(10 * time.Second):
		_ = p.cmd.Process.Kill()
		t.Fatal("child or stderr drain hung")
		return Report{}, nil
	}
}

func assertFloodReport(t *testing.T, report Report, err error) {
	t.Helper()
	var exitErr *exec.ExitError
	if !errors.As(err, &exitErr) || exitErr.ExitCode() != 23 {
		t.Fatalf("expected exit code 23, got %v", err)
	}
	if report.ReadError != nil || report.DrainTimedOut || report.TotalBytes != floodSize+uint64(len(footer)) {
		t.Fatalf("unexpected capture: bytes=%d readErr=%v timedOut=%v", report.TotalBytes, report.ReadError, report.DrainTimedOut)
	}
	want := strings.Repeat("x", TailSize-len(footer)) + footer
	if report.Tail != want {
		t.Fatalf("wrong diagnostic tail (length %d, want %d)", len(report.Tail), len(want))
	}
}

func TestProcessDrainsLargeUnterminatedStderr(t *testing.T) {
	p, err := Start(helperCommand("flood"), nil)
	if err != nil {
		t.Fatal(err)
	}
	report, err := waitProcess(t, p)
	assertFloodReport(t, report, err)
}

func TestProcessExit(t *testing.T) {
	for _, tc := range []struct{ mode, tail string }{
		{"success", "normal exit\n"}, {"empty", ""},
	} {
		t.Run(tc.mode, func(t *testing.T) {
			out := &testOutput{}
			s := NewSink(out)
			p, err := Start(helperCommand(tc.mode), s)
			if err != nil {
				s.Close()
				t.Fatal(err)
			}
			report, err := waitProcess(t, p)
			s.Close()
			waitSignal(t, s.done)
			if err != nil || report.Tail != tc.tail || report.ReadError != nil || report.DrainTimedOut || out.String() != tc.tail {
				t.Fatalf("report=%+v err=%v output=%q", report, err, out.String())
			}
		})
	}
}

func TestProcessSharedSinkStaysNonblockingAcrossRestarts(t *testing.T) {
	out := newBlockedOutput()
	s := NewSink(out)
	t.Cleanup(func() {
		close(out.release)
		s.Close()
		waitSignal(t, s.done)
	})
	_, _ = s.Write([]byte("prime"))
	waitSignal(t, out.entered)
	for i := 0; i < 3; i++ {
		p, err := Start(helperCommand("flood"), s)
		if err != nil {
			t.Fatal(err)
		}
		report, err := waitProcess(t, p)
		assertFloodReport(t, report, err)
	}
	if stats := s.Stats(); stats.DroppedBytes == 0 || stats.WrittenBytes != 0 {
		t.Fatalf("expected bounded dropping while one disk writer is stuck: %+v", stats)
	}
}

func TestProcessSurvivesLogWriteFailure(t *testing.T) {
	s := NewSink(failedOutput{err: os.ErrPermission})
	p, err := Start(helperCommand("flood"), s)
	if err != nil {
		s.Close()
		t.Fatal(err)
	}
	report, err := waitProcess(t, p)
	assertFloodReport(t, report, err)
	s.Close()
	waitSignal(t, s.done)
	if stats := s.Stats(); stats.DroppedBytes != report.TotalBytes || stats.WriteErrors == 0 {
		t.Fatalf("expected all disk writes to fail without stopping drain: %+v", stats)
	}
}

func TestProcessInheritedStderrHasBoundedDrain(t *testing.T) {
	cmd := helperCommand("inherit")
	var stdout bytes.Buffer
	cmd.Stdout = &stdout
	p, err := Start(cmd, nil)
	if err != nil {
		t.Fatal(err)
	}
	start := time.Now()
	report, err := waitProcess(t, p)
	pid, parseErr := strconv.Atoi(strings.TrimSpace(stdout.String()))
	if parseErr != nil {
		t.Fatalf("could not find helper descendant pid: %q", stdout.String())
	}
	child, findErr := os.FindProcess(pid)
	if findErr != nil {
		t.Fatal(findErr)
	}
	defer child.Release()
	_ = child.Kill()
	elapsed := time.Since(start)
	if err != nil || !report.DrainTimedOut || report.ReadError != nil || report.Tail != "parent exited\n" {
		t.Fatalf("report=%+v err=%v", report, err)
	}
	if elapsed < drainTimeout || elapsed > 5*time.Second {
		t.Fatalf("unexpected drain duration %s", elapsed)
	}
}

func TestProcessStartFailure(t *testing.T) {
	path := filepathForMissingProgram(t)
	for i := 0; i < 10; i++ {
		if p, err := Start(exec.Command(path), nil); err == nil || p != nil {
			t.Fatalf("expected Start failure, got process=%v err=%v", p, err)
		}
	}
}

func filepathForMissingProgram(t *testing.T) string {
	t.Helper()
	return t.TempDir() + string(os.PathSeparator) + "nonexistent-program"
}

func TestProcessRepeatedStartsReleaseDescriptors(t *testing.T) {
	fdDir := "/proc/self/fd"
	if runtime.GOOS == "darwin" {
		fdDir = "/dev/fd"
	}
	countFDs := func() int {
		files, err := os.ReadDir(fdDir)
		if err != nil {
			return -1
		}
		return len(files)
	}
	// Warm up the Go poller before measuring descriptor usage.
	p, err := Start(helperCommand("empty"), nil)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := waitProcess(t, p); err != nil {
		t.Fatal(err)
	}
	before := countFDs()
	for i := 0; i < 12; i++ {
		p, err := Start(helperCommand("success"), nil)
		if err != nil {
			t.Fatal(err)
		}
		if report, err := waitProcess(t, p); err != nil || report.ReadError != nil || report.DrainTimedOut {
			t.Fatalf("restart %d: report=%+v err=%v", i, report, err)
		}
	}
	if after := countFDs(); before >= 0 && after > before {
		t.Fatalf("descriptor leak: before=%d after=%d", before, after)
	}
}

func TestTailBuffer(t *testing.T) {
	rng := rand.New(rand.NewSource(1))
	for _, size := range []int{0, 1, TailSize - 1, TailSize, TailSize + 1, TailSize*4 + 17} {
		data := make([]byte, size)
		_, _ = rng.Read(data)
		want := data
		if len(want) > TailSize {
			want = want[len(want)-TailSize:]
		}
		for _, step := range []int{1, 7919, TailSize, len(data) + 1} {
			var b tailBuffer
			for offset := 0; offset < len(data); {
				end := offset + step
				if end > len(data) {
					end = len(data)
				}
				b.Write(data[offset:end])
				offset = end
			}
			if b.String() != string(want) {
				t.Fatalf("tail mismatch at size=%d step=%d", size, step)
			}
		}
	}
}
