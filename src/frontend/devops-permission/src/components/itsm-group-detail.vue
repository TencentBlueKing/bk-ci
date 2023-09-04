<script setup lang="ts">
import {
  ref,
  onMounted
} from 'vue';
import {
  useRoute,
} from 'vue-router';
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
const route = useRoute();
const { t } = useI18n();

import { Error } from 'bkui-vue/lib/icon'
const emits = defineEmits(['hidden-detail']);

const showDetail = ref(false);
const showInstancesDetail = ref(false);
const isLoading= ref(false);
const groupPermissionDetail = ref([]);
const relatedResourceInfo = ref({});
const groupId = route.query['group_id'] || ''

const fetchGroupPermissionDetail = async () => {
  if (groupId) {
    await http.getGroupPermissionDetail(groupId).then(res => {
        const res1 = [
  {
    "actionId": "cert_list",
    "name": "证书列表",
    "relatedResourceInfo": {
      "type": "cert",
      "name": "证书",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "cert_delete",
    "name": "删除证书",
    "relatedResourceInfo": {
      "type": "cert",
      "name": "证书",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "cert_edit",
    "name": "编辑证书",
    "relatedResourceInfo": {
      "type": "cert",
      "name": "证书",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "cert_use",
    "name": "使用证书",
    "relatedResourceInfo": {
      "type": "cert",
      "name": "证书",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "cert_view",
    "name": "查看证书",
    "relatedResourceInfo": {
      "type": "cert",
      "name": "证书",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "codecc_task_list",
    "name": "任务列表",
    "relatedResourceInfo": {
      "type": "codecc_task",
      "name": "CodeCC任务",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "credential_list",
    "name": "凭据列表",
    "relatedResourceInfo": {
      "type": "credential",
      "name": "凭据",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "credential_edit",
    "name": "编辑凭据",
    "relatedResourceInfo": {
      "type": "credential",
      "name": "凭据",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "credential_use",
    "name": "使用凭据",
    "relatedResourceInfo": {
      "type": "credential",
      "name": "凭据",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "credential_view",
    "name": "查看凭据",
    "relatedResourceInfo": {
      "type": "credential",
      "name": "凭据",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "credential_delete",
    "name": "删除凭据",
    "relatedResourceInfo": {
      "type": "credential",
      "name": "凭据",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "env_node_view",
    "name": "查看节点",
    "relatedResourceInfo": {
      "type": "env_node",
      "name": "节点",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "env_node_list",
    "name": "节点列表",
    "relatedResourceInfo": {
      "type": "env_node",
      "name": "节点",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "env_node_use",
    "name": "使用节点",
    "relatedResourceInfo": {
      "type": "env_node",
      "name": "节点",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "env_node_delete",
    "name": "删除节点",
    "relatedResourceInfo": {
      "type": "env_node",
      "name": "节点",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "env_node_edit",
    "name": "编辑节点",
    "relatedResourceInfo": {
      "type": "env_node",
      "name": "节点",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "environment_view",
    "name": "查看环境",
    "relatedResourceInfo": {
      "type": "environment",
      "name": "环境",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "environment_list",
    "name": "环境列表",
    "relatedResourceInfo": {
      "type": "environment",
      "name": "环境",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "environment_delete",
    "name": "删除环境",
    "relatedResourceInfo": {
      "type": "environment",
      "name": "环境",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "environment_edit",
    "name": "编辑环境",
    "relatedResourceInfo": {
      "type": "environment",
      "name": "环境",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "environment_use",
    "name": "使用环境",
    "relatedResourceInfo": {
      "type": "environment",
      "name": "环境",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_group_view",
    "name": "查看版本体验用户组",
    "relatedResourceInfo": {
      "type": "experience_group",
      "name": "版本体验用户组",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_group_list",
    "name": "版本体验用户组列表",
    "relatedResourceInfo": {
      "type": "experience_group",
      "name": "版本体验用户组",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_group_delete",
    "name": "删除版本体验用户组",
    "relatedResourceInfo": {
      "type": "experience_group",
      "name": "版本体验用户组",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_group_edit",
    "name": "编辑版本体验用户组",
    "relatedResourceInfo": {
      "type": "experience_group",
      "name": "版本体验用户组",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_task_view",
    "name": "查看版本体验任务",
    "relatedResourceInfo": {
      "type": "experience_task",
      "name": "版本体验任务",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_task_list",
    "name": "版本体验任务列表",
    "relatedResourceInfo": {
      "type": "experience_task",
      "name": "版本体验任务",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_task_delete",
    "name": "下架版本体验任务",
    "relatedResourceInfo": {
      "type": "experience_task",
      "name": "版本体验任务",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_task_edit",
    "name": "编辑版本体验任务",
    "relatedResourceInfo": {
      "type": "experience_task",
      "name": "版本体验任务",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_list",
    "name": "流水线列表",
    "relatedResourceInfo": {
      "type": "pipeline",
      "name": "流水线",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_download",
    "name": "下载制品",
    "relatedResourceInfo": {
      "type": "pipeline",
      "name": "流水线",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_share",
    "name": "分享制品",
    "relatedResourceInfo": {
      "type": "pipeline",
      "name": "流水线",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_view",
    "name": "查看流水线",
    "relatedResourceInfo": {
      "type": "pipeline",
      "name": "流水线",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_execute",
    "name": "执行流水线",
    "relatedResourceInfo": {
      "type": "pipeline",
      "name": "流水线",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_edit",
    "name": "编辑流水线",
    "relatedResourceInfo": {
      "type": "pipeline",
      "name": "流水线",
      "instances": {
        "type": "pipeline",
        "name": "流水线",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "848461",
              "name": "【gitci-测试】部署SCM服务[230209095293]",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "393620",
              "name": "qd-php/tafSpringbootActivityGeneralServant",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "458",
              "name": "重启V2开发环境微服务",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "566",
              "name": "【V2-开发】部署微服务",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "505",
              "name": "【V2-测试】部署微服务",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_delete",
    "name": "删除流水线",
    "relatedResourceInfo": {
      "type": "pipeline",
      "name": "流水线",
      "instances": {
        "type": "pipeline",
        "name": "流水线",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "848461",
              "name": "【gitci-测试】部署SCM服务[230209095293]",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "393620",
              "name": "qd-php/tafSpringbootActivityGeneralServant",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ],
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            },
            {
              "id": "458",
              "name": "重启V2开发环境微服务",
              "type": "pipeline",
              "type_name": "流水线",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "project_visit",
    "name": "访问项目",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "project_list",
    "name": "项目列表",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "pipeline_create",
    "name": "创建流水线",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "codecc_task_create",
    "name": "新建任务",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "codecc_rule_set_create",
    "name": "规则集创建",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "codecc_rule_set_list",
    "name": "规则集列表",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "repertory_create",
    "name": "关联代码库",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "credential_create",
    "name": "创建凭据",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "cert_create",
    "name": "创建证书",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "environment_create",
    "name": "创建环境",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "env_node_create",
    "name": "创建节点",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_task_create",
    "name": "创建版本体验任务",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "experience_group_create",
    "name": "创建版本体验用户组",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "project_view",
    "name": "查看项目",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "rule_create",
    "name": "创建质量红线规则",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "quality_group_create",
    "name": "创建质量红线规则用户组",
    "relatedResourceInfo": {
      "type": "project",
      "name": "项目",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "quality_group_list",
    "name": "质量红线规则用户组列表",
    "relatedResourceInfo": {
      "type": "quality_group",
      "name": "质量红线通知组",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "quality_group_edit",
    "name": "编辑质量红线规则用户组",
    "relatedResourceInfo": {
      "type": "quality_group",
      "name": "质量红线通知组",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "quality_group_delete",
    "name": "删除质量红线规则用户组",
    "relatedResourceInfo": {
      "type": "quality_group",
      "name": "质量红线通知组",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "repertory_list",
    "name": "代码库列表",
    "relatedResourceInfo": {
      "type": "repertory",
      "name": "代码库",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "repertory_use",
    "name": "使用代码库",
    "relatedResourceInfo": {
      "type": "repertory",
      "name": "代码库",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "repertory_view",
    "name": "查看代码库",
    "relatedResourceInfo": {
      "type": "repertory",
      "name": "代码库",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "repertory_delete",
    "name": "删除代码库",
    "relatedResourceInfo": {
      "type": "repertory",
      "name": "代码库",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "repertory_edit",
    "name": "编辑代码库",
    "relatedResourceInfo": {
      "type": "repertory",
      "name": "代码库",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "rule_list",
    "name": "质量红线规则列表",
    "relatedResourceInfo": {
      "type": "rule",
      "name": "质量红线规则",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "rule_edit",
    "name": "编辑质量红线规则",
    "relatedResourceInfo": {
      "type": "rule",
      "name": "质量红线规则",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "rule_enable",
    "name": "启用.action/停用质量红线规则",
    "relatedResourceInfo": {
      "type": "rule",
      "name": "质量红线规则",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  },
  {
    "actionId": "rule_delete",
    "name": "删除质量红线规则",
    "relatedResourceInfo": {
      "type": "rule",
      "name": "质量红线规则",
      "instances": {
        "type": "project",
        "name": "项目",
        "path": [
          [
            {
              "id": "bkdevops",
              "name": "蓝盾BKDevOps项目",
              "type": "project",
              "type_name": "项目",
              "system_id": "bk_ci_rbac"
            }
          ]
        ]
      }
    }
  }
]
      groupPermissionDetail.value = res1.map(i => {
        return {
            ...i,
            expand: false,
        }
      });
      console.log(groupPermissionDetail.value, 'groupPermissionDetail')
    }).catch(() => {
      groupPermissionDetail.value = [];
    });
    isLoading.value = false;
  }
};

const handleShowInstances = (data, name) => {
  relatedResourceInfo.value = { ...data, actionName: name };
  showInstancesDetail.value = true;
}
onMounted(() => {
    fetchGroupPermissionDetail();
});
</script>

<template>
  <section>
    <bk-table
        class="resources-table"
        :data="groupPermissionDetail"
        :border="['row', 'outer']">
        <bk-table-column :label="t('操作')" width="150" show-overflow-tooltip>
            <template #default="{ data }">
            {{ data?.name }}
            </template>
        </bk-table-column>
        <bk-table-column :label="t('操作对象')">
            <template #default="{ data }">
            <div v-if="data?.relatedResourceInfo" :class="{
                'resources-info': true,
                'show': data.expand
            }">
                <div class="resources-content">
                    <span v-if="data.relatedResourceInfo?.instances.type.includes('pipeline')">
                        {{ t('共N条XX', [data.relatedResourceInfo?.instances?.path.length, data.relatedResourceInfo?.instances.name]) }}
                    </span>
                    <span v-else-if="data.relatedResourceInfo?.instances.type.includes('project')">
                        {{ t('共N个XX', [1, data.relatedResourceInfo?.instances.name]) }}
                    </span>
                    <span v-else>
                        {{ t('共N个XX', [data.relatedResourceInfo?.instances?.path.length, data.relatedResourceInfo?.name]) }}
                    </span>
                </div>
                <div v-if="data.relatedResourceInfo?.instances.type.includes('project')" class="resources-content" >
                    <div>
                          {{ data.relatedResourceInfo?.instances?.path[0][0].name }}
                    </div>
                </div>
                <div v-else class="resources-content" v-for="(path, pathIndex) in data.relatedResourceInfo?.instances?.path" :key="pathIndex">
                    <div class="item">
                        <span v-for="(item, index) in path" :key="item.id">
                          {{ item.name }} {{ index !== path.length -1 ? ' / ' : '' }} 
                        </span>
                        <bk-button class="expand-btn" v-if="!data.expand && data.relatedResourceInfo?.instances?.path.length > 3 && pathIndex === 2" text @click="data.expand = true">{{ t('展开') }}</bk-button>
                    </div>
                </div>
                <bk-button class="expand-btn" v-if="data.expand && data.relatedResourceInfo?.instances?.path.length > 3" text @click="data.expand = false">{{ t('收起') }}</bk-button>
            </div>
            <span v-else>--</span>
            </template>
        </bk-table-column>
    </bk-table>
  </section>
</template>

<style lang="postcss" scoped>
    .resources-table {
        ::v-deep .cell {
            line-height: 30px !important;
        }
    }
    .resources-info {
        display: -webkit-box;
        overflow: hidden;
        white-space: normal !important;
        text-overflow: ellipsis;
        word-wrap: break-word;
        -webkit-line-clamp: 4;
        -webkit-box-orient: vertical;
        &.show {
            -webkit-line-clamp: 1000;
        }
    }
    .resources-content {
        .item {
            position: relative;
            display: inline-block;
        }
        .expand-btn {
            position: absolute;
            top: 10px;
            left: calc(100% + 20px);
        }
    }
    .expand-btn {
        color: #3c96ff;
    }
</style>
