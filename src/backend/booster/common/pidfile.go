/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package common

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"

	"github.com/Tencent/bk-ci/src/booster/common/conf"
)

var pidFile string

// SavePid save current process's pid into files.
func SavePid(processConfig conf.ProcessConfig) error {
	pidPath := filepath.Join(processConfig.PidDir, filepath.Base(os.Args[0])+".pid")
	if fi, err := os.Stat(pidPath); err == nil && !fi.IsDir() {
		_ = os.Remove(pidPath)
	} else if !os.IsNotExist(err) {
		return err
	}
	SetPidFilePath(pidPath)
	if err := WritePid(); err != nil {
		return fmt.Errorf("write pid file failed. err:%s", err.Error())
	}

	return nil
}

// SetPidFilePath sets the pidFile path.
func SetPidFilePath(p string) {
	pidFile = p
}

// WritePid the pidFile based on the flag. It is an error if the pidFile hasn't
// been configured.
func WritePid() error {
	if pidFile == "" {
		return fmt.Errorf("pidFile is not set")
	}

	if err := os.MkdirAll(filepath.Dir(pidFile), os.FileMode(0755)); err != nil {
		return err
	}

	file, err := AtomicFileNew(pidFile, os.FileMode(0644))
	if err != nil {
		return fmt.Errorf("error opening pidFile %s: %s", pidFile, err)
	}
	defer func() {
		_ = file.Close() // in case we fail before the explicit close
	}()

	_, err = fmt.Fprintf(file, "%d", os.Getpid())
	if err != nil {
		return err
	}

	err = file.Close()
	if err != nil {
		return err
	}

	return nil
}

// ReadPid the pid from the configured file. It is an error if the pidFile hasn't
// been configured.
func ReadPid() (int, error) {
	if pidFile == "" {
		return 0, fmt.Errorf("pidFile is empty")
	}

	d, err := ioutil.ReadFile(pidFile)
	if err != nil {
		return 0, err
	}

	pid, err := strconv.Atoi(string(bytes.TrimSpace(d)))
	if err != nil {
		return 0, fmt.Errorf("error parsing pid from %s: %s", pidFile, err)
	}

	return pid, nil
}
