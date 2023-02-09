package check

import (
	"github.com/pkg/errors"
	"github.com/spf13/cobra"
)

// NoArgs 校验没有参数的命令.
func NoArgs(cmd *cobra.Command, args []string) error {
	if len(args) > 0 {
		return errors.Errorf(
			"%q 不需要参数\n\nUsage:  %s",
			cmd.CommandPath(),
			cmd.UseLine(),
		)
	}
	return nil
}

// ExactArgs 校验需要指定参数
func ExactArgs(n int) cobra.PositionalArgs {
	return func(cmd *cobra.Command, args []string) error {
		if len(args) != n {
			return errors.Errorf(
				"%q 需要 %d 个参数\n\nUsage:  %s",
				cmd.CommandPath(),
				n,
				cmd.UseLine(),
			)
		}
		return nil
	}
}

// MinimumNArgs 校验最小参数
func MinimumNArgs(n int) cobra.PositionalArgs {
	return func(cmd *cobra.Command, args []string) error {
		if len(args) < n {
			return errors.Errorf(
				"%q 需要最少 %d 个参数\n\nUsage:  %s",
				cmd.CommandPath(),
				n,
				cmd.UseLine(),
			)
		}
		return nil
	}
}

// MaximumNArgs 校验最大参数数量
func MaximumNArgs(n int) cobra.PositionalArgs {
	return func(cmd *cobra.Command, args []string) error {
		if len(args) > n {
			return errors.Errorf(
				"%q 最多需要 %d 个参数\n\nUsage:  %s",
				cmd.CommandPath(),
				n,
				cmd.UseLine(),
			)
		}
		return nil
	}
}
