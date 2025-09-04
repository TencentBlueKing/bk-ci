package turbo

import (
	"common/logs"
	"common/util/fileutil"
	"context"
	"io/ioutil"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"remoting/pkg/config"
	"remoting/pkg/modules/user"
	"sync"

	"github.com/pkg/errors"
)

func StartTurbo(
	ctx context.Context,
	cfg *config.Config,
	childProcessEnv []string,
	wg *sync.WaitGroup,
) {
	defer wg.Done()

	workDir := filepath.Join(cfg.WorkSpace.WorkspaceRootPath, "turbo")

	// 先检查turbo文件夹是否存在
	if _, err := os.Stat(workDir); err != nil && os.IsNotExist(err) {
		os.MkdirAll(workDir, os.ModePerm)
	}

	// 更新turbo到最新版本
	if err := updateTurbo(ctx, workDir, cfg.WorkSpace.TurboDownUrl, childProcessEnv); err != nil {
		logs.Error("updateTurbo error", logs.Err(err))
		return
	}

	logs.Debugf("turbo install success~")
}

func updateTurbo(
	ctx context.Context,
	workDir string,
	turboScriptDownloadUrl string,
	childProcessEnv []string,
) error {
	// 每一次直接下载最新的对老的进行覆盖
	path := filepath.Join(workDir, "install_latest.sh")
	os.Remove(path)
	if err := downloadTurboScriptFile(ctx, path, turboScriptDownloadUrl); err != nil {
		return err
	}
	fileutil.Chmod(path, os.ModePerm)

	// 执行安装脚本
	cmd := exec.Command(filepath.Join(workDir, "install_latest.sh"))
	cmd = user.RunAsDevopsRemotingUser(cmd)
	cmd.Env = childProcessEnv
	cmd.Dir = workDir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err := cmd.Run()
	if err != nil {
		return errors.Wrap(err, "run turbo install script error")
	}
	return nil
}

func downloadTurboScriptFile(ctx context.Context, filepath string, preciDownUrl string) error {
	req, err := http.NewRequestWithContext(ctx, "GET", preciDownUrl, nil)
	if err != nil {
		return err
	}

	resp, err := http.DefaultClient.Do(req)
	defer func() {
		if resp != nil && resp.Body != nil {
			resp.Body.Close()
		}
	}()
	if err != nil {
		return errors.Wrap(err, "download turboscript file failed")
	}

	if !(resp.StatusCode >= 200 && resp.StatusCode < 300) {
		if resp.StatusCode == http.StatusNotFound {
			return errors.New("file not found")
		}
		body, _ := ioutil.ReadAll(resp.Body)
		return errors.Wrapf(err, "download turboscript file failed, status: %s, responseBody: %s", resp.Status, string(body))
	}

	err = fileutil.AtomicWriteFile(filepath, resp.Body, os.ModePerm)
	if err != nil {
		return errors.Wrap(err, "download turboscript file failed")
	}

	return nil
}
