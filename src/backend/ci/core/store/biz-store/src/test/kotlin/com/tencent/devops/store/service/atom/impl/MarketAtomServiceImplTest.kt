package com.tencent.devops.store.service.atom.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.atom.AtomParamOption
import org.junit.jupiter.api.Test

internal class MarketAtomServiceImplTest {

    @Test
    fun generateCiV2Yaml() {
        val sb = StringBuffer()
        val propsStr = """{
  "inputGroups" : [ {
    "name" : "fetch",
    "label" : "fetch",
    "isExpanded" : true
  }, {
    "name" : "submodule",
    "label" : "子模块",
    "isExpanded" : true
  }, {
    "name" : "lfs",
    "label" : "lfs",
    "isExpanded" : true
  }, {
    "name" : "merge",
    "label" : "merge",
    "isExpanded" : true
  }, {
    "name" : "config",
    "label" : "配置",
    "isExpanded" : true
  } ],
  "input" : {
    "repositoryType" : {
      "label" : "代码库",
      "default" : "ID",
      "desc" : "值为ID/NAME/URL",
      "type" : "enum-input",
      "required" : true,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "list" : [ {
        "label" : "按代码库选择",
        "value" : "ID"
      }, {
        "label" : "按代码库别名输入",
        "value" : "NAME"
      }, {
        "label" : "按仓库URL输入",
        "value" : "URL"
      } ]
    },
    "repositoryHashId" : {
      "label" : "按代码库选择",
      "default" : "",
      "placeholder" : "请选择代码库名称",
      "type" : "selector",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "optionsConf" : {
        "searchable" : true,
        "multiple" : false,
        "url" : "/repository/api/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=CODE_GIT&page=1&pageSize=5000",
        "paramId" : "repositoryHashId",
        "paramName" : "aliasName",
        "itemTargetUrl" : "http://devops.oa.com/console/codelib/{projectId}/",
        "itemText" : "关联代码库",
        "hasAddItem" : true
      },
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "ID"
        } ]
      }
    },
    "repositoryName" : {
      "label" : "按代码库别名输入",
      "default" : "",
      "placeholder" : "请输入代码库别名",
      "type" : "vuex-input",
      "desc" : "描述",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "NAME"
        } ]
      }
    },
    "repositoryUrl" : {
      "label" : "代码库链接",
      "default" : "",
      "placeholder" : "请输入代码库URL",
      "type" : "vuex-input",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "URL"
        } ]
      }
    },
    "authType" : {
      "label" : "授权类型",
      "default" : "TICKET",
      "type" : "enum-input",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "list" : [ {
        "label" : "凭证",
        "value" : "TICKET"
      }, {
        "label" : "access token",
        "value" : "ACCESS_TOKEN"
      }, {
        "label" : "username/password",
        "value" : "USERNAME_PASSWORD"
      }, {
        "label" : "流水线启动人token",
        "value" : "START_USER_TOKEN"
      } ],
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "URL"
        } ]
      }
    },
    "ticketId" : {
      "label" : "代码库凭证",
      "placeholder" : "请选中对应凭证",
      "type" : "select-input",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "URL"
        }, {
          "key" : "authType",
          "value" : "TICKET"
        } ]
      },
      "optionsConf" : {
        "searchable" : true,
        "multiple" : false,
        "url" : "/ticket/api/user/credentials/{projectId}/hasPermissionList?permission=USE&page=1&pageSize=10000",
        "paramId" : "credentialId",
        "paramName" : "credentialId",
        "itemTargetUrl" : "http://devops.oa.com/console/ticket/{projectId}/",
        "itemText" : "添加新的凭证",
        "hasAddItem" : true
      }
    },
    "accessToken" : {
      "label" : "access token",
      "placeholder" : "请输入对应access token",
      "type" : "vuex-input",
      "inputType" : "password",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : true,
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "URL"
        }, {
          "key" : "authType",
          "value" : "ACCESS_TOKEN"
        } ]
      }
    },
    "username" : {
      "label" : "username",
      "placeholder" : "请输入用户名",
      "type" : "vuex-input",
      "inputType" : "password",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "URL"
        }, {
          "key" : "authType",
          "value" : "USERNAME_PASSWORD"
        } ]
      }
    },
    "password" : {
      "label" : "password",
      "placeholder" : "请输入密码",
      "type" : "vuex-input",
      "inputType" : "password",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : true,
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "repositoryType",
          "value" : "URL"
        }, {
          "key" : "authType",
          "value" : "USERNAME_PASSWORD"
        } ]
      }
    },
    "persistCredentials" : {
      "label" : "",
      "default" : true,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "是否持久化凭证",
      "desc" : "如果后面job需要用到git凭证,则持久化,凭证只对当前仓库有效",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false
    },
    "pullType" : {
      "label" : "指定拉取方式",
      "default" : "BRANCH",
      "type" : "select-input",
      "required" : false,
      "optionsConf" : {
        "searchable" : true
      },
      "options" : [ {
        "id" : "BRANCH",
        "name" : "按分支"
      }, {
        "id" : "TAG",
        "name" : "按标签名"
      }, {
        "id" : "COMMIT_ID",
        "name" : "按提交ID"
      } ]
    },
    "refName" : {
      "label" : "分支/TAG/COMMIT",
      "default" : "master",
      "placeholder" : "请输入",
      "type" : "vuex-input",
      "desc" : "",
      "required" : true,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false
    },
    "localPath" : {
      "label" : "代码保存路径",
      "default" : "",
      "placeholder" : "请填写工作空间相对目录，不填则默认为工作空间目录",
      "type" : "vuex-input",
      "desc" : "当前流水线存在多个代码拉取插件，你需设置此字段以解决冲突问题",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false
    },
    "strategy" : {
      "label" : "拉取策略",
      "default" : "REVERT_UPDATE",
      "type" : "enum-input",
      "desc" : "Revert Update: 增量,每次先\"git reset --hard HEAD\",再\"git pull\"\n\n Fresh Checkout: 全量,每次都会全新clone代码,之前会delete整个工作空间\n\n Increment Update: 增量,只使用\"git pull\",并不清除冲突及历史缓存文件  ",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "list" : [ {
        "label" : "Revert Update",
        "value" : "REVERT_UPDATE"
      }, {
        "label" : "Fresh Checkout",
        "value" : "FRESH_CHECKOUT"
      }, {
        "label" : "Increment Update",
        "value" : "INCREMENT_UPDATE"
      } ]
    },
    "fetchDepth" : {
      "label" : "git fetch的depth参数值",
      "default" : "",
      "placeholder" : "",
      "type" : "vuex-input",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "fetch"
    },
    "fetchOnlyCurrentRef" : {
      "label" : "",
      "default" : false,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "仅fetch当前配置的分支",
      "desc" : "默认从远端获取所有的分支，配置后只fetch当前分支,如果后续有插件需要切换分支,插件需要先fetch分支再切换",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "fetch"
    },
    "enableGitLfs" : {
      "label" : "",
      "default" : false,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "是否开启Git Lfs",
      "desc" : "选中则执行git lfs pull",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "lfs"
    },
    "enableSubmodule" : {
      "label" : "",
      "default" : true,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "启用Submodule",
      "desc" : "勾选则启用外链，不勾选则不启用",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "submodule"
    },
    "submodulePath" : {
      "label" : "",
      "default" : "",
      "placeholder" : "请填写需拉取的Submodule path，多个用逗号分隔，不填默认拉所有Submodule",
      "type" : "vuex-input",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "submodule",
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "enableSubmodule",
          "value" : true
        } ]
      }
    },
    "enableSubmoduleRemote" : {
      "label" : "",
      "default" : false,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "执行git submodule update后面是否带上--remote参数",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "submodule",
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "enableSubmodule",
          "value" : true
        } ]
      }
    },
    "enableSubmoduleRecursive" : {
      "label" : "",
      "default" : true,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "执行git submodule update后面是否带上--recursive参数",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "submodule",
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "enableSubmodule",
          "value" : true
        } ]
      }
    },
    "enableVirtualMergeBranch" : {
      "label" : "",
      "default" : true,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "MR事件触发时执行Pre-Merge",
      "desc" : "我们会在MR事件触发时尝试Merge源分支到目标分支，冲突将直接判定为失败",
      "required" : true,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "merge"
    },
    "enableGitClean" : {
      "label" : "",
      "default" : true,
      "placeholder" : "",
      "type" : "atom-checkbox",
      "text" : "是否开启Git Clean",
      "desc" : "选中删除未进行版本管理的文件,排除.gitignore中配置的文件和目录(git clean -df)",
      "required" : true,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "config",
      "rely" : {
        "operation" : "AND",
        "expression" : [ {
          "key" : "strategy",
          "value" : "REVERT_UPDATE"
        } ]
      }
    },
    "autoCrlf" : {
      "label" : "AutoCrlf配置值",
      "default" : "false",
      "type" : "select-input",
      "optionsConf" : {
        "searchable" : true
      },
      "groupName" : "config",
      "options" : [ {
        "id" : "false",
        "name" : "false"
      }, {
        "id" : "true",
        "name" : "true"
      }, {
        "id" : "input",
        "name" : "input"
      } ]
    },
    "includePath" : {
      "label" : "代码库拉取相对子路径",
      "default" : "",
      "placeholder" : "请填写代码库相对目录，多个用逗号分隔",
      "type" : "vuex-input",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "config"
    },
    "excludePath" : {
      "label" : "排除代码库以下路径",
      "default" : "",
      "placeholder" : "请填写代码库相对目录，多个用逗号分隔",
      "type" : "vuex-input",
      "desc" : "",
      "required" : false,
      "disabled" : false,
      "hidden" : false,
      "isSensitive" : false,
      "groupName" : "config"
    }
  },
  "output" : {
    "BK_CI_GIT_REPO_URL" : {
      "type" : "string",
      "description" : "代码库的URL"
    },
    "BK_CI_GIT_REPO_ALIAS_NAME" : {
      "type" : "string",
      "description" : "代码库别名,只有选择`按代码库选择`或`按代码库别名输入`才有值"
    },
    "BK_CI_GIT_REPO_NAME" : {
      "type" : "string",
      "description" : "代码库的工程名称"
    },
    "BK_CI_GIT_REPO_REF" : {
      "type" : "string",
      "description" : "当前代码库分支"
    },
    "BK_CI_GIT_REPO_CODE_PATH" : {
      "type" : "string",
      "description" : "当前代码库本地存放路径"
    },
    "BK_CI_GIT_REPO_LAST_COMMIT_ID" : {
      "type" : "string",
      "description" : "拉取代码时，上次构建最后的commit id"
    },
    "BK_CI_GIT_REPO_MR_TARGET_HEAD_COMMIT_ID" : {
      "type" : "string",
      "description" : "拉取代码时，目标分支的commitId,只有mr事件触发并启用pre-merge功能才有值"
    },
    "BK_CI_GIT_REPO_HEAD_COMMIT_ID" : {
      "type" : "string",
      "description" : "拉取代码时，本次构建最后的commit id,如果启用pre-merge,则是合并完后的commitId"
    },
    "BK_CI_GIT_REPO_HEAD_COMMIT_COMMENT" : {
      "type" : "string",
      "description" : "拉取代码时，本次构建最后的commit注释"
    },
    "BK_CI_GIT_REPO_HEAD_COMMIT_AUTHOR" : {
      "type" : "string",
      "description" : "本次产生的新的author"
    },
    "BK_CI_GIT_REPO_HEAD_COMMIT_COMMITTER" : {
      "type" : "string",
      "description" : "本次产生的新的committer"
    },
    "BK_CI_GIT_REPO_COMMITS" : {
      "type" : "string",
      "description" : "本次产生的新的commit id"
    }
  }
}"""

        val props: Map<String, Any> = jacksonObjectMapper().readValue(propsStr)
        if (null != props["input"]) {
            sb.append("  with:\r\n")
            val input = props["input"] as Map<String, Any>
            input.forEach {
                val paramKey = it.key
                val paramValueMap = it.value as Map<String, Any>

                val label = paramValueMap["label"]
                val text = paramValueMap["text"]
                val desc = paramValueMap["desc"]
                val description = if (label?.toString().isNullOrBlank()) {
                    if (text?.toString().isNullOrBlank()) {
                        desc
                    } else {
                        text
                    }
                } else {
                    label
                }
                val type = paramValueMap["type"]
                val required = paramValueMap["required"]
                val defaultValue = paramValueMap["default"]
                val multipleMap = paramValueMap["optionsConf"]
                val multiple = if (null != multipleMap && null != (multipleMap as Map<String, String>)["multiple"]) {
                    "true".equals(multipleMap["multiple"].toString(), true)
                } else {
                    false
                }
                val requiredName = "必填"
                val defaultName = "默认"
                val valueName = "值"
                if ((type == "selector" && multiple) ||
                    type in listOf("atom-checkbox-list", "staff-input", "company-staff-input", "parameter")) {
                    sb.append("      # $description")
                    if (null != required && "true".equals(required.toString(), true)) {
                        sb.append(", $requiredName")
                    }
                    if (null != defaultValue && (defaultValue.toString()).isNotBlank()) {
                        sb.append(", $defaultName: ${defaultValue.toString().replace("\n", "")}")
                    }
//                    val multipleOptions = paramValueMap["options"]
//                    if (multipleOptions != null) {
//                        try {
//                            val options = multipleOptions as ArrayList<AtomParamOption>
//                            sb.append(", ")
//                            options.forEach { option ->
//                                sb.append("${option.id}[${option.name}] ||")
//                            }
//                            sb.removeSuffix("||")
//                        } catch (e: Exception) {
//                            println("load atom input[$paramKey] with error: ${e.message}")
//                        }
//                    }
                    sb.append("\r\n")
                    sb.append("      $paramKey: ")
                    sb.append("        - string\r\n")
                    sb.append("        - string\r\n")
                } else {
                    sb.append("    # ${description.toString().replace("\n", "")}")
                    if (null != required && "true".equals(required.toString(), true)) {
                        sb.append(", $requiredName")
                    }
                    if (null != defaultValue && (defaultValue.toString()).isNotBlank()) {
                        sb.append(", $defaultName: ${defaultValue.toString().replace("\n", "")}")
                    }
                    val multipleOptions = paramValueMap["options"]
                    if (multipleOptions != null) {
                        try {
                            multipleOptions as List<Map<String, String>>
                            sb.append(", $valueName:")
                            multipleOptions.forEachIndexed { index, map ->
                                if (index == multipleOptions.size - 1) sb.append(" ${map["id"]}[${map["name"]}]")
                                else sb.append(" ${map["id"]}[${map["name"]}] |")
                            }
                            sb.removeSuffix("|")
                        } catch (e: Exception) {
                            println("load atom input[$paramKey] with error: ${e.message}")
                        }
                    }
                    sb.append("\r\n")
                    sb.append("    $paramKey: ")
                    if (type == "atom-checkbox") {
                        sb.append("boolean")
                    } else {
                        sb.append("string")
                    }
                    sb.append("\r\n")
                }
            }
        }
        println(sb.toString())
    }
}
