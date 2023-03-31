<script setup lang="ts">
import http from '@/http/api';
import tools from '@/utils/tools';
import PermissionHeader from '@/components/permission-header.vue';
import GroupSearch from './group-search.vue';
import {
  ref,
  onMounted,
  nextTick,
  watch,
} from 'vue';
import {
  bkTooltips
} from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import { Message } from 'bkui-vue';
import { useRoute, useRouter } from 'vue-router';
const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const groupList = ref([]);
const projectList = ref([]);
const formData = ref<any>({
  projectCode: '',
  applicant: '',
  groupIds: [],
  expiredAt: '2592000',
  reason: '',
});
const customTime = ref(1);
const currentActive = ref<string|number>(2592000);
const timeFilters = ref({
  2592000: t('1个月'),
  7776000: t('3个月'),
  15552000: t('6个月'),
  31104000: t('12个月'),
});
const formRef = ref();
const isLoading = ref(false);
const scrollLoading = ref(false);
const pageInfo = ref({
  page: 1,
  pageSize: 30,
  projectName: '',
  loadEnd: false,
})

const curProject = ref(null);
const isDisabled = ref(true);

const rules = {
  projectCode: [
    {
      validator: (val) => val,
      message: t('请选择项目'),
      trigger: 'change',
    },
  ],
  groupIds: [
    {
      validator: () => groupList.value.length,
      message: t('请选择用户组'),
      trigger: 'change',
    },
  ],
};

watch(() => formData.value.projectCode, (val) => {
  groupList.value = [];
  curProject.value = projectList.value.find(i => i.englishName === val)
  if (curProject.value) {
    isDisabled.value = !curProject.value.permission
  }
}, {
  deep: true,
})


watch(() => curProject.value, (val) => {
  if (curProject.value) {
    isDisabled.value = !val.permission
  }
}, {
  deep: true,
})

const handleChangeTime = (value) => {
  currentActive.value = Number(value)
  formData.value.expiredAt = formatTimes(value)
};

const formatTimes = (value) => {
  const nowTimestamp = +new Date() / 1000
  const tempArr = String(nowTimestamp).split('')
  const dotIndex = tempArr.findIndex(i => i === '.')
  const nowSecond = parseInt(tempArr.splice(0, dotIndex).join(''), 10)
  return Number(value) + nowSecond
};
const handleChangCustom = () => {
  currentActive.value = 'custom'
};

const handleChangeCustomTime = (value) => {
  if (!/^[0-9]*$/.test(value)) {
    nextTick(() => {
      customTime.value = 1
    })
  } else if (customTime.value > 365) {
    nextTick(() => {
      customTime.value = 365
    })
  }
};

const handleSubmit = () => {
  formRef.value.validate().then(async () => {
    isLoading.value = true;
    if (currentActive.value === 'custom') {
      const timestamp = customTime.value * 24 * 3600
      formData.value.expiredAt = formatTimes(timestamp)
    }
    formData.value.groupIds = groupList.value.map(i => i.id);
    await http.applyToJoinGroup(formData.value).then(res => {
      Message({
        theme: 'success',
        message: t('提交成功'),
      });
      router.push({
        name: 'my-apply',
        params: {
          projectCode: formData.value.projectCode,
        }
      })
    })
    isLoading.value = false;
  });
};

const handleCancel = () => {
  router.push({
    name: 'my-permission'
  })
};

const handleChangeGroup = (values) => {
  groupList.value = values;
};
const handleGroupClear = (index) => {
  groupList.value.splice(index, 1);
};

const getUserInfo = () => {
  http.getUser().then(res => {
    formData.value.applicant = res.username;
  });
};

const handleSearchProject = (val) => {
  pageInfo.value.loadEnd = false;
  pageInfo.value.page = 1;
  pageInfo.value.projectName = val;
  projectList.value = [];
  getAllProjectList(val);
}

const getAllProjectList = (name = '') => {
  if (pageInfo.value.loadEnd || scrollLoading.value) {
    return
  }
  const { page, pageSize, projectName } = pageInfo.value;
  scrollLoading.value = true;
  http.getAllProjectList({
    page: page,
    pageSize: pageSize,
    projectName: projectName ? projectName : undefined,
  }).then(res => {
    pageInfo.value.loadEnd = !res.hasNext;
    pageInfo.value.page += 1;
    projectList.value.push(...res.records);
  }).finally(() => {
    scrollLoading.value = false;
  });
};

const getProjectByName = () => {
  const params = <any>{}
  if (formData.value.projectCode) {
    params.english_name = formData.value.projectCode;
  } 
  http.getAllProjectList(params).then(res => {
    if (res.records.length) {
      res.records.forEach(i => {
        i.hide = true;
      });
      projectList.value = [...res.records, ...projectList.value];
      curProject.value = res.records[0];
      isDisabled.value = curProject.value.permission;
    } else {
      formData.value.projectCode = ''
      setTimeout(() => {
        formRef.value.clearValidate()
      });
    }
  })
}

const handleToProjectManage = (project) => {
  const { routerTag, englishName } = project;
  switch (routerTag) {
    case 'v0':
        window.open(`/console/perm/apply-join-project?project_code=${englishName}`)
        break
    case 'v3':
        window.open(`${window.BK_IAM_URL_PREFIX}/apply-join-user-group`)
        break
  }
}

