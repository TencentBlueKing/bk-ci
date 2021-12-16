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

// RegisterStaticServer register the static server into router
func RegisterStaticServer(svr *httpserver.HTTPServer) error {
	box := packr.New("dashboard_box", "../../static/stats")

	svr.GetWebContainer().Handle("/", http.FileServer(box))

	return nil
}
