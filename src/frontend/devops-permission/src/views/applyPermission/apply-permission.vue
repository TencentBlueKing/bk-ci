<script setup lang="ts">
import PermissionHeader from '@/components/permission-header.vue';
import GroupSearch from './group-search.vue';
import {
  ref,
  computed,
  nextTick,
} from 'vue';
import { useI18n } from 'vue-i18n';
const { t } = useI18n();

const navs = ref([
  { name: '我的权限', url: `/permission/${'hwweng'}` },
  { name: '权限申请' }
]);
const formData = ref<any>({
  expireTime: '',
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

const handleChangeTime = (value) => {
    currentActive.value = Number(value)
    formData.value.expireTime = formatTimes(value)
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
  if (currentActive.value === 'custom') {
    const timestamp = customTime.value * 24 * 3600
    formData.value.expireTime = formatTimes(timestamp)
  }
  console.log(formData.value)
}
</script>

<template>
  <article class="apply-permission">
    <permission-header :navs="navs"></permission-header>
    <section class="apply-from-content">
      <bk-form
        class="group-form"
        :model="formData"
        label-width="150">
        <div class="form-group">
          <bk-form-item :label="t('项目')">
            <bk-select class="project-select"></bk-select>
          </bk-form-item>
          <bk-form-item :label="t('选择用户组')">
            <group-search></group-search>
          </bk-form-item>
        </div>
        <div class="form-group">
          <bk-form-item :label="t('已选用户组')">
            <span class="empty-group">{{ t('请先从上方选择用户组') }}</span>
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
              @click="handleSubmit"
            >
              {{ t('提交') }}
            </bk-button>
            <bk-button
              class="form-btn"
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
</style>
