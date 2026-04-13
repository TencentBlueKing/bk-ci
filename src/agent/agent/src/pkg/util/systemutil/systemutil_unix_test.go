//go:build !windows
// +build !windows

package systemutil

import (
	"os"
	"os/user"
	"strconv"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func TestGetCurrentUser_Normal(t *testing.T) {
	logs.UNTestDebugInit()

	u := GetCurrentUser()
	if u == nil {
		t.Fatal("GetCurrentUser() returned nil")
	}
	if u.Username == "" {
		t.Error("GetCurrentUser().Username is empty")
	}
	if u.Uid == "" {
		t.Error("GetCurrentUser().Uid is empty")
	}
}

func TestFallbackCurrentUser(t *testing.T) {
	tests := []struct {
		name         string
		envUser      string
		envLogName   string
		envHome      string
		wantUsername string
		wantHome     string
	}{
		{
			name:         "from_USER_env",
			envUser:      "testuser",
			envHome:      "/home/testuser",
			wantUsername: "testuser",
			wantHome:     "/home/testuser",
		},
		{
			name:         "from_LOGNAME_env",
			envUser:      "",
			envLogName:   "loguser",
			envHome:      "/home/loguser",
			wantUsername: "loguser",
			wantHome:     "/home/loguser",
		},
		{
			name:         "uid_fallback",
			envUser:      "",
			envLogName:   "",
			envHome:      "",
			wantUsername: "uid:" + strconv.Itoa(os.Getuid()),
			wantHome:     "/tmp",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			oldUser := os.Getenv("USER")
			oldLogName := os.Getenv("LOGNAME")
			oldHome := os.Getenv("HOME")
			defer func() {
				setOrUnset("USER", oldUser)
				setOrUnset("LOGNAME", oldLogName)
				setOrUnset("HOME", oldHome)
			}()

			setOrUnset("USER", tt.envUser)
			setOrUnset("LOGNAME", tt.envLogName)
			setOrUnset("HOME", tt.envHome)

			u := fallbackCurrentUser()
			if u == nil {
				t.Fatal("fallbackCurrentUser() returned nil")
			}
			if u.Username != tt.wantUsername {
				t.Errorf("Username = %q, want %q", u.Username, tt.wantUsername)
			}
			if u.HomeDir != tt.wantHome {
				t.Errorf("HomeDir = %q, want %q", u.HomeDir, tt.wantHome)
			}
			wantUID := strconv.Itoa(os.Getuid())
			if u.Uid != wantUID {
				t.Errorf("Uid = %q, want %q", u.Uid, wantUID)
			}
		})
	}
}

func TestGetCurrentUser_NeverNil(t *testing.T) {
	logs.UNTestDebugInit()

	for i := 0; i < 10; i++ {
		u := GetCurrentUser()
		if u == nil {
			t.Fatalf("GetCurrentUser() returned nil on iteration %d", i)
		}
	}
}

func setOrUnset(key, val string) {
	if val == "" {
		os.Unsetenv(key)
	} else {
		os.Setenv(key, val)
	}
}

// verify fallbackCurrentUser matches user.Current when possible
func TestFallbackCurrentUser_MatchesReal(t *testing.T) {
	realUser, err := user.Current()
	if err != nil {
		t.Skipf("user.Current() failed: %v", err)
	}

	oldUser := os.Getenv("USER")
	oldHome := os.Getenv("HOME")
	defer func() {
		setOrUnset("USER", oldUser)
		setOrUnset("HOME", oldHome)
	}()

	os.Setenv("USER", realUser.Username)
	os.Setenv("HOME", realUser.HomeDir)

	fb := fallbackCurrentUser()
	if fb.Uid != realUser.Uid {
		t.Errorf("Uid mismatch: fallback=%q, real=%q", fb.Uid, realUser.Uid)
	}
	if fb.Username != realUser.Username {
		t.Errorf("Username mismatch: fallback=%q, real=%q", fb.Username, realUser.Username)
	}
	if fb.HomeDir != realUser.HomeDir {
		t.Errorf("HomeDir mismatch: fallback=%q, real=%q", fb.HomeDir, realUser.HomeDir)
	}
}
