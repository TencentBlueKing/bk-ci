# -*- coding: utf-8 -*-
"""
TencentBlueKing is pleased to support the open source community by making
蓝鲸智云-权限中心Python SDK(iam-python-sdk) available.
Copyright (C) 2017-2021 THL A29 Limited, a Tencent company. All rights reserved.
Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://opensource.org/licenses/MIT
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
"""


from __future__ import unicode_literals

import argparse
import json
import os

import requests


# NOTE: the usage doc https://bk.tencent.com/docs/document/6.0/160/8388

__version__ = "1.0.0"

BK_IAM_HOST = os.getenv("BK_IAM_V3_INNER_HOST", "http://bkiam.service.consul:5001")
USE_APIGATEWAY = os.getenv("BK_IAM_USE_APIGATEWAY", "false").lower() == "true"

APP_CODE = ""
APP_SECRET = ""
data_file = ""


# =================== load json ===================
def enable_use_apigateway():
    global USE_APIGATEWAY
    USE_APIGATEWAY = True


def load_data(filename):
    """
    解析JSON数据文件
    """
    data = {}
    try:
        with open(filename) as data_file:
            data = json.load(data_file)
        print("parser json data file success!")
    except Exception as error:
        print("parser json data file error: %s" % error)
        data = {}
    return data


# =================== http request ===================


def _gen_header():
    headers = {
        "Content-Type": "application/json",
    }
    return headers


def _http_request(method, url, headers=None, data=None, timeout=None, verify=False, cert=None, cookies=None):
    try:
        if method == "GET":
            resp = requests.get(
                url=url, headers=headers, params=data, timeout=timeout, verify=verify, cert=cert, cookies=cookies
            )
        elif method == "HEAD":
            resp = requests.head(url=url, headers=headers, verify=verify, cert=cert, cookies=cookies)
        elif method == "POST":
            resp = requests.post(
                url=url, headers=headers, json=data, timeout=timeout, verify=verify, cert=cert, cookies=cookies
            )
        elif method == "DELETE":
            resp = requests.delete(
                url=url, headers=headers, json=data, timeout=timeout, verify=verify, cert=cert, cookies=cookies
            )
        elif method == "PUT":
            resp = requests.put(
                url=url, headers=headers, json=data, timeout=timeout, verify=verify, cert=cert, cookies=cookies
            )
        else:
            return False, {"error": "method not supported"}
    except requests.exceptions.RequestException as e:
        print("http request error! method: %s, url: %s, data: %s! err=%s", method, url, data, e)
        return False, {"error": str(e)}
    else:
        if resp.status_code != 200:
            content = resp.content[:100] if resp.content else ""
            error_msg = (
                "http request fail! method: %s, url: %s, data: %s, " "response_status_code: %s, response_content: %s"
            )
            print(error_msg % (method, url, str(data), resp.status_code, content))
            return False, {"error": "status_code is %d, not 200" % resp.status_code}

        return True, resp.json()


def http_get(url, data, headers=None, verify=False, cert=None, timeout=None, cookies=None):
    if not headers:
        headers = _gen_header()
    return _http_request(
        method="GET", url=url, headers=headers, data=data, verify=verify, cert=cert, timeout=timeout, cookies=cookies
    )


def http_post(url, data, headers=None, verify=False, cert=None, timeout=None, cookies=None):
    if not headers:
        headers = _gen_header()
    return _http_request(
        method="POST", url=url, headers=headers, data=data, timeout=timeout, verify=verify, cert=cert, cookies=cookies
    )


def http_put(url, data, headers=None, verify=False, cert=None, timeout=None, cookies=None):
    if not headers:
        headers = _gen_header()
    return _http_request(
        method="PUT", url=url, headers=headers, data=data, timeout=timeout, verify=verify, cert=cert, cookies=cookies
    )


def http_delete(url, data, headers=None, verify=False, cert=None, timeout=None, cookies=None):
    if not headers:
        headers = _gen_header()
    return _http_request(
        method="DELETE", url=url, headers=headers, data=data, timeout=timeout, verify=verify, cert=cert, cookies=cookies
    )


