package registry

import (
	"net/http"

	"github.com/hashicorp/go-retryablehttp"
)

type httpOpts struct {
	HTTPClient *http.Client
	Logger     retryablehttp.LeveledLogger

	RetryMax int

	RequestLogHook retryablehttp.RequestLogHook

	ResponseLogHook retryablehttp.ResponseLogHook
}

type Option func(opts *httpOpts)

func NewRetryableHTTPClient(options ...Option) *http.Client {
	
}