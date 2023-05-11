package registry

import (
	"context"
	"errors"
	"io"
	"net/http"
	"testing"

	"github.com/containerd/containerd/remotes"
	"github.com/containerd/containerd/remotes/docker"
	"github.com/google/go-cmp/cmp"
	"github.com/hashicorp/go-retryablehttp"
	"github.com/opencontainers/go-digest"
)

func TestRetryableFetcher(t *testing.T) {
	type Expectation struct {
		Digest string
		Error  string
	}

	tests := []struct {
		Name        string
		Ref         string
		Retries     bool
		Expectation Expectation
	}{
		{
			Name:        "valid reference",
			Ref:         "docker.io/library/alpine@sha256:7580ece7963bfa863801466c0a488f11c86f85d9988051a9f9c68cb27f6b7872",
			Retries:     true,
			Expectation: Expectation{Digest: "sha256:7580ece7963bfa863801466c0a488f11c86f85d9988051a9f9c68cb27f6b7872"},
		},
		{
			Name:        "invalid reference",
			Ref:         "docker.io/library/invalid-alpine",
			Retries:     false,
			Expectation: Expectation{Error: "object required"},
		},
	}

	for _, test := range tests {
		t.Run(test.Name, func(t *testing.T) {
			var retries int
			resolverFactory := func() remotes.Resolver {
				client := NewRetryableHTTPClient(
					WithHTTPClient(
						&http.Client{
							Transport: &failFirstErrorRoundTrip{rt: http.DefaultTransport},
						},
					),
					WithRequestLogHook(func(_ retryablehttp.Logger, _ *http.Request, attempt int) {
						retries = attempt
					}),
				)

				resolverOpts := docker.ResolverOptions{
					Client: client,
				}

				return docker.NewResolver(resolverOpts)
			}

			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()

			resolver := resolverFactory()

			_, desc, err := resolver.Resolve(ctx, test.Ref)
			if err != nil {
				if !test.Retries && retries == 0 {
					return
				}

				t.Fatalf("cannot download ref: %+q", err)
			}

			fetcher, err := resolver.Fetcher(context.Background(), test.Ref)
			if err != nil {
				t.Error(err)
			}

			reader, err := fetcher.Fetch(ctx, desc)
			if err != nil {
				t.Error(err)
			}

			var (
				outcome Expectation
			)

			data, err := io.ReadAll(reader)
			if err != nil {
				outcome.Error = err.Error()
			}

			outcome.Digest = digest.FromBytes(data).String()

			if diff := cmp.Diff(test.Expectation, outcome); diff != "" {
				t.Errorf("Info() mismatch (-want +got):\n%s", diff)
			}
		})
	}
}

type failFirstErrorRoundTrip struct {
	rt       http.RoundTripper
	requests int
}

func (rt *failFirstErrorRoundTrip) RoundTrip(req *http.Request) (resp *http.Response, err error) {
	if rt.requests == 0 {
		rt.requests += 1
		return nil, errors.New("connection reset by peer")
	}

	return rt.rt.RoundTrip(req)
}
