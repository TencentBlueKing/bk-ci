<script setup lang="ts">
import {
  Plus,
  Search,
  ArrowsRight,
  CloseLine,
} from 'bkui-vue/lib/icon';
import { InfoBox } from 'bkui-vue';
import {
  ref,
  watch,
} from 'vue';
import http from '@/http/api';

import { sharedProps } from '../plugin-fail-analysis/common/props-type';
import useFilter from '@/composables/use-filter';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const emit = defineEmits(['change']);
defineProps(sharedProps);

const {
  handleChange,
} = useFilter(emit);

// 状态
const isShow = ref(false);
const searchStr = ref('');
const isLoading = ref(false);
const isSubmitting = ref(false);
const changeFlag = ref(false);

let originProjectPluginList = [];
let originProjectOptionPluginList = [];
const projectPluginList = ref([]);
const projectOptionPluginList = ref([]);

// 事件
const handleToggleShow = () => {
  isShow.value = !isShow.value;
};

const handleCancel = () => {
  if (changeFlag.value) {
    InfoBox({
      title: t('确认离开当前页？'),
      subTitle: t('离开将会导致未保存信息丢失'),
      confirmText: t('离开'),
      onConfirm() {
        isShow.value=!isShow.value;
        searchStr.value = '';
        changeFlag.value = false;
      },
      headerAlign: 'center',
      footerAlign: 'center',
      contentAlign: 'center'
    })
  } else {
    searchStr.value = '';
    isShow.value = !isShow.value;
  }
};

const getPluginList = () => {
  isLoading.value = true;
  Promise.all([
    http.getProjectShowPluginList(),
    http.getProjectOptionPluginList({ keyword: searchStr.value, page: 1, pageSize: 100 }),
  ])
    .then(([
      pluginList,
      optionPluginList,
    ]) => {
      originProjectPluginList = [...pluginList.atomBaseInfos];
      projectPluginList.value = [...pluginList.atomBaseInfos];
      originProjectOptionPluginList = [...optionPluginList.records];
      projectOptionPluginList.value = [...optionPluginList.records];
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const restore = () => {
  projectPluginList.value = originProjectPluginList;
  projectOptionPluginList.value = originProjectOptionPluginList;
};

const addPlugin = (plugin) => {
  changeFlag.value = true;
  projectPluginList.value.push(plugin);
  const index = projectOptionPluginList.value.findIndex(item => item === plugin);
  projectOptionPluginList.value.splice(index, 1);
};

const minusPlugin = (plugin) => {
  changeFlag.value = true;
  projectOptionPluginList.value.push(plugin);
  const index = projectPluginList.value.findIndex(item => item === plugin);
  projectPluginList.value.splice(index, 1);
};

const submit = async () => {
  try {
    isSubmitting.value = true;
    const newPluginList = [];
    const deletePluginList = [];
    originProjectPluginList.forEach((originPlugin) => {
      const hasDelete = projectPluginList.value.every(plugin => plugin.atomCode !== originPlugin.atomCode);
      if (hasDelete) {
        deletePluginList.push(originPlugin);
      }
    });
    projectPluginList.value.forEach((plugin) => {
      const isNew = originProjectPluginList.every(originPlugin => plugin.atomCode !== originPlugin.atomCode);
      if (isNew) {
        newPluginList.push(plugin);
      }
    });
    if (deletePluginList.length) {
      await http.deleteProjectPlugin({
        atomBaseInfos: deletePluginList,
      });
    }
    if (newPluginList.length) {
      await http.addProjectPlugin({
        atomBaseInfos: newPluginList,
      });
    }
    handleToggleShow();
  } catch (error) {
    console.error(error);
  } finally {
    const atomCodes = projectPluginList.value.map(item => item.atomCode)
    handleChange({ atomCodes })
    isSubmitting.value = false;
  }
};

// 触发
watch(
  searchStr,
  getPluginList,
);

watch(
  isShow,
  () => isShow.value && getPluginList(),
);
</script>

<template>
  <bk-button @click="handleToggleShow">
    <plus
      class="mr5"
      width="22px"
      height="22px"
    />
    {{ t('Add plugin') }}
  </bk-button>
  <bk-sideslider
    class="add-plugin-slider"
    width="640"
    :title="t('Add plugin')"
    quick-close
    :before-close="handleCancel"
    v-model:isShow="isShow"
  >
    <bk-loading
      class="add-plugin"
      :loading="isLoading"
    >
      <section class="add-plugin-main">
        <section class="plugin-list">
          <bk-input
            class="list-header"
            clearable
            v-model="searchStr"
            @change="changeFlag = true"
          >
            <template #suffix>
              <span class="input-icon">
                <search />
              </span>
            </template>
          </bk-input>
          <ul class="list-main">
            <li
              class="list-item"
              :key="plugin.atomCode"
              v-for="plugin in projectOptionPluginList"
              @click="addPlugin(plugin)"
            >
              {{ plugin.atomName }}
              <arrows-right width="25px" height="25px" class="list-icon"></arrows-right>
            </li>
            <bk-exception
              type="empty"
              scene="part"
              v-if="projectOptionPluginList.length <= 0"
            />
          </ul>
        </section>
        <section class="use-list">
          <span class="list-header use-header">{{ t('The selected plugin') }} （{{ projectPluginList.length }}）</span>
          <ul class="list-main">
            <li
              class="list-item"
              :key="plugin.atomCode"
              v-for="plugin in projectPluginList"
              @click="minusPlugin(plugin)"
            >
              {{ plugin.atomName }}
              <close-line class="list-icon"></close-line>
            </li>
            <bk-exception
              type="empty"
              scene="part"
              v-if="projectPluginList.length <= 0"
            />
          </ul>
        </section>
      </section>
      <section class="add-plugin-footer">
        <section>
          <bk-button theme="primary" class="mr8" :loading="isSubmitting" @click="submit">{{ t('Submit') }}</bk-button>
          <bk-button @click="handleCancel">{{ t('Cancel') }}</bk-button>
        </section>
        <!-- <bk-button @click="restore">Restore default</bk-button> -->
      </section>
    </bk-loading>
  </bk-sideslider>
</template>

<style lang="scss" scoped>
.add-plugin-slider {
  :deep(.bk-sideslider-footer) {
    height: 0;
  }
}

.add-plugin {
  height: calc(100vh - 61px);
  .add-plugin-main {
    height: calc(100% - 48px);
    display: flex;
  }
  .add-plugin-footer {
    height: 48px;
    background: #FAFBFD;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 24px;
  }
}

.plugin-list, .use-list {
  width: 50%;
  height: 100%;
  overflow-y: hidden;
}

.plugin-list {
  border-right: 1px solid #DCDEE5;
}

.list-header {
  margin: 24px 24px 18px;
  width: 271px;
  &.use-header {
    display: block;
    font-size: 12px;
    color: #313238;
    background: #F5F7FA;
    border-radius: 2px;
    line-height: 32px;
    text-indent: 24px;
  }
}

.list-main {
  height: calc(100% - 74px);
  overflow-y: auto;
  .list-item {
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 32px;
    line-height: 32px;
    padding: 0 24px 0 32px;
    .list-icon {
      display: none;
    }
    &:hover {
      background: #EDF4FF;
      color: #3A84FF;
      .list-icon {
        display: block;
      }
    }
  }
}

.input-icon {
  display: block;
  padding: 0 10px;
  font-size: 16px;
  color: #c4c6cc;
  align-self: center;
}
</style>
