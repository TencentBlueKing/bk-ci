package api

import (
	"devopsRemoting/src/pkg/remoting/service"

	"github.com/gin-gonic/gin"
)

func initCommandsApi(r *gin.RouterGroup) {
	r.GET("/status", getCommandStatus)
}

func getCommandStatus(c *gin.Context) {
	select {
	case <-c.Request.Cancel:
		return
	case <-service.ApiService.CommandManager.Ready:
	}

	ok(c, service.ApiService.CommandManager.Status())
}
