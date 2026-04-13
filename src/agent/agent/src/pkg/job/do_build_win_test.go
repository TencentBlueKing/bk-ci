//go:build windows
// +build windows

package job

import (
	"os"
	"os/exec"
	"strings"
	"syscall"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
)

func init() {
	logs.UNTestDebugInit()
	envs.Init()
}

func TestStartProcessCmd_NoInheritHandles(t *testing.T) {
	// Default: NoInheritHandles should be true
	os.Unsetenv(constant.DevopsAgentNoInheritHandles)

	cmd, err := StartProcessCmd("cmd.exe", []string{"/c", "echo", "hello"}, "", nil, "")
	if err != nil {
		t.Fatalf("StartProcessCmd failed: %v", err)
	}
	defer cmd.Process.Kill()

	if cmd.SysProcAttr == nil {
		t.Fatal("SysProcAttr should not be nil")
	}
	if !cmd.SysProcAttr.NoInheritHandles {
		t.Error("NoInheritHandles should be true by default")
	}

	cmd.Wait()
}

func TestStartProcessCmd_NoInheritHandles_Disabled(t *testing.T) {
	os.Setenv(constant.DevopsAgentNoInheritHandles, "false")
	defer os.Unsetenv(constant.DevopsAgentNoInheritHandles)

	cmd, err := StartProcessCmd("cmd.exe", []string{"/c", "echo", "hello"}, "", nil, "")
	if err != nil {
		t.Fatalf("StartProcessCmd failed: %v", err)
	}
	defer cmd.Process.Kill()

	if cmd.SysProcAttr == nil {
		t.Fatal("SysProcAttr should not be nil")
	}
	if cmd.SysProcAttr.NoInheritHandles {
		t.Error("NoInheritHandles should be false when DEVOPS_AGENT_NO_INHERIT_HANDLES=false")
	}

	cmd.Wait()
}

func TestStartProcessCmd_NewConsoleFlag(t *testing.T) {
	t.Run("without_new_console", func(t *testing.T) {
		os.Unsetenv(constant.DevopsAgentEnableNewConsole)

		cmd, err := StartProcessCmd("cmd.exe", []string{"/c", "echo", "test"}, "", nil, "")
		if err != nil {
			t.Fatalf("StartProcessCmd failed: %v", err)
		}
		defer cmd.Process.Kill()

		if cmd.SysProcAttr.CreationFlags&constant.WinCommandNewConsole != 0 {
			t.Error("CREATE_NEW_CONSOLE should not be set when env is not enabled")
		}
		cmd.Wait()
	})

	t.Run("with_new_console", func(t *testing.T) {
		os.Unsetenv(constant.DevopsAgentNoInheritHandles)
		os.Setenv(constant.DevopsAgentEnableNewConsole, "true")
		defer os.Unsetenv(constant.DevopsAgentEnableNewConsole)

		cmd, err := StartProcessCmd("cmd.exe", []string{"/c", "echo", "test"}, "", nil, "")
		if err != nil {
			t.Fatalf("StartProcessCmd failed: %v", err)
		}
		defer cmd.Process.Kill()

		if cmd.SysProcAttr.CreationFlags&constant.WinCommandNewConsole == 0 {
			t.Error("CREATE_NEW_CONSOLE should be set when env is enabled")
		}
		if !cmd.SysProcAttr.NoInheritHandles {
			t.Error("NoInheritHandles should still be true with new console")
		}
		cmd.Wait()
	})
}

func TestStartProcessCmd_WorkDir(t *testing.T) {
	tmpDir := t.TempDir()

	cmd, err := StartProcessCmd("cmd.exe", []string{"/c", "cd"}, tmpDir, nil, "")
	if err != nil {
		t.Fatalf("StartProcessCmd failed: %v", err)
	}

	if cmd.Dir != tmpDir {
		t.Errorf("cmd.Dir = %q, want %q", cmd.Dir, tmpDir)
	}

	cmd.Wait()
}

