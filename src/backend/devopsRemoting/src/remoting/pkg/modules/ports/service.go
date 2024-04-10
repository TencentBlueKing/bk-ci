package ports

import (
	"errors"
	"io"
	"remoting/pkg/types"

	"github.com/gin-gonic/gin"
)

func (m *PortsManager) PortStatus(c *gin.Context, observe bool) (*types.PortsStatusResponse, error) {
	if !observe {
		return &types.PortsStatusResponse{
			Ports: m.Status(),
		}, nil
	}

	sub, err := m.Subscribe()
	if err == ErrTooManySubscriptions {
		return nil, errors.New("too many subscriptions")
	}
	if err != nil {
		return nil, err
	}
	defer sub.Close()

	c.Stream(func(_ io.Writer) bool {
		for {
			select {
			case <-c.Done():
				return false
			case update := <-sub.Updates():
				if update == nil {
					return false
				}
				c.SSEvent("message", &types.PortsStatusResponse{
					Ports: update,
				})
				return true
			}
		}
	})

	return nil, nil
}
