package api

import (
	"github.com/gin-gonic/gin"
)

func initRemotingApi(r *gin.RouterGroup) {
	r.GET("/status", status)
}

func status(c *gin.Context) {
	result := true
	ok(c, result)
}
