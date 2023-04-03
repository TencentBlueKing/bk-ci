package kubernetes

import (
	"common/logs"
	"context"
	"crypto/tls"
	"fmt"
	"net"
	"net/http"
	"strings"
	"time"
)

func NetworkIsReachableProbe(url string) func() error {
	logs.Infof("creating network check using URL %v", url)
	return func() error {
		tr := &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
			Proxy:           http.ProxyFromEnvironment,
		}
		client := &http.Client{
			Transport: tr,
			Timeout:   5 * time.Second,
			// never follow redirects
			CheckRedirect: func(*http.Request, []*http.Request) error {
				return http.ErrUseLastResponse
			},
		}

		resp, err := client.Get(url)
		if err != nil {
			logs.Errorf("unexpected error checking URL %v: %v", url, err)
			return err
		}
		resp.Body.Close()

		if resp.StatusCode > 499 {
			logs.WithField("url", url).WithField("statusCode", resp.StatusCode).WithError(err).Error("NetworkIsReachableProbe: unexpected status code checking URL")
			return fmt.Errorf("returned status %d", resp.StatusCode)
		}

		return nil
	}
}

func DNSCanResolveProbe(host string, timeout time.Duration) func() error {
	logs.Infof("creating DNS check for host %v", host)

	// remove port if there is one
	host = strings.Split(host, ":")[0]

	resolver := net.Resolver{}
	return func() error {
		ctx, cancel := context.WithTimeout(context.Background(), timeout)
		defer cancel()

		addrs, err := resolver.LookupHost(ctx, host)
		if err != nil {
			logs.WithField("host", host).WithError(err).Error("NetworkIsReachableProbe: unexpected error resolving host")
			return err
		}

		if len(addrs) < 1 {
			return fmt.Errorf("could not resolve host")
		}

		return nil
	}
}
