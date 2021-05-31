#!/bin/bash

rm -rf /data/bkce/codecc/frontend

cp -rf codecc/frontend /data/bkce/codecc

sh codecc/scripts/codecc/codecc_render_tpl -m codecc/frontend codecc/frontend/index.html

ls -l /data/bkce/codecc/frontend