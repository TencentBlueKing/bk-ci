package api

import (
	"remoting/pkg/api/errorcode"
	"remoting/pkg/service"

	"github.com/gin-gonic/gin"
)

func initPortsApi(r *gin.RouterGroup) {
	r.GET("/status", portsStatus)
}

func portsStatus(c *gin.Context) {
	observe := c.Query("observe")
	observeB := false
	if observe == "true" {
		observeB = true
	}

	ports, err := service.ApiService.Ports.PortStatus(c, observeB)
	if err != nil {
		okFail(c, errorcode.UserError, err)
		return
	}

	if !observeB {
		ok(c, ports)
	}
}
