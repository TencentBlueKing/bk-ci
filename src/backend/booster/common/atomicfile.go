/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

// Package common provides the ability to write a file with an eventual
// rename on Close (using os.Rename). This allows for a file to always be in a
// consistent state and never represent an in-progress write.
//
// NOTE: `os.Rename` may not be atomic on your operating system.
package common

import (
	"io/ioutil"
	"os"
	"path/filepath"
)

// AtomicFile behaves like os.File, but does an atomic rename operation at Close.
type AtomicFile struct {
	*os.File
	path string
}

// AtomicFileNew creates a new temporary file that will replace the file at the given
// path when Closed.
func AtomicFileNew(path string, mode os.FileMode) (*AtomicFile, error) {
	f, err := ioutil.TempFile(filepath.Dir(path), filepath.Base(path))
	if err != nil {
		return nil, err
	}
	if err := os.Chmod(f.Name(), mode); err != nil {
		_ = f.Close()
		_ = os.Remove(f.Name())
		return nil, err
	}
	return &AtomicFile{File: f, path: path}, nil
}

// Close the file replacing the configured file.
func (f *AtomicFile) Close() error {
	if err := f.File.Close(); err != nil {
		_ = os.Remove(f.File.Name())
		return err
	}
	if err := os.Rename(f.Name(), f.path); err != nil {
		return err
	}
	return nil
}

// Abort closes the file and removes it instead of replacing the configured
// file. This is useful if after starting to write to the file you decide you
// don't want it anymore.
func (f *AtomicFile) Abort() error {
	if err := f.File.Close(); err != nil {
		_ = os.Remove(f.Name())
		return err
	}
	if err := os.Remove(f.Name()); err != nil {
		return err
	}
	return nil
}
