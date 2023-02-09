package service

import (
	"devopsRemoting/common/logs"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"os/user"
	"regexp"
	"strconv"
	"strings"

	"github.com/pkg/errors"
)

const (
	devopsRemotingUID       = 33333
	devopsRemotingUserName  = "devopsRemoting"
	devopsRemotingGID       = 33333
	devopsRemotingGroupName = "devopsRemoting"
)

func AddDevopsRemotingUserIfNotExists() error {
	ok, err := hasGroup(devopsRemotingGroupName, devopsRemotingGID)
	if err != nil {
		return err
	}
	if !ok {
		err = addGroup(devopsRemotingGroupName, devopsRemotingGID)
		if err != nil {
			return err
		}
	}
	if err := addSudoer(devopsRemotingGroupName); err != nil {
		logs.WithError(err).Error("add devopsRemoting sudoers")
	}

	targetUser := &user.User{
		Uid:      strconv.Itoa(devopsRemotingUID),
		Gid:      strconv.Itoa(devopsRemotingGID),
		Username: devopsRemotingUserName,
		HomeDir:  "/home/" + devopsRemotingUserName,
	}
	ok, err = hasUser(targetUser)
	if err != nil {
		return err
	}
	if ok {
		return nil
	}

	return addUser(targetUser)
}

func hasGroup(name string, gid int) (bool, error) {
	grpByName, err := user.LookupGroup(name)
	if err == user.UnknownGroupError(name) {
		err = nil
	}
	if err != nil {
		return false, err
	}
	grpByID, err := user.LookupGroupId(strconv.Itoa(gid))
	if err == user.UnknownGroupIdError(strconv.Itoa(gid)) {
		err = nil
	}
	if err != nil {
		return false, err
	}

	if grpByID == nil && grpByName == nil {
		// 组不存在
		return false, nil
	}
	if grpByID != nil && grpByName == nil {
		// GID存在但是名字不同
		return true, errors.Errorf("group %s already uses GID %d", grpByID.Name, gid)
	}
	if grpByID == nil && grpByName != nil {
		// 名字存在但是GID不同
		return true, errors.Errorf("group named %s exists but uses different GID %s, should be: %d", name, grpByName.Gid, devopsRemotingGID)
	}

	return true, nil
}

func hasUser(u *user.User) (bool, error) {
	userByName, err := user.Lookup(u.Username)
	if err == user.UnknownUserError(u.Username) {
		err = nil
	}
	if err != nil {
		return false, err
	}
	userByID, err := user.LookupId(u.Uid)
	uid, _ := strconv.Atoi(u.Uid)
	if err == user.UnknownUserIdError(uid) {
		err = nil
	}
	if err != nil {
		return false, err
	}

	if userByID == nil && userByName == nil {
		// 用户不存在
		return false, nil
	}
	if userByID != nil && userByName == nil {
		// 用户ID存在但是名字不同
		return true, errors.Errorf("user %s already uses UID %s", userByID.Username, u.Uid)
	}
	if userByID == nil && userByName != nil {
		// 用户名字存在但是ID不同
		return true, errors.Errorf("user named %s exists but uses different UID %s, should be: %d", u.Username, userByName.Uid, devopsRemotingUID)
	}

	existingUser := userByID
	if existingUser.Gid != u.Gid {
		return true, errors.Errorf("existing user %s has different GID %s (instead of %s)", existingUser.Username, existingUser.Gid, u.Gid)
	}
	if existingUser.HomeDir != u.HomeDir {
		return true, errors.Errorf("existing user %s has different home directory %s (instead of %s)", existingUser.Username, existingUser.HomeDir, u.HomeDir)
	}

	return true, nil
}

func addGroup(name string, gid int) error {
	flavour := determineAddgroupFlavour()
	if flavour == cmdUnknown {
		return errors.New("no addgroup command found")
	}

	args := addgroupCommands[flavour](name, gid)
	out, err := exec.Command(args[0], args[1:]...).CombinedOutput()
	if err != nil {
		return errors.Errorf("%s: %s", err.Error(), string(out))
	}
	logs.WithField("args", args).Debug("addgroup")

	return nil
}

