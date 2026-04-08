//go:build linux
// +build linux

package command

import (
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

func TestSetUser_SameUser_SkipsCredential(t *testing.T) {
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

	err = SetUser(cmd, currentUser.Username)
	if err != nil {
		t.Fatalf("SetUser returned error: %v", err)
	}

	if cmd.SysProcAttr.Setpgid != true {
		t.Error("same user should preserve Setpgid")
	}
	if cmd.SysProcAttr.Credential != nil {
		t.Error("same user with envs should not set Credential")
	}
}

func TestSetUser_SameUser_EnsuresEnvs(t *testing.T) {
	initTestLogger(t)

	currentUser, err := user.Current()
	if err != nil {
		t.Skipf("cannot get current user: %v", err)
	}

	cmd := exec.Command("echo")
	cmd.Env = []string{}

	err = SetUser(cmd, currentUser.Username)
	if err != nil {
		t.Fatalf("SetUser returned error: %v", err)
	}

	envMap := make(map[string]bool)
	for _, e := range cmd.Env {
		envMap[e] = true
	}
	if !envMap["HOME="+currentUser.HomeDir] {
		t.Errorf("expected HOME=%s in env", currentUser.HomeDir)
	}
	if !envMap["USER="+currentUser.Username] {
		t.Errorf("expected USER=%s in env", currentUser.Username)
	}
	if !envMap["LOGNAME="+currentUser.Username] {
		t.Errorf("expected LOGNAME=%s in env", currentUser.Username)
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

func TestSetUser_NonexistentUser(t *testing.T) {
	initTestLogger(t)

	cmd := exec.Command("echo")
	cmd.Env = []string{}

	err := SetUser(cmd, "bkci_nonexistent_user_12345")
	if err == nil {
		t.Error("SetUser with nonexistent user should return error")
	}
}

func TestHasRequiredUserEnvs(t *testing.T) {
	u := &user.User{Username: "test", HomeDir: "/home/test"}

	tests := []struct {
		name string
		env  []string
		want bool
	}{
		{"all_present", []string{"HOME=/home/test", "USER=test", "LOGNAME=test"}, true},
		{"missing_home", []string{"USER=test", "LOGNAME=test"}, false},
		{"missing_user", []string{"HOME=/home/test", "LOGNAME=test"}, false},
		{"missing_logname", []string{"HOME=/home/test", "USER=test"}, false},
		{"empty_values", []string{"HOME=", "USER=", "LOGNAME="}, false},
		{"empty_env", []string{}, false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := hasRequiredUserEnvs(tt.env, u)
			if got != tt.want {
				t.Errorf("hasRequiredUserEnvs() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestEnsureUserEnvs(t *testing.T) {
	u := &user.User{Username: "testuser", HomeDir: "/home/testuser"}

	t.Run("adds_missing_envs", func(t *testing.T) {
		cmd := &exec.Cmd{Env: []string{"PATH=/usr/bin"}}
		ensureUserEnvs(cmd, u)

		envMap := make(map[string]bool)
		for _, e := range cmd.Env {
			envMap[e] = true
		}
		if !envMap["HOME=/home/testuser"] {
			t.Error("expected HOME to be added")
		}
		if !envMap["USER=testuser"] {
			t.Error("expected USER to be added")
		}
		if !envMap["LOGNAME=testuser"] {
			t.Error("expected LOGNAME to be added")
		}
	})

	t.Run("skips_existing_envs", func(t *testing.T) {
		cmd := &exec.Cmd{Env: []string{
			"HOME=/custom/home",
			"USER=customuser",
			"LOGNAME=customuser",
		}}
		ensureUserEnvs(cmd, u)

		count := 0
		for _, e := range cmd.Env {
			if len(e) > 5 && e[:5] == "HOME=" {
				count++
			}
		}
		if count != 1 {
			t.Errorf("HOME appears %d times, want 1", count)
		}
	})
}
