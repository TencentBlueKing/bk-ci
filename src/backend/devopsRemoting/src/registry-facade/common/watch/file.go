package watch

import (
	"common/logs"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"path/filepath"

	"github.com/fsnotify/fsnotify"
)

type fileWatcher struct {
	onChange func()

	watcher *fsnotify.Watcher

	hash string
}

func File(ctx context.Context, path string, onChange func()) error {
	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		return fmt.Errorf("unexpected error creating file watcher: %w", err)
	}

	fw := &fileWatcher{
		watcher:  watcher,
		onChange: onChange,
	}

	// initial hash of the file
	hash, err := hashConfig(path)
	if err != nil {
		return fmt.Errorf("cannot get hash of file %v: %w", path, err)
	}

	// 卷中的可见文件是作者数据目录中文件的符号链接。
	// 文件存储在一个隐藏的时间戳目录中，该目录由数据目录符号链接。
	// 带时间戳的目录和数据目录符号链接在作者的目标目录中创建。
	// https://pkg.go.dev/k8s.io/kubernetes/pkg/volume/util#AtomicWriter
	watchDir, _ := filepath.Split(path)
	err = watcher.Add(watchDir)
	if err != nil {
		watcher.Close()
		return fmt.Errorf("unexpected error watching file %v: %w", path, err)
	}

	logs.Infof("starting watch of file %v", path)

	fw.hash = hash

	go func() {
		defer func() {
			logs.WithError(err).Error("Stopping file watch")

			err = watcher.Close()
			if err != nil {
				logs.WithError(err).Error("Unexpected error closing file watcher")
			}
		}()

		for {
			select {
			case event, ok := <-watcher.Events:
				if !ok {
					return
				}

				if !eventOpIs(event, fsnotify.Create) && !eventOpIs(event, fsnotify.Remove) {
					continue
				}

				currentHash, err := hashConfig(path)
				if err != nil {
					logs.WithError(err).Warn("Cannot check if config has changed")
					return
				}

				// no change
				if currentHash == fw.hash {
					continue
				}

				logs.WithField("path", path).Info("reloading file after change")

				fw.hash = currentHash
				fw.onChange()
			case err := <-watcher.Errors:
				logs.WithError(err).Error("Unexpected error watching event")
			case <-ctx.Done():
				return
			}
		}
	}()

	return nil
}

func hashConfig(path string) (hash string, err error) {
	f, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer f.Close()

	h := sha256.New()

	_, err = io.Copy(h, f)
	if err != nil {
		return "", err
	}

	return hex.EncodeToString(h.Sum(nil)), nil
}

func eventOpIs(event fsnotify.Event, op fsnotify.Op) bool {
	return event.Op&op == op
}
