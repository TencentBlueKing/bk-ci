//go:build windows
// +build windows

package process

import (
	"os/exec"
	"testing"
	"time"
)

func TestNewProcessExitGroup(t *testing.T) {
	eg, err := NewProcessExitGroup()
	if err != nil {
		t.Fatalf("NewProcessExitGroup failed: %v", err)
	}
	if eg == 0 {
		t.Fatal("exit group handle should not be zero")
	}
	if err := eg.Dispose(); err != nil {
		t.Errorf("Dispose failed: %v", err)
	}
}

func TestProcessExitGroup_AddProcess(t *testing.T) {
	eg, err := NewProcessExitGroup()
	if err != nil {
		t.Fatalf("NewProcessExitGroup failed: %v", err)
	}
	defer eg.Dispose()

	cmd := exec.Command("cmd.exe", "/c", "ping", "-n", "60", "127.0.0.1", ">nul")
	if err := cmd.Start(); err != nil {
		t.Fatalf("cmd.Start failed: %v", err)
	}

	if err := eg.AddProcess(cmd.Process); err != nil {
		cmd.Process.Kill()
		cmd.Wait()
		t.Fatalf("AddProcess failed: %v", err)
	}

	if cmd.ProcessState != nil {
		t.Error("process should still be running")
	}

	cmd.Process.Kill()
	cmd.Wait()
}

func TestProcessExitGroup_KillOnClose(t *testing.T) {
	eg, err := NewProcessExitGroup()
	if err != nil {
		t.Fatalf("NewProcessExitGroup failed: %v", err)
	}

	cmd := exec.Command("cmd.exe", "/c", "ping", "-n", "120", "127.0.0.1", ">nul")
	if err := cmd.Start(); err != nil {
		t.Fatalf("cmd.Start failed: %v", err)
	}

	if err := eg.AddProcess(cmd.Process); err != nil {
		cmd.Process.Kill()
		cmd.Wait()
		t.Fatalf("AddProcess failed: %v", err)
	}

	if err := eg.Dispose(); err != nil {
		t.Fatalf("Dispose failed: %v", err)
	}

	done := make(chan error, 1)
	go func() { done <- cmd.Wait() }()

	select {
	case <-done:
		// Process was terminated by job close — expected
	case <-time.After(5 * time.Second):
		cmd.Process.Kill()
		t.Fatal("process was not killed within 5 seconds after Dispose")
	}
}

func TestProcessExitGroup_MultipleProcesses(t *testing.T) {
	eg, err := NewProcessExitGroup()
	if err != nil {
		t.Fatalf("NewProcessExitGroup failed: %v", err)
	}

	cmds := make([]*exec.Cmd, 3)
	for i := range cmds {
		cmds[i] = exec.Command("cmd.exe", "/c", "ping", "-n", "120", "127.0.0.1", ">nul")
		if err := cmds[i].Start(); err != nil {
			t.Fatalf("cmd[%d].Start failed: %v", i, err)
		}
		if err := eg.AddProcess(cmds[i].Process); err != nil {
			for j := 0; j <= i; j++ {
				cmds[j].Process.Kill()
				cmds[j].Wait()
			}
			eg.Dispose()
			t.Fatalf("AddProcess[%d] failed: %v", i, err)
		}
	}

	eg.Dispose()

	for i, cmd := range cmds {
		done := make(chan error, 1)
		go func() { done <- cmd.Wait() }()

		select {
		case <-done:
			// ok
		case <-time.After(5 * time.Second):
			cmd.Process.Kill()
			t.Errorf("process[%d] was not killed within 5 seconds", i)
		}
	}
}

func TestProcessExitGroup_DisposeIdempotent(t *testing.T) {
	eg, err := NewProcessExitGroup()
	if err != nil {
		t.Fatalf("NewProcessExitGroup failed: %v", err)
	}

	if err := eg.Dispose(); err != nil {
		t.Errorf("first Dispose failed: %v", err)
	}
	// Second Dispose should return error (invalid handle) but not panic
	_ = eg.Dispose()
}
