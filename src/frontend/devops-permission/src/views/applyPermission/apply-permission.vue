<script setup lang="ts">
import http from '@/http/api';
import PermissionHeader from '@/components/permission-header.vue';
import GroupSearch from './group-search.vue';
import {
  ref,
  onMounted,
  nextTick,
} from 'vue';
import { useI18n } from 'vue-i18n';
import { Message } from 'bkui-vue';
import { useRoute, useRouter } from 'vue-router';
const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const groupList = ref([]);
const projectList = ref([]);
const userName = ref('');
const navs = ref([
  { name: '我的权限', url: `/permission/${'hwweng'}/permission` },
  { name: '权限申请' }
]);
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

const getAllProjectList = () => {
  http.getAllProjectList().then(res => {
    projectList.value = res;
  });
};
onMounted(() => {
  formData.value.expiredAt = formatTimes(2592000);
  formData.value.projectCode = route.params.projectCode || '';
  getUserInfo();
  getAllProjectList();
});

</script>

<template>
  <article class="apply-permission">
    <section class="apply-from-content">
      <bk-form
        ref="formRef"
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
            >
              <bk-option
                v-for="(project, index) in projectList"
                :key="index"
                :value="project.projectCode"
                :label="project.projectName"
              />
            </bk-select>
          </bk-form-item>
          <bk-form-item :label="t('选择用户组')" required property="groupIds">
            <group-search
              :groupList="groupList"
              :project-code="formData.projectCode"
              @handle-change-select-group="handleChangeGroup"
            ></group-search>
          </bk-form-item>
        </div>
        <div class="form-group">
          <bk-form-item :label="t('已选用户组')">
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
          <bk-form-item :label="t('理由')">
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
      border-left: 1px solid #c4c6cc;
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
</style>
