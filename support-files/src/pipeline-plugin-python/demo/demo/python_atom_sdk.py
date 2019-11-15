# -*- coding: utf-8 -*-

import os
import json
import codecs
import sys
import logging

# ===================== setting ==========================

# 输出输出位置和文件名存储的环境变量名称
BK_DATA_DIR = "bk_data_dir"
BK_DATA_INPUT = "bk_data_input"
BK_DATA_OUTPUT = "bk_data_output"

#  插件输出状态
BK_ATOM_STATUS = {
    "SUCCESS": "success",
    "FAILURE": "failure",
    "ERROR": "error"
}

#  插件输出模版类型
BK_OUTPUT_TEMPLATE_TYPE = {
    "DEFAULT": "default",
    "QUALITY": "quality"
}

#  插件输出字段类型
BK_OUTPUT_FIELD_TYPE = {
    "STRING": "string",
    "ARTIFACT": "artifact",
    "REPORT": "report"
}

# 插件输出为报告时，报告类型
BK_OUTPUT_REPORT_TYPR = {
    "INTERNAL": "INTERNAL",
    "THIRDPARTY": "THIRDPARTY"
}

# ========================================================

# ===================== const ============================


class Status:
    """
    @summary:  插件执行结果定义
    """
    ERROR = BK_ATOM_STATUS.get("ERROR", None)
    FAILURE = BK_ATOM_STATUS.get("FAILURE", None)
    SUCCESS = BK_ATOM_STATUS.get("SUCCESS", None)


class OutputTemplateType:
    """
    @summary:  插件输出模版类型
    """
    DEFAULT = BK_OUTPUT_TEMPLATE_TYPE.get("DEFAULT", None)
    QUALITY = BK_OUTPUT_TEMPLATE_TYPE.get("QUALITY", None)


class OutputFieldType:
    """
    @summary:  插件输出字段类型
    """
    STRING = BK_OUTPUT_FIELD_TYPE.get("STRING", None)
    ARTIFACT = BK_OUTPUT_FIELD_TYPE.get("ARTIFACT", None)
    REPORT = BK_OUTPUT_FIELD_TYPE.get("REPORT", None)


class OutputReportType:
    """
    @summary:  插件输出字段类型为报告时，报告类型
    """
    INTERNAL = BK_OUTPUT_REPORT_TYPR.get("INTERNAL", None)
    THIRDPARTY = BK_OUTPUT_REPORT_TYPR.get("THIRDPARTY", None)

# ========================================================

# ===================== logger ===========================

LOG_NAME = "ATOM_LOG"
LOG_FORMAT = "[%(levelname)s]: %(message)s"
LOG_LEVEL = logging.DEBUG


def getLogger():
    logger = logging.getLogger(LOG_NAME)
    if logger.handlers:
        return logger
    logger.setLevel(LOG_LEVEL)
    formatter = logging.Formatter(LOG_FORMAT)

    console = logging.StreamHandler(sys.stdout)
    console.setFormatter(formatter)

    logger.addHandler(console)

    return logger

# ========================================================

# ===================== input ============================


