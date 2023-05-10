package fileutil

import (
	"common/internal/third_party/dep/fs"
	"common/util/fileutil/copyfile"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"syscall"
)

func AtomicWriteFile(filename string, reader io.Reader, mode os.FileMode) error {
	tempFile, err := ioutil.TempFile(filepath.Split(filename))
	if err != nil {
		return err
	}
	tempName := tempFile.Name()

	if _, err := io.Copy(tempFile, reader); err != nil {
		tempFile.Close() // return value is ignored as we are already on error path
		return err
	}

	if err := tempFile.Close(); err != nil {
		return err
	}

	if err := Chmod(tempName, mode); err != nil {
		return err
	}

	return fs.RenameWithFallback(tempName, filename)
}

func Chmod(file string, perm os.FileMode) error {
	stat, err := os.Stat(file)
	if stat != nil && stat.Mode() != perm { // 修正目录权限
		mask := syscall.Umask(0)   // 临时消除用户权限掩码
		defer syscall.Umask(mask)  // 重置掩码
		err = os.Chmod(file, perm) // 修改权限
	}
	return err
}

func CopyDir(src, dst string, exclude []string) error {
	if err := os.MkdirAll(dst, os.ModePerm); err != nil && !os.IsExist(err) {
		return err
	}
	return copyfile.CopyDir(src, dst, exclude)
}
