# -*- coding: utf-8 -*-

from __future__ import print_function
from __future__ import absolute_import
from __future__ import unicode_literals

from .python_atom_sdk import *
sdk = AtomSDK()


def main():
    """
    @summary: main
    """
    sdk.log.info("enter main")

    # 输入
    input_params = sdk.get_input()
    sdk.log.info(input_params)

    # 插件逻辑
    sdk.log.info("hello")

    # 插件执行结果、输出数据
    output_data = {
        "status": sdk.status.SUCCESS,
        "message": "run succ",
        "type": sdk.output_template_type.DEFAULT,
        "data": {
            "code_git_mr_review_status": {
                "type": sdk.output_field_type.STRING,
                "value": "test output"
            }
        }
    }
    sdk.set_output(output_data)

    sdk.log.info("finish")

    exit(0)
