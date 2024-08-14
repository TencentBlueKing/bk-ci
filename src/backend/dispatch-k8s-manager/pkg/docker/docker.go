package docker

import (
	"encoding/base64"
	"encoding/json"
	"io"
	"strings"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/client"
	"github.com/pkg/errors"
	"golang.org/x/net/context"
)

var cli *client.Client

func InitDockerCli() error {
	c, err := client.NewClientWithOpts(client.FromEnv, client.WithAPIVersionNegotiation())
	if err != nil {
		return err
	}
	cli = c

	return nil
}

func ImageList(ctx context.Context) ([]types.ImageSummary, error) {
	images, err := cli.ImageList(ctx, types.ImageListOptions{})
	if err != nil {
		return nil, errors.Wrap(err, "list image error")
	}

	return images, nil
}

func ImagePull(
	ctx context.Context,
	ref string,
	username string,
	password string,
) (string, error) {
	imageName := strings.TrimSpace(ref)

	reader, err := cli.ImagePull(ctx, imageName, types.ImagePullOptions{
		RegistryAuth: generateDockerAuth(username, password),
	})
	if err != nil {
		return "", errors.Wrap(err, "pull new image error")
	}
	defer reader.Close()
	buf := new(strings.Builder)
	_, _ = io.Copy(buf, reader)

	return buf.String(), nil
}

func ImageInspect(ctx context.Context, imageId string) (*types.ImageInspect, error) {
	image, _, err := cli.ImageInspectWithRaw(ctx, imageId)
	if err != nil {
		return nil, errors.Wrap(err, "image inspect error")
	}

	return &image, nil
}

func ImageRemove(ctx context.Context, imageId string, opts types.ImageRemoveOptions) error {
	_, err := cli.ImageRemove(ctx, imageId, opts)
	if err != nil {
		return err
	}

	return nil
}

// generateDockerAuth 创建拉取docker凭据
func generateDockerAuth(user, password string) string {
	if user == "" || password == "" {
		return ""
	}

	authConfig := types.AuthConfig{
		Username: user,
		Password: password,
	}
	encodedJSON, err := json.Marshal(authConfig)
	if err != nil {
		panic(err)
	}

	return base64.URLEncoding.EncodeToString(encodedJSON)
}
