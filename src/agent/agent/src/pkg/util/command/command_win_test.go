//go:build windows
// +build windows

package command

import (
	"os/exec"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func init() {
	logs.UNTestDebugInit()
}

func TestSetUser_Windows(t *testing.T) {
	tests := []struct {
		name    string
		runUser string
		wantErr bool
	}{
		{"empty_user", "", false},
		{"any_user", "administrator", false},
		{"domain_user", "DOMAIN\\user", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cmd := exec.Command("cmd.exe", "/c", "echo", "test")
			err := SetUser(cmd, tt.runUser)
			if (err != nil) != tt.wantErr {
				t.Errorf("SetUser() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestRunCommand(t *testing.T) {
	output, err := RunCommand("cmd.exe", []string{"/c", "echo", "hello"}, "", nil)
	if err != nil {
		t.Fatalf("RunCommand failed: %v", err)
	}
	if len(output) == 0 {
		t.Error("expected non-empty output")
	}
}

func TestStartProcess(t *testing.T) {
	pid, err := StartProcess("cmd.exe", []string{"/c", "exit", "0"}, "", nil, "")
	if err != nil {
		t.Fatalf("StartProcess failed: %v", err)
	}
	if pid <= 0 {
		t.Errorf("expected positive PID, got %d", pid)
	}
}

func TestRunCommand_InvalidCommand(t *testing.T) {
	_, err := RunCommand("nonexistent_cmd_xyz.exe", nil, "", nil)
	if err == nil {
		t.Error("expected error for nonexistent command")
	}
}
