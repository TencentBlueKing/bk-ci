/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package dashboard

import (
	"net/http"

	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"

	"github.com/gobuffalo/packr/v2"
)

// RegisterStaticServer add the static server to router
func RegisterStaticServer(svr *httpserver.HTTPServer) error {
	box := packr.New("controller_box", "../../../dashboard/static/controller")

	svr.GetWebContainer().Handle("/", http.FileServer(box))

	return nil
}
