//go:build !windows
// +build !windows

package systemutil

import (
	"os"
	"os/user"
)

func fallbackCurrentUser() *user.User {
	uid := strconv.Itoa(os.Getuid())
	gid := strconv.Itoa(os.Getgid())

	username := os.Getenv("USER")
	if username == "" {
		username = os.Getenv("LOGNAME")
	}
	if username == "" {
		username = "uid:" + uid
	}

	home := os.Getenv("HOME")
	if home == "" {
		home = "/tmp"
	}

	return &user.User{
		Uid:      uid,
		Gid:      gid,
		Username: username,
		Name:     username,
		HomeDir:  home,
	}
}
