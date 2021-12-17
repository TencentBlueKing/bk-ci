/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cache

import (
	"io"
	"os"
	"path/filepath"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
)

// File describe the cache file instance
type File interface {
	Name() string
	MD5() string
	AbsPath() string
	Dir() string
	SaveTo(string) error
	Equal(File) bool
	Size() int64
}

// NewFile get a new File instance
func NewFile(path string) (File, error) {
	return newFile(path)
}

func newFile(path string) (File, error) {
	if !filepath.IsAbs(path) {
		dir, _ := os.Getwd()
		path = filepath.Join(dir, path)
	}

	stat := dcFile.Stat(path)
	if stat.Error() != nil {
		return nil, stat.Error()
	}

	return &file{
		stat:    stat,
		absPath: path,
	}, nil
}

type file struct {
	stat    *dcFile.Info
	absPath string

	md5    string
	md5Err error
}

// Equal check if two file are the same one
func (f *file) Equal(t File) bool {
	if t == nil {
		return false
	}

	return f.Name() == t.Name() && f.MD5() == t.MD5()
}

// Name return file name
func (f *file) Name() string {
	return f.stat.Basic().Name()
}

// MD5 return the file md5
func (f *file) MD5() string {
	if f.md5 == "" && f.md5Err == nil {
		f.md5, f.md5Err = f.stat.Md5()
	}

	return f.md5
}

// AbsPath return the file abs path
func (f *file) AbsPath() string {
	return f.absPath
}

// Dir return the file abs path's dir
func (f *file) Dir() string {
	return filepath.Dir(f.absPath)
}

// Size return the file size
func (f *file) Size() int64 {
	return f.stat.Size()
}

// SaveTo copy this file to the destination
func (f *file) SaveTo(destination string) error {
	if err := os.MkdirAll(filepath.Dir(destination), os.ModePerm); err != nil {
		return err
	}

	s, err := os.Open(f.absPath)
	if err != nil {
		return err
	}

	defer func() {
		_ = s.Close()
	}()

	d, err := os.OpenFile(destination, os.O_RDWR|os.O_CREATE|os.O_TRUNC, os.ModePerm)
	if err != nil {
		return err
	}

	defer func() {
		_ = d.Close()
	}()

	_, err = io.Copy(d, s)
	return err
}
