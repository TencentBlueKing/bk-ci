/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import "fmt"

const (
	ProjectIDBlockSep = "_"

	projectIDWithScene = "%s" + ProjectIDBlockSep + "%s"
)

// GetProjectIDWithScene return the real projectID
// If scene is empty, just return projectID which is given
// else, return the string made up with both projectID and scene
func GetProjectIDWithScene(projectID, scene string) string {
	if scene == "" || projectID == "" {
		return projectID
	}

	return fmt.Sprintf(projectIDWithScene, projectID, scene)
}
