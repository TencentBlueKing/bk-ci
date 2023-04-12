/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package fileutil

import (
	"archive/zip"
	"crypto/md5"
	"encoding/hex"
	"errors"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
)

func Exists(file string) bool {
	_, err := os.Stat(file)
	return !(err != nil && os.IsNotExist(err))
}

func TryRemoveFile(file string) error {
	return os.Remove(file)
}

func SetExecutable(file string) error {
	fileInfo, err := os.Stat(file)
	if err != nil {
		return err
	}
	return os.Chmod(file, fileInfo.Mode()|0111)
}

func GetFileMd5(file string) (string, error) {
	if !Exists(file) {
		return "", nil
	}

	pFile, err := os.Open(file)
	defer pFile.Close()
	if err != nil {
		return "", err
	}

	md5h := md5.New()
	io.Copy(md5h, pFile)
	return hex.EncodeToString(md5h.Sum(nil)), nil
}

func CopyFile(src string, dst string, overwrite bool) (written int64, err error) {
	logs.Info("copy file from : " + src + ", to: " + dst)

	srcStat, err := os.Stat(src)
	if err != nil {
		return 0, err
	}
	if srcStat.IsDir() {
		return 0, errors.New("src is a directory")
	}

	dstStat, err := os.Stat(dst)
	if !(err != nil && os.IsNotExist(err)) {
		if !overwrite {
			return 0, errors.New("dst file exists")
		}
		if dstStat.IsDir() {
			return 0, errors.New("dst is a directory")
		}
		if err = os.Remove(dst); err != nil {
			return 0, err
		}
	}

	srcFile, err := os.Open(src)
	if err != nil {
		return 0, err
	}
	defer srcFile.Close()

	dstFile, err := os.Create(dst)
	if err != nil {
		return 0, err
	}
	defer dstFile.Close()

	types, err := io.Copy(dstFile, srcFile)
	return types, err
}

func GetString(file string) (string, error) {
	fileStr, err := ioutil.ReadFile(file)
	if err != nil {
		return "", err
	}

	return string(fileStr), nil
}

func GetPid(file string) (int, error) {
	pidStr, err := GetString(file)
	if err != nil && !os.IsNotExist(err) {
		return 0, err
	}

	pid, err := strconv.Atoi(pidStr)
	return pid, nil
}

func WriteString(file, str string) error {
	f, err := os.OpenFile(file, os.O_WRONLY|os.O_CREATE, os.ModePerm)
	if os.IsNotExist(err) {
		f, err = os.Create(file)
	}
	if err != nil {
		return err
	}

	if err = f.Truncate(0); err != nil {
		return err
	}

	if _, err = f.WriteString(str); err != nil {
		return err
	}

	return nil
}

func Unzip(archive, target string) error {
	reader, err := zip.OpenReader(archive)
	if err != nil {
		return err
	}
	defer func() { _ = reader.Close() }()

	if err := os.MkdirAll(target, os.ModePerm); err != nil {
		return err
	}

	for _, file := range reader.File {
		path := filepath.Join(target, file.Name)
		if file.FileInfo().IsDir() {
			os.MkdirAll(path, os.ModePerm)
			continue
		}

		err2 := unzipFile(file, path)
		if err2 != nil {
			return err2
		}
	}

	return nil
}

func unzipFile(file *zip.File, path string) error {
	fileReader, err := file.Open()
	if err != nil {
		return err
	}
	defer func() { _ = fileReader.Close() }()

	targetFile, err := os.OpenFile(path, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, os.ModePerm)
	if err != nil {
		return err
	}

	defer func() { _ = targetFile.Close() }()

	if _, err := io.Copy(targetFile, fileReader); err != nil {
		return err
	}
	return nil
}
