//go:build linux
// +build linux

package command

import (
	"fmt"
	"os"
	"os/exec"
	"os/user"
	"strconv"
	"strings"
	"syscall"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func SetUser(cmd *exec.Cmd, runUser string) error {
	if len(runUser) == 0 {
		return nil
	}

	rUser, err := user.Lookup(runUser)
	if err != nil {
		logs.WithError(err).Error("user lookup failed, user: -", runUser, "-, error")
		return errors.New("user lookup failed, user: " + runUser)
	}

	targetUID, _ := strconv.Atoi(rUser.Uid)
	if targetUID == os.Getuid() {
		if hasRequiredUserEnvs(cmd.Env, rUser) {
			return nil
		}
		ensureUserEnvs(cmd, rUser)
		return nil
	}

	logs.Info("set user(linux): ", runUser)

	uid, _ := strconv.Atoi(rUser.Uid)
	gid, _ := strconv.Atoi(rUser.Gid)
	if cmd.SysProcAttr == nil {
		cmd.SysProcAttr = &syscall.SysProcAttr{}
	}
	cmd.SysProcAttr.Credential = &syscall.Credential{Uid: uint32(uid), Gid: uint32(gid)}

	ensureUserEnvs(cmd, rUser)
	return nil
}

// hasRequiredUserEnvs checks whether cmd.Env already contains non-empty
// HOME, USER, and LOGNAME entries.
func hasRequiredUserEnvs(env []string, u *user.User) bool {
	homeOK, userOK, logNameOK := false, false, false
	for _, e := range env {
		parts := strings.SplitN(e, "=", 2)
		if len(parts) != 2 || parts[1] == "" {
			continue
		}
		switch parts[0] {
		case "HOME":
			homeOK = true
		case "USER":
			userOK = true
		case "LOGNAME":
			logNameOK = true
		}
	}
	return homeOK && userOK && logNameOK
}

// ensureUserEnvs appends HOME, USER, and LOGNAME to cmd.Env if not already
// present with non-empty values.
func ensureUserEnvs(cmd *exec.Cmd, u *user.User) {
	set := make(map[string]bool)
	for _, e := range cmd.Env {
		parts := strings.SplitN(e, "=", 2)
		if len(parts) == 2 && parts[1] != "" {
			set[parts[0]] = true
		}
	}
	if !set["HOME"] {
		cmd.Env = append(cmd.Env, fmt.Sprintf("HOME=%s", u.HomeDir))
	}
	if !set["USER"] {
		cmd.Env = append(cmd.Env, fmt.Sprintf("USER=%s", u.Username))
	}
	if !set["LOGNAME"] {
		cmd.Env = append(cmd.Env, fmt.Sprintf("LOGNAME=%s", u.Username))
	}
}
