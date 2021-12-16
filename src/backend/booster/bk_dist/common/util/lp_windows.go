// +build windows

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

func chkStat(file string) error {
	d, err := os.Stat(file)
	if err != nil {
		return err
	}
	if d.IsDir() {
		return os.ErrPermission
	}
	return nil
}

func hasExt(file string) bool {
	i := strings.LastIndex(file, ".")
	if i < 0 {
		return false
	}
	return strings.LastIndexAny(file, `:\/`) < i
}

func findExecutable(file string, exts []string) (string, error) {
	if len(exts) == 0 {
		return file, chkStat(file)
	}
	if hasExt(file) {
		if chkStat(file) == nil {
			return file, nil
		}
	}
	for _, e := range exts {
		if f := file + e; chkStat(f) == nil {
			return f, nil
		}
	}
	return "", os.ErrNotExist
}

// LookPath searches for an executable named file in the
// directories named by the PATH environment variable.
// If file contains a slash, it is tried directly and the PATH is not consulted.
// LookPath also uses PATHEXT environment variable to match
// a suitable candidate.
// The result may be an absolute path or a path relative to the current directory.
// path = os.Getenv("path")
// pathExt = os.Getenv(`PATHEXT`)
func LookPath(file, path, pathExt string) (string, error) {
	if path == "" {
		path = os.Getenv("path")
	}
	if pathExt == "" {
		pathExt = os.Getenv(`PATHEXT`)
	}

	var exts []string
	if pathExt != "" {
		for _, e := range strings.Split(strings.ToLower(pathExt), `;`) {
			if e == "" {
				continue
			}
			if e[0] != '.' {
				e = "." + e
			}
			exts = append(exts, e)
		}
	} else {
		exts = []string{".com", ".exe", ".bat", ".cmd"}
	}

	if strings.ContainsAny(file, `:\/`) {
		if f, err := findExecutable(file, exts); err == nil {
			return f, nil
		} else {
			return "", &exec.Error{file, err}
		}
	}
	if f, err := findExecutable(filepath.Join(".", file), exts); err == nil {
		return f, nil
	}
	for _, dir := range filepath.SplitList(path) {
		if f, err := findExecutable(filepath.Join(dir, file), exts); err == nil {
			return f, nil
		}
	}
	return "", &exec.Error{file, exec.ErrNotFound}
}
