/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package flock

import (
	"os"
	"os/exec"
	"path/filepath"

	"github.com/gofrs/flock"
)

// define vars
var (
	lockfileSuffix = ".lock"
	lock           *flock.Flock
)

// TryLock : try lock
func TryLock(lockfile string) (bool, error) {
	initLock(lockfile)
	return lock.TryLock()
}

// TryRLock : try read lock
func TryRLock(lockfile string) (bool, error) {
	initLock(lockfile)
	return lock.TryRLock()
}

// Lock :  Lock
func Lock(lockfile string) error {
	initLock(lockfile)
	return lock.Lock()
}

// RLock :  read lock
func RLock(lockfile string) error {
	initLock(lockfile)
	return lock.RLock()
}

// Unlock :  unlock
func Unlock() error {
	if lock == nil {
		return nil
	}
	return lock.Unlock()
}

// Close is equivalent to calling Unlock.
//
// This will release the lock and close the underlying file descriptor.
// It will not remove the file from disk, that's up to your application.
func Close() error {
	if lock == nil {
		return nil
	}
	return lock.Close()
}

func initLock(lockfile string) {
	if lock == nil {
		lock = flock.New(getLockFile(lockfile))
	}
}

// getExecFullPath : get current exe abs path
func getExecFullPath() string {
	execPath, _ := exec.LookPath(os.Args[0])
	fileInfo, _ := os.Lstat(execPath)
	if fileInfo.Mode()&os.ModeSymlink == os.ModeSymlink {
		execPath, _ = os.Readlink(execPath)
	}
	fullPath, _ := filepath.Abs(execPath)

	return fullPath
}

func getLockFile(lockfile string) string {
	if lockfile != "" {
		abspath, _ := filepath.Abs(lockfile)
		return abspath
	}

	exeabspath := getExecFullPath()
	return exeabspath + lockfileSuffix
}
