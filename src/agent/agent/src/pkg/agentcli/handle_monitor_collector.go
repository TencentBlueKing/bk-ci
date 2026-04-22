//go:build !loong64
// +build !loong64

package agentcli

import (
	"context"
	"io"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/collector"
)

func runCollectorOnceStdout(ctx context.Context, out io.Writer, duration time.Duration) error {
	return collector.RunOnceStdout(ctx, out, duration)
}