# =================== iam func ===================


class Client(object):
    def __init__(self, app_code, app_secret, bk_iam_host):
        self.app_code = app_code
        self.app_secret = app_secret
        self.bk_iam_host = bk_iam_host
        self.system_id_set = set()
        self.resource_id_set = set()
        self.action_id_set = set()

    # 调用权限中心方法
    def _call_iam_api(self, http_func, path, data):
        headers = {"X-BK-APP-CODE": self.app_code, "X-BK-APP-SECRET": self.app_secret}
        if USE_APIGATEWAY:
            headers = {
                "X-Bkapi-Authorization": json.dumps({"bk_app_code": self.app_code, "bk_app_secret": self.app_secret}),
            }

        url = "{host}{path}".format(host=self.bk_iam_host, path=path)
        ok, _data = http_func(url, data, headers=headers)
        # TODO: add debug here
        if not ok:
            message = _data.get("error", "verify from iam server fail")
            print("_call_iam_api fail.", "error:", message)
            return False, message, None

        if _data.get("code") != 0:
            message = _data.get("message", "iam api fail")

            if not (
                    http_func.__name__ == "http_get"
                    and path.startswith("/api/v1/model/systems/")
                    and path.endswith("/query")
                    and message.startswith("not found:system(")
                    and message.endswith(") not exists")
            ):
                print("_call_iam_api fail.", "method:", http_func.__name__, "path:", path, "error:", message)

            return False, message, None

        _d = _data.get("data")

        return True, "ok", _d

    # all operations
    operation_funcs = {
        "add_system": "add_system",
        "update_system": "update_system",
        "upsert_system": "upsert_system",
        "add_resource_type": "add_resource_type",
        "update_resource_type": "update_resource_type",
        "delete_resource_type": "delete_resource_type",
        "upsert_resource_type": "upsert_resource_type",
        "add_instance_selection": "add_instance_selection",
        "update_instance_selection": "update_instance_selection",
        "delete_instance_selection": "delete_instance_selection",
        "upsert_instance_selection": "upsert_instance_selection",
        "add_action": "add_action",
        "update_action": "update_action",
        "delete_action": "delete_action",
        "upsert_action": "upsert_action",
        "add_action_groups": "add_action_groups",
        "update_action_groups": "update_action_groups",
        "upsert_action_groups": "update_action_groups",
        "add_resource_creator_actions": "add_resource_creator_actions",
        "update_resource_creator_actions": "update_resource_creator_actions",
        "upsert_resource_creator_actions": "update_resource_creator_actions",
        "add_common_actions": "add_common_actions",
        "update_common_actions": "update_common_actions",
        "upsert_common_actions": "update_common_actions",
        "add_feature_shield_rules": "add_feature_shield_rules",
        "update_feature_shield_rules": "update_feature_shield_rules",
        "upsert_feature_shield_rules": "update_feature_shield_rules",
        "add_custom_frontend_settings": "add_custom_frontend_settings",
        "update_custom_frontend_settings": "update_custom_frontend_settings",
        "upsert_custom_frontend_settings": "update_custom_frontend_settings"
    }

    """
    index:
    - Add system
    - Update system

    - Add resource types(batch)
    - Update resource type(one by one)
    - Delete resource types(batch)

    - Add actions(batch)
    - Update action(one by one)
    - Delete actions(batch)

    - Query
    """

    # ---------- system
    def api_add_system(self, data):
        path = "/api/v1/model/systems"
        ok, message, data = self._call_iam_api(http_post, path, data)
        # if alreay exists, return true
        return ok, message

    def api_update_system(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    # ---------- resource_type

    def api_batch_add_resource_types(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/resource-types".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        # if alreay exists, return true
        return ok, message

    def api_update_resource_type(self, system_id, resource_type_id, data):
        path = "/api/v1/model/systems/{system_id}/resource-types/{resource_type_id}".format(
            system_id=system_id, resource_type_id=resource_type_id
        )
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    def api_batch_delete_resource_types(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/resource-types?check_existence=false".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_delete, path, data)
        return ok, message

    # ---------- instance_selection

    def api_batch_add_instance_selections(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/instance-selections".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        # if alreay exists, return true
        return ok, message

    def api_update_instance_selection(self, system_id, instance_selection_id, data):
        path = "/api/v1/model/systems/{system_id}/instance-selections/{instance_selection_id}".format(
            system_id=system_id, instance_selection_id=instance_selection_id
        )
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    def api_batch_delete_instance_selections(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/instance-selections?check_existence=false".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_delete, path, data)
        return ok, message

    # ---------- action

    def api_batch_add_actions(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/actions".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        # if alreay exists, return true
        return ok, message

    def api_update_action(self, system_id, action_id, data):
        path = "/api/v1/model/systems/{system_id}/actions/{action_id}".format(system_id=system_id, action_id=action_id)
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    def api_batch_delete_actions(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/actions?check_existence=false".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_delete, path, data)
        return ok, message

    # ---------- action_groups
    def api_add_action_groups(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/action_groups".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        return ok, message

    def api_update_action_groups(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/action_groups".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    # ---------- resource_creator_actions
    def api_add_resource_creator_actions(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/resource_creator_actions".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        return ok, message

    def api_update_resource_creator_actions(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/resource_creator_actions".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    # ---------- common_actions
    def api_add_common_actions(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/common_actions".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        return ok, message

    def api_update_common_actions(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/common_actions".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    # ---------- feature_shield_rules
    def api_add_feature_shield_rules(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/feature_shield_rules".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        return ok, message

    def api_update_feature_shield_rules(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/feature_shield_rules".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    # ---------- custom_frontend_settings
    def api_add_custom_frontend_settings(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/custom_frontend_settings".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_post, path, data)
        return ok, message

    def api_update_custom_frontend_settings(self, system_id, data):
        path = "/api/v1/model/systems/{system_id}/configs/custom_frontend_settings".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_put, path, data)
        return ok, message

    # ---------- query

    def api_query(self, system_id):
        path = "/api/v1/model/systems/{system_id}/query".format(system_id=system_id)
        ok, message, data = self._call_iam_api(http_get, path, None)
        return ok, message, data

    # =================== operations ===================

    def add_system(self, system_id, data):
        d_system_id = data.get("id")
        if not d_system_id:
            return False, "the field `id` required"

        if system_id != d_system_id:
            return False, "json[system_id] is not equals the value of `id`"

        return self.api_add_system(data)

    def update_system(self, system_id, data):
        d_system_id = data.get("id")
        if not d_system_id:
            return False, "the field `id` required"

        if system_id != d_system_id:
            return False, "json[system_id] is not equals the value of `id`"

        return self.api_update_system(system_id, data)

    def add_resource_type(self, system_id, data):
        d_resource_type_id = data.get("id")
        if not d_resource_type_id:
            return False, "the field `id` required"

        d = [data]
        return self.api_batch_add_resource_types(system_id, d)

    def update_resource_type(self, system_id, data):
        d_resource_type_id = data.get("id")
        if not d_resource_type_id:
            return False, "the field `id` required"

        data.pop("id")
        return self.api_update_resource_type(system_id, d_resource_type_id, data)

    def delete_resource_type(self, system_id, data):
        d_resource_type_id = data.get("id")
        if not d_resource_type_id:
            return False, "the field `id` required"

        d = [{"id": d_resource_type_id}]

        return self.api_batch_delete_resource_types(system_id, d)

    def add_instance_selection(self, system_id, data):
        d_instance_selection_id = data.get("id")
        if not d_instance_selection_id:
            return False, "the field `id` required"

        d = [data]
        return self.api_batch_add_instance_selections(system_id, d)

    def update_instance_selection(self, system_id, data):
        d_instance_selection_id = data.get("id")
        if not d_instance_selection_id:
            return False, "the field `id` required"

        data.pop("id")
        return self.api_update_instance_selection(system_id, d_instance_selection_id, data)

    def delete_instance_selection(self, system_id, data):
        d_instance_selection_id = data.get("id")
        if not d_instance_selection_id:
            return False, "the field `id` required"

        d = [{"id": d_instance_selection_id}]

        return self.api_batch_delete_instance_selections(system_id, d)

    def add_action(self, system_id, data):
        d_action_id = data.get("id")
        if not d_action_id:
            return False, "the field `id` required"

        d = [data]
        return self.api_batch_add_actions(system_id, d)

    def update_action(self, system_id, data):
        d_action_id = data.get("id")
        if not d_action_id:
            return False, "the field `id` required"

        return self.api_update_action(system_id, d_action_id, data)

    def delete_action(self, system_id, data):
        d_action_id = data.get("id")
        if not d_action_id:
            return False, "the field `id` required"

        d = [{"id": d_action_id}]

        return self.api_batch_delete_actions(system_id, d)

    def add_action_groups(self, system_id, data):
        return self.api_add_action_groups(system_id, data)

    def update_action_groups(self, system_id, data):
        return self.api_update_action_groups(system_id, data)

    def add_common_actions(self, system_id, data):
        return self.api_add_common_actions(system_id, data)

    def update_common_actions(self, system_id, data):
        return self.api_update_common_actions(system_id, data)

    def add_resource_creator_actions(self, system_id, data):
        return self.api_add_resource_creator_actions(system_id, data)

    def update_resource_creator_actions(self, system_id, data):
        return self.api_update_resource_creator_actions(system_id, data)

    def add_feature_shield_rules(self, system_id, data):
        return self.api_add_feature_shield_rules(system_id, data)

    def update_feature_shield_rules(self, system_id, data):
        return self.api_update_feature_shield_rules(system_id, data)

    def add_custom_frontend_settings(self, system_id, data):
        return self.api_add_custom_frontend_settings(system_id, data)

    def update_custom_frontend_settings(self, system_id, data):
        return self.api_update_custom_frontend_settings(system_id, data)

    def upsert_system(self, system_id, data):
        if system_id not in self.system_id_set:
            return self.add_system(system_id, data)
        return self.update_system(system_id, data)

    def upsert_resource_type(self, system_id, data):
        d_resource_type_id = data.get("id")
        if not d_resource_type_id:
            return False, "the field `id` required"

        if d_resource_type_id not in self.resource_id_set:
            return self.add_resource_type(system_id, data)
        return self.update_resource_type(system_id, data)

    def upsert_instance_selection(self, system_id, data):
        d_instance_selection_id = data.get("id")
        if not d_instance_selection_id:
            return False, "the field `id` required"

        if d_instance_selection_id not in self.instance_selection_id_set:
            return self.add_instance_selection(system_id, data)
        return self.update_instance_selection(system_id, data)

    def upsert_action(self, system_id, data):
        d_action_id = data.get("id")
        if not d_action_id:
            return False, "the field `id` required"

        if d_action_id not in self.action_id_set:
            return self.add_action(system_id, data)
        return self.update_action(system_id, data)

    def query_all_models(self, system_id):
        ok, message, data = self.api_query(system_id)
        if not ok:
            # ignore the first migration do_migrate fail
            if "0001_" in data_file:
                pass
            else:
                print(
                    "[Ignore this message if do_migrate the first file 0001_*.json, "
                    "because the system is not registered yet] do api_query fail",
                    message,
                )
            return set(), set(), set(), set()

        system = data.get("base_info", {}) or {}
        resource_types = data.get("resource_types", []) or []
        actions = data.get("actions", []) or []
        instance_selections = data.get("instance_selections", []) or []

        system_ids = {system.get("id")}
        resource_type_ids = {r.get("id") for r in resource_types}
        action_ids = {a.get("id") for a in actions}
        instance_selection_ids = {r.get("id") for r in instance_selections}

        return system_ids, resource_type_ids, action_ids, instance_selection_ids

    def do_operation(self, op, system_id, data):
        if op not in self.operation_funcs:
            print("invalid operation: %s" % op)
            exit(1)

        return getattr(self, self.operation_funcs[op])(system_id, data)

    def setup_models(self, system_id_set, resource_id_set, action_id_set, instance_selection_id_set):
        self.system_id_set = system_id_set
        self.resource_id_set = resource_id_set
        self.action_id_set = action_id_set
        self.instance_selection_id_set = instance_selection_id_set


# ---------- ping


def api_ping(bk_iam_host):
    url = "{host}{path}".format(host=bk_iam_host, path="/ping")
    ok, data = http_get(url, None, timeout=5)
    return ok, data


def do_migrate(data, bk_iam_host=BK_IAM_HOST, app_code=APP_CODE, app_secret=APP_SECRET):
    system_id = data.get("system_id")
    if not system_id:
        print("invald json. [system_id] required, and should not be empty")
        return False

    operations = data.get("operations")
    if not operations:
        print("invald json. [operations] required, and should not be empty")
        return False

    print("do migrate")

    client = Client(app_code, app_secret, bk_iam_host)

    # 1. query all data of the system
    system_ids, resource_type_ids, action_ids, instance_selection_ids = client.query_all_models(system_id)

    client.setup_models(system_ids, resource_type_ids, action_ids, instance_selection_ids)

    for op in operations:
        operation = op.get("operation")
        if not operation:
            print("there got a empty `operation` in the json, will ignore and continue")
            continue
            # print("")
            # return False

        data = op.get("data")
        if not data:
            print("no `data` in the json body or the `data` is empty, operation=%s" % operation)
            return False

        op_data_id = ""
        if isinstance(data, dict):
            op_data_id = "id=%s" % data.get("id")

        ok, message = client.do_operation(operation, system_id, data)
        if not ok:
            print("execute operation [%s] %s fail, error message: %s" % (operation, message, op_data_id))
            return False
        print("execute operation [%s] %s success!" % (operation, op_data_id))

    print("end migrate")
    return True


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument(
        "-t",
        action="store",
        dest="bk_iam_host",
        help=(
            "bk_iam_host, i.e: http://iam.service.consul;"
            "you can use bk_apigateway_url here, set with the '--apigateway' "
        ),
        required=True,
    )
    p.add_argument(
        "-f",
        action="store",
        dest="json_data_file",
        help="which migration file to execute, i.e: 00001_bk_cmdb_20190618100210.json",
        required=True,
    )
    p.add_argument("-a", action="store", dest="app_code", help="app code", required=True)
    p.add_argument("-s", action="store", dest="app_secret", help="app secret", required=True)

    p.add_argument(
        "--apigateway",
        action="store_true",
        dest="use_apigateway",
        help="you can use bk_apigateway_url in '-t', should set this flag",
    )
    args = p.parse_args()

    BK_IAM_HOST = args.bk_iam_host.rstrip("/")
    USE_APIGATEWAY = args.use_apigateway
    if USE_APIGATEWAY:
        print(
            "use apigateway:",
            args.use_apigateway,
            ", please make sure '-t %s' is a valid bk_apigateway_url" % args.bk_iam_host,
            )

    if not BK_IAM_HOST.startswith("http") :
        BK_IAM_HOST = "http://%s" % BK_IAM_HOST

    data_file = args.json_data_file
    APP_CODE = args.app_code
    APP_SECRET = args.app_secret

    # test ping
    ok, _ = api_ping(BK_IAM_HOST)
    if not ok:
        print("iam service is not available: %s" % BK_IAM_HOST)
        exit(1)

    print("start migrate [%s]" % data_file)

    # 数据解析
    data = load_data(data_file)
    if not data:
        exit(1)

    ok = do_migrate(data, BK_IAM_HOST, APP_CODE, APP_SECRET)
    if not ok:
        print("do migrate [%s] fail" % data_file)
        exit(1)
    print("do migrate [%s] success!" % data_file)
