#!/bin/bash

sh scripts/codecc_render_tpl -m ci/gateway/core/vhosts codecc/gateway/vhosts/codecc.server.conf
sh scripts/codecc_render_tpl -m ci/gateway/core/vhosts codecc/gateway/vhosts/codecc.frontend.conf


ls -l /data/bkce/ci/gateway/core/vhosts