class ParseParams():
    """
    @summary: 获取 插件入参
    """

    _log = getLogger()

    def __init__(self):
        self.data_path = os.getenv(BK_DATA_DIR, '.')
        self.input_file_name = os.getenv(BK_DATA_INPUT, 'input.json')

    def get_input(self):
        """
        @summary: 获取 插件输入参数
        @return dict
        """
        input_file_path = os.path.join(self.data_path, self.input_file_name)
        if os.path.exists(input_file_path):
            if sys.version_info.major == 2:
                with codecs.open(input_file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                    return json.loads(content)
            else:
                with open(input_file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                    return json.loads(content)

        return {}

# ========================================================

# ===================== output ===========================


class SetOutput():
    """
    @summary: 设置 插件输出
    """

    _log = getLogger()

    def __init__(self):
        self.data_path = os.getenv(BK_DATA_DIR, '.')
        self.output_file_name = os.getenv(BK_DATA_OUTPUT, 'output.json')

    def check_output(self, output):
        """
        @summary: 检查 插件输出是否合法
        """
        status = output.get("status", None)
        if not status or status not in BK_ATOM_STATUS.values():
            self._log.error("[check output error]invalid status:{}".format(status))
            exit(-1)

        output_template_type = output.get("type", None)
        if not output_template_type or output_template_type not in BK_OUTPUT_TEMPLATE_TYPE.values():
            self._log.error("[check output error]invalid output_template_type:{}".format(output_template_type))
            exit(-1)

        output_data = output.get("data", {})
        for k, v in output_data.items():
            field_type = v.get("type", None)
            if field_type == BK_OUTPUT_FIELD_TYPE.get("STRING", ""):
                pass
            elif field_type == BK_OUTPUT_FIELD_TYPE.get("ARTIFACT", ""):
                field_value = v.get("value", [])
                if not isinstance(field_value, list):
                    self._log.error("[check output error]invalid field[{}], should be list".format(k))
                    exit(-1)
                for file_path in field_value:
                    if not os.path.exists(file_path):
                        self._log.error("[check output error]invalid field[{}], not exists[{}]".format(k, file_path))
                        exit(-1)
            elif field_type == BK_OUTPUT_FIELD_TYPE.get("REPORT", ""):
                pass
            else:
                self._log.error("[check output error]invalid field type: {}".format(field_type))
                exit(-1)

        return

    def set_output(self, output):
        """
        @summary: 设置 插件执行结果、输出参数
        @param output: 输出参数和执行结果dict
        """
        self.check_output(output)

        output_file_path = os.path.join(self.data_path, self.output_file_name)
        if not os.path.exists(self.data_path):
            try:
                os.mkdir(self.data_path)
            except FileExistsError:
                self._log.debug("mkdir data_path error")
                pass

        with open(output_file_path, 'w') as f:
            json.dump(output, f)


# ========================================================

# ===================== sdk method =======================


class AtomSDK():

    def __init__(self):
        self.log = getLogger()
        self.parseParamsObj = ParseParams()
        self.params = self.parseParamsObj.get_input()
        self.status = Status()
        self.output_template_type = OutputTemplateType()
        self.output_field_type = OutputFieldType()
        self.output_report_type = OutputReportType()

    def get_input(self):
        """
        @summary: 获取 插件输入参数
        @return dict
        """
        return self.params

    def get_project_name(self):
        return self.params.get("project.name", None)

    def get_project_name_cn(self):
        return self.params.get("project.name.chinese", None)

    def get_pipeline_id(self):
        return self.params.get("pipeline.id", None)

    def get_pipeline_name(self):
        return self.params.get("pipeline.name", None)

    def get_pipeline_build_id(self):
        return self.params.get("pipeline.build.id", None)

    def get_pipeline_build_num(self):
        return self.params.get("pipeline.build.num", None)

    def get_pipeline_start_type(self):
        return self.params.get("pipeline.start.type", None)

    def get_pipeline_start_user_id(self):
        return self.params.get("pipeline.start.user.id", None)

    def get_pipeline_start_user_name(self):
        return self.params.get("pipeline.start.user.name", None)

    def get_pipeline_creator(self):
        return self.params.get("BK_CI_PIPELINE_CREATE_USER", None)

    def get_pipeline_modifier(self):
        return self.params.get("BK_CI_PIPELINE_UPDATE_USER", None)

    def get_pipeline_time_start_mills(self):
        return self.params.get("pipeline.time.start", None)

    def get_pipeline_version(self):
        return self.params.get("pipeline.version", None)

    def get_workspace(self):
        return self.params.get("bkWorkspace", None)

    def get_test_version_flag(self):
        """
        @summary: 当前插件是否是测试版本标识
        """
        return self.params.get("testVersionFlag", None)

    def get_sensitive_conf(self, key):
        confJson = self.params.get("bkSensitiveConfInfo", None)
        if confJson:
            return confJson.get(key, None)
        else:
            return None

    def set_output(self, output):
        """
        @summary: 设置输出
        """
        setOutput = SetOutput()
        setOutput.set_output(output)

# ========================================================
