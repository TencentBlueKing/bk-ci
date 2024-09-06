package service

import (
	"context"
	"disaptch-k8s-manager/pkg/db/mysql"
	"disaptch-k8s-manager/pkg/docker"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/task"
	"disaptch-k8s-manager/pkg/types"
	"encoding/json"
	"strings"
	"time"

	dockerTypes "github.com/docker/docker/api/types"
	"github.com/pkg/errors"
)

func DockerInspect(info *DockerInspectInfo) (string, error) {
	taskId := generateTaskId()

	if err := mysql.InsertTask(types.Task{
		TaskId:     taskId,
		TaskKey:    info.Name,
		TaskBelong: types.TaskBelongDocker,
		Action:     types.TaskDockerActionInspect,
		Status:     types.TaskWaiting,
		Message:    nil,
		ActionTime: time.Now(),
		UpdateTime: time.Now(),
	}); err != nil {
		return "", err
	}

	go inspect(taskId, info)

	return taskId, nil
}

func inspect(taskId string, info *DockerInspectInfo) {
	task.UpdateTask(taskId, types.TaskRunning)

	ctx := context.Background()

	// 拉取镜像
	pullMsg, err := docker.ImagePull(ctx, info.Ref, info.Credential.Username, info.Credential.Password)
	if err != nil {
		logs.Error("inspect ImagePull error", err)
		task.FailTask(taskId, err.Error())
		return
	}

	// 寻找ID
	imageName := strings.TrimSpace(info.Ref)
	imageStr := strings.TrimPrefix(strings.TrimPrefix(imageName, "http://"), "https://")
	images, err := docker.ImageList(ctx)
	if err != nil {
		logs.Error("get image list error", err)
		task.FailTask(taskId, err.Error())
		return
	}
	id := ""
	for _, image := range images {
		for _, tagName := range image.RepoTags {
			if tagName == imageStr {
				id = image.ID
			}
		}
	}
	if id == "" {
		err = errors.Errorf("image %s not found", imageName)
		logs.Errorf("pullMsg %s error %s", pullMsg, err.Error())
		task.FailTask(taskId, err.Error())
		return
	}

	defer func() {
		// 完事后删除镜像
		if err = docker.ImageRemove(ctx, id, dockerTypes.ImageRemoveOptions{Force: true}); err != nil {
			logs.Errorf("remove image %s id %s error %s", info.Ref, id, err.Error())
		}
	}()

	// 分析镜像
	image, err := docker.ImageInspect(ctx, info.Ref)
	if err != nil {
		logs.Error("inspect ImageInspect error", err)
		task.FailTask(taskId, err.Error())
		return
	}

	msg := &DockerInspectResp{
		Architecture: image.Architecture,
		Os:           image.Os,
		Size:         image.Size,
		Created:      image.Created,
		Id:           image.ID,
		Author:       image.Author,
		Parent:       image.Parent,
		OsVersion:    image.OsVersion,
	}

	msgStr, err := json.Marshal(msg)
	if err != nil {
		logs.Error("inspect jsonMarshal error", err)
		task.FailTask(taskId, err.Error())
		return
	}

	task.OkTaskWithMessage(taskId, string(msgStr))

}