func addUser(opts *user.User) error {
	flavour := determineAdduserFlavour()
	if flavour == cmdUnknown {
		return errors.Errorf("no adduser command found")
	}

	args := adduserCommands[flavour](opts)
	out, err := exec.Command(args[0], args[1:]...).CombinedOutput()
	if err != nil {
		return errors.Errorf("%v: %s: %s", args, err.Error(), string(out))
	}
	logs.WithField("args", args).Debug("adduser")

	return nil
}

// addSudoer 将用户组添加的管理员组
func addSudoer(group string) error {
	if group == "" {
		return errors.New("group name should not be empty")
	}
	sudoersPath := "/etc/sudoers"
	finfo, err := os.Stat(sudoersPath)
	if err != nil {
		return err
	}
	b, err := ioutil.ReadFile(sudoersPath)
	if err != nil {
		return err
	}
	devopsRemotingSudoer := []byte(fmt.Sprintf("%%%s ALL=NOPASSWD:ALL", group))
	// Line starts with "%devopsRemoting ..."
	re := regexp.MustCompile(fmt.Sprintf("(?m)^%%%s\\s+.*?$", group))
	if len(re.FindStringIndex(string(b))) > 0 {
		nb := re.ReplaceAll(b, devopsRemotingSudoer)
		return os.WriteFile(sudoersPath, nb, finfo.Mode().Perm())
	}
	file, err := os.OpenFile(sudoersPath, os.O_APPEND|os.O_WRONLY, os.ModeAppend)
	if err != nil {
		return err
	}
	defer file.Close()
	_, err = file.Write(append([]byte("\n"), devopsRemotingSudoer...))
	return err
}

func determineCmdFlavour(args []string) bool {
	var flags []string
	for _, a := range args {
		if len(a) > 0 && a[0] == '-' {
			flags = append(flags, a)
		}
	}

	rout, _ := exec.Command(args[0], "-h").CombinedOutput()
	var (
		out   = string(rout)
		found = true
	)
	for _, f := range flags {
		if !strings.Contains(out, f) {
			found = false
			break
		}
	}
	return found
}

func determineAddgroupFlavour() int {
	for flavour, gen := range addgroupCommands {
		args := gen("", 0)
		if determineCmdFlavour(args) {
			return flavour
		}
	}
	return cmdUnknown
}

func determineAdduserFlavour() int {
	for flavour, gen := range adduserCommands {
		args := gen(&user.User{})
		if determineCmdFlavour(args) {
			return flavour
		}
	}
	return cmdUnknown
}

const (
	cmdUnknown = -1
)

const defaultShell = "/bin/sh"

// adduserCommands 在不同操作系统中用来添加用户的命令
var adduserCommands = []func(*user.User) []string{
	func(opts *user.User) []string {
		return []string{"adduser", "--home", opts.HomeDir, "--shell", defaultShell, "--disabled-login", "--gid", opts.Gid, "--uid", opts.Uid, opts.Username}
	}, // Debian
	func(opts *user.User) []string {
		return []string{"adduser", "-h", opts.HomeDir, "-s", defaultShell, "-D", "-G", opts.Gid, "-u", opts.Uid, opts.Username}
	}, // Busybox
	func(opts *user.User) []string {
		return []string{"useradd", "-m", "--home-dir", opts.HomeDir, "--shell", defaultShell, "--gid", opts.Gid, "--uid", opts.Uid, opts.Username}
	}, // Useradd
}

// addgroupCommands 首先检查长标志以避免 --gid 包含 -g 而 -g 标志并不真正存在
var addgroupCommands = []func(name string, gid int) []string{
	func(name string, gid int) []string { return []string{"addgroup", "--gid", strconv.Itoa(gid), name} }, // Debian
	func(name string, gid int) []string { return []string{"groupadd", "--gid", strconv.Itoa(gid), name} }, // Useradd
	func(name string, gid int) []string { return []string{"addgroup", "-g", strconv.Itoa(gid), name} },    // Busybox
}
