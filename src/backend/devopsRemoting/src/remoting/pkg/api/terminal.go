package api

import (
	"errors"
	"net/http"
	"remoting/pkg/api/errorcode"
	"remoting/pkg/service"

	"github.com/gin-gonic/gin"
)

func initTerminalApi(r *gin.RouterGroup) {
	r.GET("/list", listTerminal)
	r.GET("/listen", listenTerminal)
	r.POST("/close", closeTerminal)
}

func listTerminal(c *gin.Context) {
	ok(c, service.ApiService.TermSrv.List())
}

func listenTerminal(c *gin.Context) {
	alias := c.Query("alias")
	if alias == "" {
		okFail(c, http.StatusBadRequest, errors.New("query param alias is null"))
		return
	}

	if err := service.ApiService.TermSrv.Listen(c, alias); err != nil {
		okFail(c, errorcode.UserError, err)
		return
	}
}

func closeTerminal(c *gin.Context) {
	alias := c.Query("alias")
	if alias == "" {
		okFail(c, http.StatusBadRequest, errors.New("query param alias is null"))
		return
	}

	if err := service.ApiService.TermSrv.Close(c, alias); err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, true)
}
