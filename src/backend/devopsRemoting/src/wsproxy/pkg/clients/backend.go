package clients

import (
	"common/logs"
	"context"
	"crypto/hmac"
	"crypto/sha1"
	"fmt"
	"io/ioutil"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
)

type BackendClient struct {
	client    *http.Client
	urlPrefix string
	sha1key   string
}

func NewBackendClient(hostname, sha1key string) (*BackendClient, error) {
	return &BackendClient{
		client:    &http.Client{},
		urlPrefix: hostname,
		sha1key:   sha1key,
	}, nil
}

func (b *BackendClient) SetRemotedevHeader(request *http.Request, workspaceId, timestamp string) {
	request.Header.Set("Content-Type", "application/json; charset=UTF-8")
	request.Header.Set("accept", "application/json")
	// 加密
	request.Header.Set("X-Signature", hmacsha1Encrypt(b.sha1key, workspaceId+timestamp))
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

func (b *BackendClient) SendHeartbeat(ctx context.Context, workspaceId string) error {
	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/workspace/heartbeat?workspaceName=%s&timestamp=%s", b.urlPrefix, workspaceId, timestamp)
	logs.Debugf("SendHeartbeat url: %s", url)
	request, err := http.NewRequestWithContext(ctx, "POST", url, nil)
	if err != nil {
		return err
	}
	b.SetRemotedevHeader(request, workspaceId, timestamp)

	resp, err := b.client.Do(request)
	if err != nil {
		return errors.Wrap(err, "requset remoting SendHeartbeat error")
	}
	defer resp.Body.Close()

	return nil
}

type BackendWorkspaceDetail struct {
	WorkspaceName string `json:"workspaceName"`
	PodIp         string `json:"podIp"`
	SSHKey        string `json:"sshKey"`
}

func (b *BackendClient) GetWorkspaceDetail(ctx context.Context, workspaceId string) (*BackendWorkspaceDetail, error) {
	timestamp := strconv.FormatInt(time.Now().UnixMilli(), 10)
	url := fmt.Sprintf("%s/remotedev/api/remotedev/workspace-proxy/detail?workspaceName=%s&timestamp=%s", b.urlPrefix, workspaceId, timestamp)
	logs.Debugf("GetWorkspaceDetail url: %s", url)
	request, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return nil, err
	}
	b.SetRemotedevHeader(request, workspaceId, timestamp)

	resp, err := b.client.Do(request)
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

// CheckAuth 请求蓝盾后台校验用户是否具有工作空间权限
func CheckAuthBackend(ctx context.Context, host, wsid, ticket string) (bool, error) {
	url := fmt.Sprintf("%s/remotedev/api/user/workspaces/checkPermission?workspaceName=%s", host, wsid)
	req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return false, err
	}
	req.Header.Set("X-DEVOPS-PROJECT-ID", "grayproject")
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

func hmacsha1Encrypt(keyStr, content string) string {
	key := []byte(keyStr)
	mac := hmac.New(sha1.New, key)
	mac.Write([]byte(content))
	//进行base64编码
	res := mac.Sum(nil)
	return fmt.Sprintf("%x", res)
}
