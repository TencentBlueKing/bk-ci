package thirdpartapi

import (
	"common/logs"
	"common/util/fileutil"
	"common/util/rsa"
	"context"
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
)

// 蓝盾后台服务Api接口
// 使用密匙加密作为传递凭据，密匙需要自己配置
type ServerApi struct {
	client  *http.Client
	host    string
	sha1key string
}

var (
	Sha1key string
)

func initServerApi(host string) *ServerApi {
	return &ServerApi{
		client:  http.DefaultClient,
		host:    host,
		sha1key: Sha1key,
	}
}

func (s *ServerApi) SetRemotedevHeader(request *http.Request, workspaceId, sign string) {
	request.Header.Set("Content-Type", "application/json; charset=UTF-8")
	request.Header.Set("accept", "application/json")
	// 加密
	request.Header.Set("X-Signature", hmacsha1Encrypt(s.sha1key, sign))
	// 项目，区分灰度
	// TODO: 目前先写死，未来通过workspaceId区分
	request.Header.Set("X-DEVOPS-PROJECT-ID", "grayproject")
}

// parseProjectId 从工作空间ID解析项目ID
// 项目ID为 _userid  工作空间ID为 userid-xxx
func parseProjectId(workspaceId string) string {
	workspaceSub := strings.Split(workspaceId, "-")
	if len(workspaceSub) < 2 {
		logs.Warnf("worksapceid %s format error", workspaceId)
		return ""
	}
	return "_" + workspaceSub[0]
}

func (s *ServerApi) DownloadDefaultDevfile(ctx context.Context, savePath string) error {
	req, err := http.NewRequestWithContext(ctx, "GET", fmt.Sprintf("%s/remotedev/api/external/remotedev/devfile", s.host), nil)
	if err != nil {
		return err
	}
	req.Header.Set("X-DEVOPS-PROJECT-ID", "grayproject")

	resp, err := s.client.Do(req)
	defer func() {
		if resp != nil && resp.Body != nil {
			resp.Body.Close()
		}
	}()
	if err != nil {
		return errors.Wrap(err, "download default dev file failed")
	}

	if !(resp.StatusCode >= 200 && resp.StatusCode < 300) {
		if resp.StatusCode == http.StatusNotFound {
			return errors.New("file not found")
		}
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return errors.Wrapf(err, "download default dev file failed, read body error %v", resp.Body)
		}
		return errors.Wrapf(err, "download default dev file failed, status: %s, responseBody: %s", resp.Status, string(body))
	}

	err = fileutil.AtomicWriteFile(savePath, resp.Body, os.ModePerm)
	if err != nil {
		return errors.Wrap(err, "download default dev file failed")
	}

	return nil
}

func (s *ServerApi) GetUserGitCred(ctx context.Context, workspaceId string, username string) (cred string, host string, err error) {
	prk, puk, err := rsa.GenRsaKey()
	if err != nil {
		return "", "", errors.Wrap(err, "create rsa key error")
	}

	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/oauth?workspaceName=%s&userId=%s&timestamp=%s", s.host, workspaceId, username, timestamp)
	request, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return "", "", err
	}

	// 加密
	base64Puk := base64.StdEncoding.EncodeToString(puk)
	s.SetRemotedevHeader(request, workspaceId, base64Puk+timestamp)
	request.Header.Set("X-Key", base64Puk)

	resp, err := s.client.Do(request)
	if err != nil {
		return "", "", errors.Wrap(err, "requset remoting GetWorkspaceDetail error")
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", "", errors.Wrap(err, "")
	}

	result, err := IntoDevopsResult[GetUserGitCredResp](body)
	if err != nil {
		return "", "", errors.Wrap(err, "parse remoting GetUserGitCred result error")
	}
	decodeCred, err := base64.StdEncoding.DecodeString(result.Cred)
	if err != nil {
		return "", "", errors.Wrap(err, "base64 decode cred error")
	}
	cred, err = rsa.RSADecrypt(decodeCred, prk)
	if err != nil {
		return "", "", errors.Wrap(err, "rsa decrypt cred error")
	}

	return cred, result.Host, nil
}

func (s *ServerApi) GetWorkspaceDetail(ctx context.Context, workspaceId string) (*BackendWorkspaceDetail, error) {
	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/workspace-proxy/detail?workspaceName=%s&timestamp=%s", s.host, workspaceId, timestamp)
	logs.Debugf("GetWorkspaceDetail url: %s", url)
	request, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	s.SetRemotedevHeader(request, workspaceId, workspaceId+timestamp)
	if err != nil {
		return nil, err
	}

	resp, err := s.client.Do(request)
	if err != nil {
		return nil, errors.Wrap(err, "requset remoting GetWorkspaceDetail error")
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)

	result, err := IntoDevopsResult[BackendWorkspaceDetail](body)
	if err != nil {
		return nil, errors.Wrap(err, "parse remoting GetWorkspaceDetail result error")
	}

	return result, nil
}

func IntoDevopsResult[T any](body []byte) (*T, error) {
	res := &DevopsHttpResult{}
	err := json.Unmarshal(body, res)
	if err != nil {
		return nil, errors.Wrapf(err, "parse devops result error body %s", string(body))
	}

	if res.Status != 0 {
		return nil, errors.Errorf("devops result status error %s", res.Message)
	}

	data, err := json.Marshal(res.Data)
	if err != nil {
		return nil, errors.Wrapf(err, "marshal davops result data error data %v", res.Data)
	}

	result := new(T)
	err = json.Unmarshal(data, result)
	if err != nil {
		return nil, errors.Wrap(err, "parse devops result data error")
	}

	return result, err
}

func hmacsha1Encrypt(keyStr, content string) string {
	key := []byte(keyStr)
	mac := hmac.New(sha1.New, key)
	mac.Write([]byte(content))
	//进行base64编码
	res := mac.Sum(nil)
	return fmt.Sprintf("%x", res)
}
