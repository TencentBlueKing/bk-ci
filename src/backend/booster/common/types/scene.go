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
	if scene == "" {
		return projectID
	}

	return fmt.Sprintf(projectIDWithScene, projectID, scene)
}
