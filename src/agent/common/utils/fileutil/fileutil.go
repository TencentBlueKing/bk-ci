package fileutil

import (
	"archive/zip"
	"crypto/md5"
	"encoding/hex"
	"errors"
	"io"
	"os"
	"path/filepath"
	"strconv"
	"strings"
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
	fileStr, err := os.ReadFile(file)
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

	// 获取目标目录的绝对路径
	absTarget, err := filepath.Abs(target)
	if err != nil {
		return err
	}

	if err := os.MkdirAll(absTarget, os.ModePerm); err != nil {
		return err
	}

	for _, file := range reader.File {
		// 清理文件名中的路径遍历字符
		cleanName := filepath.Clean(file.Name)
		
		// 构建完整路径并获取绝对路径
		path := filepath.Join(absTarget, cleanName)
		absPath, err := filepath.Abs(path)
		if err != nil {
			return err
		}

		// 验证解压路径是否在目标目录内,防止 Zip Slip 攻击
		// 使用 filepath.Rel 来检查相对路径关系
		rel, err := filepath.Rel(absTarget, absPath)
		if err != nil {
			return err
		}
		// 如果相对路径以 .. 开头,说明目标路径在目标目录之外
		if strings.HasPrefix(rel, ".."+string(filepath.Separator)) || rel == ".." {
			return errors.New("illegal file path: " + file.Name)
		}

		if file.FileInfo().IsDir() {
			os.MkdirAll(absPath, os.ModePerm)
			continue
		}

		err2 := unzipFile(file, absPath)
		if err2 != nil {
			return err2
		}
	}

	return nil
}

func unzipFile(file *zip.File, path string) error {
	// 确保父目录存在
	if err := os.MkdirAll(filepath.Dir(path), os.ModePerm); err != nil {
		return err
	}

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
