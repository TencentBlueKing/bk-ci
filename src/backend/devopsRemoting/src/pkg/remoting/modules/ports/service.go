package ports

import (
	"devopsRemoting/src/pkg/remoting/types"
	"errors"
	"io"

	"github.com/gin-gonic/gin"
)

func (m *PortsManager) PortStatus(c *gin.Context, observe bool) error {
	if !observe {
		c.Stream(func(_ io.Writer) bool {
			c.SSEvent("message", &types.PortsStatusResponse{
				Ports: m.Status(),
			})
			return false
		})
	}

	sub, err := m.Subscribe()
	if err == ErrTooManySubscriptions {
		return errors.New("too many subscriptions")
	}
	if err != nil {
		return err
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

	return nil
}
