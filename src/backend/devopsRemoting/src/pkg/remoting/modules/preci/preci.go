package preci

import (
	"context"
	"devopsRemoting/common/logs"
	"devopsRemoting/common/util/fileutil"
	"devopsRemoting/src/pkg/remoting/config"
	"devopsRemoting/src/pkg/remoting/modules/user"
	"devopsRemoting/src/pkg/remoting/service"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"

	"github.com/pkg/errors"
)

func StartPreci(
	ctx context.Context,
	cstate *service.InMemoryContentState,
	cfg *config.Config,
	tokenService *service.TokenService,
	childProcessEnv []string,
) {

	workDir := filepath.Join(cfg.WorkSpace.WorkspaceRootPath, "PreCI")

	// 先检查preci文件夹是否存在
	if _, err := os.Stat(workDir); err != nil && os.IsNotExist(err) {
		os.MkdirAll(workDir, os.ModePerm)
	}

	// 更新preci到最新版本
	if err := updatePreci(ctx, workDir, cfg.WorkSpace.PreCIDownUrl); err != nil {
		logs.WithError(err).Error("updatePreci error")
		return
	}

	tokenUpdate := tokenService.ObserveBkticket(ctx)

	<-cstate.ContentReady()

	for {
		ticket := <-tokenUpdate

		if err := updateSession(workDir, ticket, cfg.WorkSpace.PreciGateWayUrl); err != nil {
			logs.WithError(err).Error("updateSession error")
			continue
		}

		// 都完成了之后启动preci
		// TODO: 根据问题，看未来要不要加上重试机制
		if err := preciStart(workDir, childProcessEnv); err != nil {
			logs.WithError(err).Error("preciStart error")
			continue
		}

		// preci初始化用户项目，初始化失败可能是别的原因，暂时不因为初始化失败干掉整个preci
		if err := preciInit(workDir, cfg.WorkSpace.GitRepoRootPath, childProcessEnv); err != nil {
			logs.WithError(err).Error("preciInit error")
			continue
		}

		logs.Debugf("preci init success~")
	}
}

func updateSession(workDir string, ticket *service.BkticktContent, preciGateway string) error {
	sessionPath := filepath.Join(workDir, "session")

	info := &preciInfo{
		Username:    ticket.User,
		ExpiredTime: 0,
		GatewayURL:  preciGateway,
		Path:        sessionPath,
		BkTicket:    ticket.Ticket,
	}

	// 创建session文件，如果存在则重新创建
	if err := os.Remove(sessionPath); err != nil && !os.IsNotExist(err) {
		return errors.Wrap(err, "updateSession remove old session file error")
	}

	sessionData, err := json.Marshal(info)
	if err != nil {
		return errors.Wrap(err, "updateSession error")
	}
	err = os.WriteFile(sessionPath, sessionData, os.ModePerm)
	if err != nil {
		return errors.Wrap(err, "updateSession error")
	}

	return nil
}

type preciInfo struct {
	Username    string `json:"username"`
	ExpiredTime int    `json:"expired_time"`
	GatewayURL  string `json:"gateway_url"`
	Path        string `json:"Path"`
	BkTicket    string `json:"bk_ticket"`
}

type acessTokenRequestResp struct {
	Uid         string `json:"uid"`
	AccessToken string `json:"accessToken"`
	BkTicket    string `json:"bk_ticket"`
}

func updatePreci(ctx context.Context, workDir string, preciDownUrl string) error {
	// 每一次直接下载最新的对老的进行覆盖
	preciPath := filepath.Join(workDir, "preci")
	os.Remove(preciPath)
	if err := downloadPreciFile(ctx, preciPath, preciDownUrl); err != nil {
		return err
	}
	fileutil.Chmod(preciPath, os.ModePerm)
	return nil
}

func downloadPreciFile(ctx context.Context, filepath string, preciDownUrl string) error {
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
		return errors.Wrap(err, "download upgrade file failed")
	}

	if !(resp.StatusCode >= 200 && resp.StatusCode < 300) {
		if resp.StatusCode == http.StatusNotFound {
			return errors.New("file not found")
		}
		body, _ := ioutil.ReadAll(resp.Body)
		return errors.Wrapf(err, "download upgrade file failed, status: %s, responseBody: %s", resp.Status, string(body))
	}

	err = fileutil.AtomicWriteFile(filepath, resp.Body, os.ModePerm)
	if err != nil {
		return errors.Wrap(err, "download upgrade file failed")
	}

	return nil
}

func preciStart(workDir string, childProcEnvvars []string) error {
	cmd := exec.Command(filepath.Join(workDir, "preci"), append([]string{"server", "--restart"})...)
	cmd = user.RunAsDevopsRemotingUser(cmd)
	cmd.Env = childProcEnvvars
	cmd.Dir = workDir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err := cmd.Run()
	if err != nil {
		return errors.Wrap(err, "preciStart error")
	}
	return nil
}

func preciInit(workDir, gitRoot string, childProcEnvvars []string) error {
	cmd := exec.Command(filepath.Join(workDir, "preci"), append([]string{"init"})...)
	cmd = user.RunAsDevopsRemotingUser(cmd)
	cmd.Env = childProcEnvvars
	cmd.Dir = gitRoot
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err := cmd.Run()
	if err != nil {
		return errors.Wrap(err, "preciInit error")
	}
	return nil
}
