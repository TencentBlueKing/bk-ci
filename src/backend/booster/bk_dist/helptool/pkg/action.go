package pkg

import (
	"fmt"
	"path/filepath"

	commandCli "github.com/urfave/cli"
)

// Action return command actions
func Action(c *commandCli.Context) error {
	return action(c)
}

func action(c *commandCli.Context) error {
	switch c.Command.Name {
	case CommandDownload:
		return doDownload(c)
	case CommandMd5sum:
		return doMd5sum(c)
	case CommandUnzip:
		return doUnzip(c)
	default:
		return fmt.Errorf("unknown command[%s]", c.Command.Name)
	}
}

func doDownload(c *commandCli.Context) error {
	url := ""
	if c.IsSet(FlagURL) {
		url = c.String(FlagURL)
	} else {
		return fmt.Errorf("http url must be specified to download")
	}

	file := ""
	if c.IsSet(FlagFile) {
		file, _ = filepath.Abs(c.String(FlagFile))
	} else {
		return fmt.Errorf("file name must be specified to save")
	}

	return downloadFile(file, url)
}

func doMd5sum(c *commandCli.Context) error {
	file := ""
	if c.IsSet(FlagFile) {
		file, _ = filepath.Abs(c.String(FlagFile))
	} else {
		return fmt.Errorf("file name must be specified to md5sum")
	}

	md5str, err := md5sum(file)
	if err == nil {
		fmt.Printf("%s\n", md5str)
	}

	return err
}

func doUnzip(c *commandCli.Context) error {
	file := ""
	if c.IsSet(FlagFile) {
		file, _ = filepath.Abs(c.String(FlagFile))
	} else {
		return fmt.Errorf("zip file must be specified")
	}

	dir := ""
	if c.IsSet(FlagDir) {
		dir, _ = filepath.Abs(c.String(FlagDir))
	} else {
		return fmt.Errorf("directory must be specified to save unzip files")
	}

	_, err := unzip(file, dir)
	return err
}
