//go:build windows
// +build windows

package systemutil

import (
	"os"
	"os/user"
)

func fallbackCurrentUser() *user.User {
	username := os.Getenv("USERNAME")
	if username == "" {
		username = "unknown"
	}

	home := os.Getenv("USERPROFILE")
	if home == "" {
		home = os.Getenv("HOMEDRIVE") + os.Getenv("HOMEPATH")
	}
	if home == "" {
		home = "C:\\Users\\" + username
	}

	return &user.User{
		Uid:      "-1",
		Gid:      "-1",
		Username: username,
		Name:     username,
		HomeDir:  home,
	}
}
