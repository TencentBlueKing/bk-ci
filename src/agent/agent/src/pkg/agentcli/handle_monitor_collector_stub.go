//go:build loong64
// +build loong64

package agentcli

import (
	"context"
	"fmt"
	"io"
	"time"
)

func runCollectorOnceStdout(ctx context.Context, out io.Writer, duration time.Duration) error {
	_ = ctx
	_ = duration
	fmt.Fprintln(out, "# collector (telegraf) disabled in this build (loong64/out tag)")
	return nil
}