onMounted(async () => {
  formData.value.expiredAt = formatTimes(2592000);
  formData.value.projectCode = route?.query.project_code || tools.getCookie('X-DEVOPS-PROJECT-ID') || '';
  await getUserInfo();
  await getAllProjectList();
  await getProjectByName();
});

</script>

<template>
  <article class="apply-permission">
    <section class="apply-from-content">
      <bk-form
        ref="formRef"
        :rules="rules"
        class="group-form"
        :model="formData"
        label-width="150">
        <div class="form-group">
          <bk-form-item :label="t('项目')" required property="projectCode">
            <bk-select
              v-model="formData.projectCode"
              filterable
              :input-search="false"
              class="project-select"
              :scroll-loading="scrollLoading"
              @scroll-end="getAllProjectList"
              :remote-method="handleSearchProject"
            >
              <div v-for="(project, index) in projectList"
                :key="index">
                <bk-option
                    v-show="!project.hide"
                    :value="project.englishName"
                    :disabled="['v0', 'v3'].includes(project.routerTag)"
                    :label="project.projectName"
                >
                  <div
                    class="option-item">
                    {{ project.projectName }}
                    <i
                      v-if="['v0', 'v3'].includes(project.routerTag)"
                      v-bk-tooltips="$t('项目尚未升级到新版权限系统，点击前往旧版权限中心申请')"
                      class="permission-icon permission-icon-edit edit-icon"
                      @click="handleToProjectManage(project)"
                    >
                    </i>
                  </div>
                </bk-option>
              </div>
            </bk-select>
          </bk-form-item>
          <bk-form-item :label="t('选择用户组')" required>
            <group-search
              :groupList="groupList"
              :cur-project="curProject"
              :is-disabled="isDisabled"
              :project-code="formData.projectCode"
              @handle-change-select-group="handleChangeGroup"
            ></group-search>
          </bk-form-item>
        </div>
        <div class="form-group">
          <bk-form-item :label="t('已选用户组')" property="groupIds">
            <span class="empty-group" v-if="!groupList.length">{{ t('请先从上方选择用户组') }}</span>
            <div v-else class="selected-group">
              <span class="group-item" v-for="(group, index) in groupList" :key="group.id">
                {{ group.name }} 
                <span @click="handleGroupClear(index)" class="permission-icon permission-icon-close-samll clear-icon"></span>
              </span>
            </div>
          </bk-form-item>
          <bk-form-item :label="t('申请期限')">
            <div class="bk-button-group deadline-wrapper">
              <bk-button
                v-for="(item, key, index) in timeFilters"
                :key="index"
                @click="handleChangeTime(key)"
                :class="{
                  'is-selected': currentActive === Number(key),
                  'deadline-btn': true
                }">
                {{ item }}
              </bk-button>
              <bk-button
                class="deadline-btn"
                v-show="currentActive !== 'custom'"
                @click="handleChangCustom"
              >
                {{ t('自定义') }}
              </bk-button>
              <bk-input
                v-model="customTime"
                v-show="currentActive === 'custom'"
                class="custom-time-select"
                type="number"
                :showControl="false"
                placeholder="1-365"
                :min="1"
                :max="365"
                @input="handleChangeCustomTime"
              >
                <template #suffix>
                  <div class="suffix-slot">
                      {{ t('天') }}
                  </div>
                </template>
              </bk-input>
            </div>
          </bk-form-item>
          <bk-form-item :label="t('理由')" required property="reason">
            <bk-input
              v-model="formData.reason"
              class="reason-textarea"
              type="textarea"
              :maxlength="100"
              :placeholder="t('请输入')"
            >
            </bk-input>
          </bk-form-item>
          <bk-form-item>
            <bk-button
              class="form-btn"
              theme="primary"
              :loading="isLoading"
              @click="handleSubmit"
            >
              {{ t('提交') }}
            </bk-button>
            <bk-button
              class="form-btn"
              :loading="isLoading"
              @click="handleCancel"
            >
              {{ t('取消') }}
            </bk-button>
          </bk-form-item>
        </div>
      </bk-form>
    </section>
  </article>
</template>

<style lang="postcss" scoped>
  .apply-permission {
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  .apply-from-content {
    overflow: auto;
    &::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
  }
  .group-form {
    padding: 24px;
    .form-group {
      width: 100%;
      padding: 24px;
      margin-bottom: 16px;
      background-color: #fff;
      box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
    }
    .empty-group {
      font-size: 12px;
      color: #C4C6CC;
    }
    .project-select {
      width: 280px;
    }
    .deadline-wrapper {
        display: flex;
    }
    .deadline-btn {
        min-width: 100px;
    }
    .custom-time-select {
        width: 110px;
        position: relative;
        left: -1px;
    }
    .reason-textarea {
      width: 440px;
      :deep(textarea) {
        width: auto;
      }
    }
    .form-btn {
      width: 88px;
      margin-right: 8px;
    }
    .suffix-slot {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 35px;
      background: #fff;
    }
  }
  .group-item {
    display: inline-block;
    height: 22px;
    line-height: 22px;
    font-size: 12px;
    padding: 0 6px;
    margin-right: 10px;
    background: #F0F1F5;
    border-radius: 2px;
    .clear-icon {
      cursor: pointer;
      color: #989ca7;
      font-size: 16px;
    }
  }
  .option-item {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    &:hover {
      .edit-icon {
        display: block;
      }
    }
    .edit-icon {
      display: none;
      color: blue;
      cursor: pointer;
    }
  }
</style>
