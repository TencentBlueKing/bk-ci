package clients

import (
	"context"
	apitypes "devopsRemoting/src/pkg/remoting-api/types"
	"devopsRemoting/src/pkg/wsproxy/constant"
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/pkg/errors"
)

// 封装了 devops-remoting的Api

type RemotingClient struct {
	client *http.Client
}

func NewRemotingClient() *RemotingClient {
	return &RemotingClient{
		client: &http.Client{},
	}
}

func (c *RemotingClient) CreateSSHKeyPair(ctx context.Context, ip string) (*apitypes.CreateSSHKeyPairResp, error) {
	url := fmt.Sprintf("http://%s:%d/_remoting/api/ssh/createKey", ip, constant.RemotingApiPort)
	request, err := http.NewRequestWithContext(ctx, "POST", url, nil)
	request.Header.Set("Content-Type", "application/json; charset=UTF-8")
	if err != nil {
		return nil, err
	}
	request.WithContext(ctx)

	resp, err := c.client.Do(request)
	if err != nil {
		return nil, errors.Wrap(err, "requset remoting CreateSSHKeyPair error")
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)

	result, err := IntoDevopsResult[apitypes.CreateSSHKeyPairResp](body)
	if err != nil {
		return nil, errors.Wrap(err, "parse remoting CreateSSHKeyPair result error")
	}

	return result, nil
}
