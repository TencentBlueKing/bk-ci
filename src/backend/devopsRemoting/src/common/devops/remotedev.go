package devops

import (
	"common/util/fileutil"
	"common/util/rsa"
	"context"
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/pkg/errors"
)

// devops后台 remotedev接口
type RemoteDevClient struct {
	client  *http.Client
	host    string
	sha1key string
}

func NewRemoteDevClient(hostname, sha1key string) *RemoteDevClient {
	return &RemoteDevClient{
		client:  &http.Client{},
		host:    hostname,
		sha1key: sha1key,
	}
}

func (r *RemoteDevClient) setRemotedevHeader(request *http.Request, workspaceId, sign string) {
	request.Header.Set("Content-Type", "application/json; charset=UTF-8")
	request.Header.Set("accept", "application/json")
	// 加密
	request.Header.Set("X-Signature", hmacsha1Encrypt(r.sha1key, sign))
	// 项目，区分灰度
	// TODO: 目前先写死，未来通过workspaceId区分
	request.Header.Set("X-DEVOPS-PROJECT-ID", ParseWsId2UserProjectId(workspaceId))
}

// DownloadDefaultDevfile 下载工作空间默认的devfile
func (r *RemoteDevClient) DownloadDefaultDevfile(ctx context.Context, savePath string, wsId string) error {
	req, err := http.NewRequestWithContext(ctx, "GET", fmt.Sprintf("%s/remotedev/api/external/remotedev/devfile", r.host), nil)
	if err != nil {
		return err
	}
	req.Header.Set("X-DEVOPS-PROJECT-ID", ParseWsId2UserProjectId(wsId))

	resp, err := r.client.Do(req)
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

// GetUserGitCred 获取用户git凭据信息
func (r *RemoteDevClient) GetUserGitCred(ctx context.Context, workspaceId string, username string) (cred string, host string, err error) {
	prk, puk, err := rsa.GenRsaKey()
	if err != nil {
		return "", "", errors.Wrap(err, "create rsa key error")
	}

	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/oauth?workspaceName=%s&userId=%s&timestamp=%s", r.host, workspaceId, username, timestamp)
	request, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return "", "", err
	}

	// 加密
	base64Puk := base64.StdEncoding.EncodeToString(puk)
	r.setRemotedevHeader(request, workspaceId, base64Puk+timestamp)
	request.Header.Set("X-Key", base64Puk)

	resp, err := r.client.Do(request)
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

type GetUserGitCredResp struct {
	Cred string `json:"value"`
	Host string `json:"host"`
}

// GetWorkspaceDetail 获取工作空间详情
func (r *RemoteDevClient) GetWorkspaceDetail(ctx context.Context, workspaceId string) (*BackendWorkspaceDetail, error) {
	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/workspace-proxy/detail?workspaceName=%s&timestamp=%s", r.host, workspaceId, timestamp)
	request, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	r.setRemotedevHeader(request, workspaceId, workspaceId+timestamp)
	if err != nil {
		return nil, err
	}

	resp, err := r.client.Do(request)
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

type BackendWorkspaceDetail struct {
	WorkspaceName   string `json:"workspaceName"`
	PodIp           string `json:"podIp"`
	SSHKey          string `json:"sshKey"`
	EnvironmentHost string `json:"environmentHost"`
}

// SendHeartbeat 发送工作空间心跳信息
func (r *RemoteDevClient) SendHeartbeat(ctx context.Context, workspaceId string) error {
	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/workspace/heartbeat?workspaceName=%s&timestamp=%s", r.host, workspaceId, timestamp)
	request, err := http.NewRequestWithContext(ctx, "POST", url, nil)
	if err != nil {
		return err
	}
	r.setRemotedevHeader(request, workspaceId, timestamp)

	resp, err := r.client.Do(request)
	if err != nil {
		return errors.Wrap(err, "requset remoting SendHeartbeat error")
	}
	defer resp.Body.Close()

	return nil
}

// CheckAuthBackend(非clinet方便作为参数) 请求蓝盾后台校验用户是否具有工作空间权限
func CheckAuthBackend(ctx context.Context, host, wsid, ticket string) (bool, error) {
	url := fmt.Sprintf("%s/remotedev/api/user/workspaces/checkPermission?workspaceName=%s", host, wsid)
	req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return false, err
	}
	req.Header.Set("X-DEVOPS-PROJECT-ID", ParseWsId2UserProjectId(wsid))
	req.AddCookie(&http.Cookie{
		Name:  "bk_ticket",
		Value: ticket,
	})

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return false, errors.Wrap(err, "requset checkAuth error")
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)

	result, err := IntoDevopsResult[bool](body)
	if err != nil {
		return false, errors.Wrap(err, "parse remoting GetWorkspaceDetail result error")
	}

	return *result, nil
}

// GetImageSpecConfig 获取工作空间镜像信息
func (r *RemoteDevClient) GetImageSpecConfig(ctx context.Context, workspaceId string) (*ImageSpecConfigResp, error) {
	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/workspace/image/spec?workspaceName=%s&timestamp=%s", r.host, workspaceId, timestamp)
	request, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	r.setRemotedevHeader(request, workspaceId, workspaceId+timestamp)
	if err != nil {
		return nil, err
	}

	resp, err := r.client.Do(request)
	if err != nil {
		return nil, errors.Wrap(err, "requset remoting GetImageSpecConfig error")
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)

	result, err := IntoDevopsResult[ImageSpecConfigResp](body)
	if err != nil {
		return nil, errors.Wrap(err, "parse remoting GetImageSpecConfig result error")
	}

	return result, nil
}

type ImageSpecConfigResp struct {
	BaseRef      string          `json:"baseRef,omitempty"`
	IdeRef       string          `json:"ideRef,omitempty"`
	ContentLayer []*ContentLayer `json:"contentLayer,omitempty"`
	RemotingRef  string          `json:"remotingRef,omitempty"`
	IdeLayerRef  []string        `json:"ideLayerRef,omitempty"`
}

type ContentLayer struct {
	Remote *RemoteContentLayer `json:"remote"`
	Direct *DirectContentLayer `json:"direct"`
}

type RemoteContentLayer struct {
	Url       string `json:"url,omitempty"`
	Digest    string `json:"digest,omitempty"`
	DiffId    string `json:"diffId,omitempty"`
	MediaType string `json:"mediaType,omitempty"`
	Size      int64  `json:"size,omitempty"`
}

type DirectContentLayer struct {
	Content []byte `json:"content,omitempty"`
}

func hmacsha1Encrypt(keyStr, content string) string {
	key := []byte(keyStr)
	mac := hmac.New(sha1.New, key)
	mac.Write([]byte(content))
	//进行base64编码
	res := mac.Sum(nil)
	return fmt.Sprintf("%x", res)
}
