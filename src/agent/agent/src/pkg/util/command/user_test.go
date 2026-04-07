//go:build linux || darwin
// +build linux darwin

package command

import (
	"os"
	"os/exec"
	"os/user"
	"path/filepath"
	"syscall"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func initTestLogger(t *testing.T) {
	t.Helper()
	logFile := filepath.Join(t.TempDir(), "test.log")
	if err := logs.Init(logFile, false, false); err != nil {
		t.Fatalf("init test logger: %v", err)
	}
}

func TestSetUser_PreservesSysProcAttr(t *testing.T) {
	initTestLogger(t)

	currentUser, err := user.Current()
	if err != nil {
		t.Skipf("cannot get current user: %v", err)
	}

	tests := []struct {
		name        string
		preSetPgid  bool
		wantSetpgid bool
	}{
		{
			name:        "preserves existing Setpgid=true",
			preSetPgid:  true,
			wantSetpgid: true,
		},
		{
			name:        "creates SysProcAttr when nil",
			preSetPgid:  false,
			wantSetpgid: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cmd := exec.Command("echo")
			cmd.Env = []string{}

			if tt.preSetPgid {
				cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
			}

			err := SetUser(cmd, currentUser.Username)
			if err != nil {
				t.Fatalf("SetUser returned error: %v", err)
			}

			if cmd.SysProcAttr == nil {
				t.Fatal("SysProcAttr should not be nil after SetUser")
			}

			if cmd.SysProcAttr.Setpgid != tt.wantSetpgid {
				t.Errorf("Setpgid = %v, want %v", cmd.SysProcAttr.Setpgid, tt.wantSetpgid)
			}

			if cmd.SysProcAttr.Credential == nil {
				t.Error("Credential should be set after SetUser")
			}
		})
	}
}

func TestSetUser_EmptyUser(t *testing.T) {
	cmd := exec.Command("echo")
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}

	err := SetUser(cmd, "")
	if err != nil {
		t.Fatalf("SetUser with empty user should not error: %v", err)
	}

	if cmd.SysProcAttr.Setpgid != true {
		t.Error("empty user should not modify SysProcAttr")
	}
}

func TestSetUser_SkipsForSameUserWithEnvs(t *testing.T) {
	initTestLogger(t)

	currentUser, err := user.Current()
	if err != nil {
		t.Skipf("cannot get current user: %v", err)
	}

	cmd := exec.Command("echo")
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
	cmd.Env = []string{
		"HOME=" + currentUser.HomeDir,
		"USER=" + currentUser.Username,
		"LOGNAME=" + currentUser.Username,
	}

	// Need to set the current user for systemutil to return
	os.Setenv("USER", currentUser.Username)

	err = SetUser(cmd, currentUser.Username)
	if err != nil {
		t.Fatalf("SetUser returned error: %v", err)
	}

	if cmd.SysProcAttr.Setpgid != true {
		t.Error("same user with envs should not modify SysProcAttr")
	}
}
