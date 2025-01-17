import codecs
import json
import os
import functools
import re
import sys

VERSION_LOG_PATH = os.environ.get("VERSION_LOG_PATH", os.getcwd())
VERSION_LOG_DIALOG_VISIBLE =  os.environ.get("VERSION_LOG_DIALOG_VISIBLE", 'True').lower() in ('true', '1')

# data元素格式
'''
{
    "version": "V1.0.0.0",
    "time": "2020-03-29",
    "content": "### 【V1.0.0.0】版本更新明细\n#### 【新增】一个功能.\n"
}
'''
language_data_map = {}
resp = {
    "success": True,
    "code": 0,
    "errorMsg": None,
    "data": [],
    "requestId": None,
    "dialogVisible": VERSION_LOG_DIALOG_VISIBLE
}
DEFAULT_LANGUAGE = "zh_CN"
time_pattern = r'\d{4}-\d{2}-\d{2}'

# 获取版本类型, 0-输出所有版本, 1-输出release版本, 2-输出rc版本
def getVersionType():
    versionType = 0
    if len(sys.argv) > 1:
        versionType = sys.argv[1]
    return int(versionType)

def extract_title_and_content(changelog_content):
    sections_data = []
    current_heading = None
    current_time = ""
    current_content = []

    lines = changelog_content.split('\n')
    for line in lines:
        line = line.rstrip()
        if line.startswith('# '):
            if current_heading:
                sections_data.append((current_heading, current_time , '\n'.join(current_content)))
            current_heading = line[2:]
            current_content = []
        elif line.startswith('## ') and re.search(time_pattern, line) and current_heading:
            current_time = line[3:]
        elif current_heading:
            current_content.append(line)

    if current_heading:
        sections_data.append((current_heading, current_time, '\n'.join(current_content)))
    return sections_data

def process(data, path):
    if path.endswith(".md"):
        f = codecs.open(path, "r", encoding="UTF-8")
        content = f.read()
        f.close()
        sections = extract_title_and_content(content)
        versionType = getVersionType()
        for version, time, content in sections:
            if versionType == 0:
                data.append({"version": version, "time": time, "content": content})
            elif versionType == 1 and "-rc" not in version:
                data.append({"version": version, "time": time, "content": content})
            elif versionType == 2 and "-rc" in version:
                data.append({"version": version, "time": time, "content": content})
    else:
        print("not md file, ignore:", path)


def search_tree(data, path):
    dir_names = sorted(os.listdir(path), reverse=True)[:3]
    for name in dir_names:
        whole_path = os.path.join(path, name)
        if os.path.isdir(whole_path):
            search_tree(data, whole_path)
        else:
            process(data, whole_path)
    return data

def compare_version(version_log_1, version_log_2):
    def parse_version(version):
        match = re.match(r'v?(\d+\.\d+\.\d+)(?:-(.*))?$', version)
        main_version, pre_release = match.groups()
        main_parts = [int(part) for part in main_version.split('.')]
        pre_release_parts = pre_release.split('.') if pre_release else []
        return main_parts, pre_release_parts

    version_1 = version_log_1["version"].lower().lstrip("v")
    version_2 = version_log_2["version"].lower().lstrip("v")
    v1_main, v1_pre = parse_version(version_1)
    v2_main, v2_pre = parse_version(version_2)

    # Compare main versions
    for v1, v2 in zip(v1_main, v2_main):
        if v1 < v2:
            return -1
        elif v1 > v2:
            return 1

    # If main versions are equal, compare pre-release versions
    for v1, v2 in zip(v1_pre, v2_pre):
        if v1 < v2:
            return -1
        elif v1 > v2:
            return 1

    # If all parts are equal, compare lengths of version parts
    if len(v1_main) < len(v2_main):
        return -1
    elif len(v1_main) > len(v2_main):
        return 1
    elif len(v1_pre) < len(v2_pre):
        return 1
    elif len(v1_pre) > len(v2_pre):
        return -1

    # If all parts are equal, versions are equal
    return 0


def write_one_language_version_log(version_log_data, file_name):
    # 版本降序排列
    version_log_data.sort(key=functools.cmp_to_key(compare_version), reverse=True)
    resp["data"] = version_log_data
    bundled_file = codecs.open(os.path.join(VERSION_LOG_PATH, file_name), "w", encoding="UTF-8")
    bundled_file.write(json.dumps(resp))
    bundled_file.close()

def run():
    files = os.listdir('.')
    dirs = []
    for file in files:
        if os.path.isdir(file):
            dirs.append(file)

    for _dir in dirs:
        language = _dir
        version_log_data = search_tree([], os.path.join(VERSION_LOG_PATH, language))
        write_one_language_version_log(version_log_data, "bundledVersionLog_" + language + ".json")
        # 生成默认语言文件
        if language.lower().replace("_", "").replace("-", "") == DEFAULT_LANGUAGE.lower() \
                .replace("_", "").replace("-", ""):
            write_one_language_version_log(version_log_data, "bundledVersionLog.json")

if __name__ == "__main__":
    """
    sys.argv参数说明:
    sys.argv[1](int): 版本类型,0-输出所有版本, 1-输出release版本, 2-输出rc版本
    """
    run()