func TestStartProcessCmd_EnvMap(t *testing.T) {
	envMap := map[string]string{
		"TEST_VAR_1": "value1",
		"TEST_VAR_2": "value2",
	}

	cmd, err := StartProcessCmd("cmd.exe", []string{"/c", "echo", "%TEST_VAR_1%"}, "", envMap, "")
	if err != nil {
		t.Fatalf("StartProcessCmd failed: %v", err)
	}
	defer cmd.Process.Kill()

	found := map[string]bool{"TEST_VAR_1": false, "TEST_VAR_2": false}
	for _, e := range cmd.Env {
		for k := range found {
			if strings.HasPrefix(e, k+"=") {
				found[k] = true
			}
		}
	}
	for k, v := range found {
		if !v {
			t.Errorf("env var %s not found in cmd.Env", k)
		}
	}

	cmd.Wait()
}

func TestStartProcessCmd_ProcessRuns(t *testing.T) {
	cmd, err := StartProcessCmd("cmd.exe", []string{"/c", "exit", "0"}, "", nil, "")
	if err != nil {
		t.Fatalf("StartProcessCmd failed: %v", err)
	}

	if cmd.Process == nil {
		t.Fatal("Process should not be nil after Start")
	}
	if cmd.Process.Pid <= 0 {
		t.Errorf("PID should be positive, got %d", cmd.Process.Pid)
	}

	err = cmd.Wait()
	if err != nil {
		t.Errorf("Process should exit cleanly, got: %v", err)
	}
}

func TestStartProcessCmd_InvalidCommand(t *testing.T) {
	_, err := StartProcessCmd("nonexistent_command_12345.exe", nil, "", nil, "")
	if err == nil {
		t.Error("expected error for nonexistent command")
	}
}

// TestNoInheritHandles_PipeNotLeaked verifies that a child process started with
// NoInheritHandles=true does NOT inherit the parent's pipe handles, so the pipe
// read end sees EOF immediately after the child exits.
func TestNoInheritHandles_PipeNotLeaked(t *testing.T) {
	var readHandle, writeHandle syscall.Handle
	sa := syscall.SecurityAttributes{
		Length:        uint32(12), // sizeof(SECURITY_ATTRIBUTES)
		InheritHandle: 1,          // mark as inheritable
	}
	err := syscall.CreatePipe(&readHandle, &writeHandle, &sa, 0)
	if err != nil {
		t.Fatalf("CreatePipe failed: %v", err)
	}
	defer syscall.CloseHandle(readHandle)

	cmd := exec.Command("cmd.exe", "/c", "exit", "0")
	cmd.SysProcAttr = &syscall.SysProcAttr{
		NoInheritHandles: true,
	}
	if err := cmd.Start(); err != nil {
		t.Fatalf("cmd.Start failed: %v", err)
	}
	cmd.Wait()

	// Close our write end — if no one else holds it, read should get EOF
	syscall.CloseHandle(writeHandle)

	buf := make([]byte, 1)
	var bytesRead uint32
	err = syscall.ReadFile(readHandle, buf, &bytesRead, nil)
	if err == nil {
		t.Error("expected error (broken pipe / EOF) from ReadFile, got nil")
	}
}

// TestInheritHandles_PipeLeaked is a control test showing that WITHOUT
// NoInheritHandles, a child process COULD inherit pipe handles.
func TestInheritHandles_PipeLeaked(t *testing.T) {
	var readHandle, writeHandle syscall.Handle
	sa := syscall.SecurityAttributes{
		Length:        uint32(12),
		InheritHandle: 1,
	}
	err := syscall.CreatePipe(&readHandle, &writeHandle, &sa, 0)
	if err != nil {
		t.Fatalf("CreatePipe failed: %v", err)
	}
	defer syscall.CloseHandle(readHandle)

	cmd := exec.Command("cmd.exe", "/c", "ping", "-n", "10", "127.0.0.1", ">nul")
	if err := cmd.Start(); err != nil {
		t.Fatalf("cmd.Start failed: %v", err)
	}
	defer func() {
		cmd.Process.Kill()
		cmd.Wait()
	}()

	syscall.CloseHandle(writeHandle)

	done := make(chan bool, 1)
	go func() {
		buf := make([]byte, 1)
		var bytesRead uint32
		syscall.ReadFile(readHandle, buf, &bytesRead, nil)
		done <- true
	}()

	select {
	case <-done:
		t.Log("ReadFile returned immediately — child may not have inherited handle in this Go version")
	case <-func() <-chan struct{} {
		ch := make(chan struct{})
		go func() {
			cmd.Process.Kill()
			cmd.Wait()
			<-done
			close(ch)
		}()
		return ch
	}():
		t.Log("ReadFile unblocked after killing child — confirms handle was inherited")
	}
}
