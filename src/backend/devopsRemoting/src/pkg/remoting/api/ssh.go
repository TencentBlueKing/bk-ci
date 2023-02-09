package api

import (
	"devopsRemoting/src/pkg/remoting/service"
	"net/http"

	"github.com/gin-gonic/gin"
)

func initSSHApi(r *gin.RouterGroup) {
	r.POST("/createKey", createSSHKey)
}

func createSSHKey(c *gin.Context) {
	resp, err := service.ApiService.SSH.CreateSSHKeyPair()
	if err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}
	ok(c, resp)
}
