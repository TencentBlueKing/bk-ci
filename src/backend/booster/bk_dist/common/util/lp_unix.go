// +build linux darwin

/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package util

import (
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

// LookPath get the executable target in PATH
func LookPath(target, path, _ string) (string, error) {
	if path == "" {
		path = os.Getenv("PATH")
	}
	if strings.Contains(target, "/") {
		err := findExecutable(target)
		if err == nil {
			return target, nil
		}
		return "", &exec.Error{Name: target, Err: err}
	}

	for _, dir := range filepath.SplitList(path) {
		if dir == "" {
			// Unix shell semantics: path element "" means "."
			dir = "."
		}
		p := filepath.Join(dir, target)
		if err := findExecutable(p); err == nil {
			return p, nil
		}
	}
	return "", &exec.Error{Name: target, Err: exec.ErrNotFound}
}

func findExecutable(file string) error {
	d, err := os.Stat(file)
	if err != nil {
		return err
	}
	if m := d.Mode(); !m.IsDir() && m&0111 != 0 {
		return nil
	}
	return os.ErrPermission
}
