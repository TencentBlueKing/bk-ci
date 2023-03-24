package clients

import (
	"common/devops"
	"encoding/json"

	"github.com/pkg/errors"
)

var (
	Remoting *RemotingClient
)

func init() {
	Remoting = NewRemotingClient()
}

func IntoDevopsResult[T any](body []byte) (*T, error) {
	res := &devops.DevopsHttpResult{}
	err := json.Unmarshal(body, res)
	if err != nil {
		return nil, errors.Wrap(err, "parse devops result error")
	}

	if res.Status != 0 {
		return nil, errors.Errorf("devops result status error %s", res.Message)
	}

	data, err := json.Marshal(res.Data)
	if err != nil {
		return nil, errors.Wrap(err, "marshal davops result data error")
	}

	result := new(T)
	err = json.Unmarshal(data, result)
	if err != nil {
		return nil, errors.Wrap(err, "parse devops result data error")
	}

	return result, err
}
