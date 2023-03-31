package registry

import (
	"common/logs"
	"net/http"

	"github.com/hashicorp/go-retryablehttp"
	"github.com/sirupsen/logrus"
)

type Option func(opts *httpOpts)

func NewRetryableHTTPClient(options ...Option) *http.Client {
	opts := defaultOptions()
	for _, o := range options {
		o(&opts)
	}

	client := retryablehttp.NewClient()
	client.RetryMax = opts.RetryMax
	client.Logger = opts.Logger
	client.RequestLogHook = opts.RequestLogHook
	client.ResponseLogHook = opts.ResponseLogHook

	if opts.HTTPClient != nil {
		client.HTTPClient = opts.HTTPClient
	}

	return client.StandardClient()
}

type httpOpts struct {
	HTTPClient *http.Client
	Logger     retryablehttp.LeveledLogger

	RetryMax int

	RequestLogHook retryablehttp.RequestLogHook

	ResponseLogHook retryablehttp.ResponseLogHook
}

func defaultOptions() httpOpts {
	return httpOpts{
		RetryMax: 5,
		Logger:   retryablehttp.LeveledLogger(&leveledLogrus{logs.Logs}),

		RequestLogHook: func(_ retryablehttp.Logger, req *http.Request, attempt int) {
			if attempt > 0 {
				logs.Warnf("%v %v request failed. Retry count: %v", req.Method, req.URL, attempt)
			}
		},
	}
}

// WithRequestLogHook can be used to configure a custom request log hook.
func WithRequestLogHook(hook retryablehttp.RequestLogHook) Option {
	return func(opts *httpOpts) {
		opts.RequestLogHook = hook
	}
}

func WithHTTPClient(client *http.Client) Option {
	return func(opts *httpOpts) {
		opts.HTTPClient = client
	}
}

type leveledLogrus struct {
	*logrus.Entry
}

func (l *leveledLogrus) fields(keysAndValues ...interface{}) map[string]interface{} {
	fields := make(map[string]interface{})

	for i := 0; i < len(keysAndValues)-1; i += 2 {
		fields[keysAndValues[i].(string)] = keysAndValues[i+1]
	}

	return fields
}

func (l *leveledLogrus) Error(msg string, keysAndValues ...interface{}) {
	l.WithFields(l.fields(keysAndValues...)).Error(msg)
}

func (l *leveledLogrus) Info(msg string, keysAndValues ...interface{}) {
	l.WithFields(l.fields(keysAndValues...)).Info(msg)
}

func (l *leveledLogrus) Debug(msg string, keysAndValues ...interface{}) {
	l.WithFields(l.fields(keysAndValues...)).Debug(msg)
}

func (l *leveledLogrus) Warn(msg string, keysAndValues ...interface{}) {
	l.WithFields(l.fields(keysAndValues...)).Warn(msg)
}
