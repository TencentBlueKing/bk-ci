package api

import (
	"errors"
	"net/http"
	"remoting/pkg/service"

	"github.com/gin-gonic/gin"
)

func initTokenApi(r *gin.RouterGroup) {
	r.POST("/updateBkTicket", updateBkTicket)
}

func updateBkTicket(c *gin.Context) {
	var ticket *service.BkticktRequestBody = &service.BkticktRequestBody{}

	err := c.ShouldBindJSON(ticket)
	if err != nil {
		okFail(c, http.StatusBadRequest, errors.New("not ticket"))
		return
	}

	if err := service.ApiService.Token.UpdateBkTicket(c.Request.Context(), ticket); err != nil {
		okFail(c, http.StatusInternalServerError, err)
		return
	}

	ok(c, true)
}
