package apiserver

import (
	"disaptch-k8s-manager/pkg/apiserver/apis"
	"disaptch-k8s-manager/pkg/apiserver/middleware"
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/logs"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/go-playground/locales/en"
	ut "github.com/go-playground/universal-translator"
	"github.com/go-playground/validator/v10"
	enTranslations "github.com/go-playground/validator/v10/translations/en"
	"github.com/pkg/errors"
	"reflect"
	"strings"
)

func InitApiServer(accessLogFile string) error {
	if err := transInit("en"); err != nil {
		return errors.Wrap(err, fmt.Sprintf("init trans failed, err:%v", err))
	}

	if !config.Envs.IsDebug {
		gin.SetMode(gin.ReleaseMode)
	}

	r := gin.Default()

	// 日志中间件，打印access
	accessLog := middleware.InitAccessLog(accessLogFile)

	// 日志中间件，添加日志接受error堆栈
	logRecovery := logs.GinRecovery(true)

	// 权限认证中间件
	auth := middleware.InitApiAuth()

	// 初始化restful接口，并添加中间件
	apis.InitApis(r, accessLog, auth, logRecovery)

	// 修改gin框架中的Validator引擎属性，实现自定制参数校验的返回信息
	if v, ok := binding.Validator.Engine().(*validator.Validate); ok {

		// 注册一个获取json tag的自定义方法
		v.RegisterTagNameFunc(func(fld reflect.StructField) string {
			name := strings.SplitN(fld.Tag.Get("json"), ",", 2)[0]
			if name == "-" {
				return ""
			}
			return name
		})
	}

	if err := r.Run(":" + config.Config.Server.Port); err != nil {
		return err
	}

	return nil
}

func transInit(local string) error {
	if v, ok := binding.Validator.Engine().(*validator.Validate); ok {
		enT := en.New() //english
		uni := ut.New(enT, enT)

		var o bool
		apis.Trans, o = uni.GetTranslator(local)
		if !o {
			return fmt.Errorf("uni.GetTranslator(%s) failed", local)
		}
		var err error
		//register translate
		// 注册翻译器
		switch local {
		default:
			err = enTranslations.RegisterDefaultTranslations(v, apis.Trans)
		}
		return err
	}
	return nil
}
