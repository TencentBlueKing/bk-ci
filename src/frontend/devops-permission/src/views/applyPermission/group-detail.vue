<script setup lang="ts">
import {
  ref,
  watch,
  computed,
} from 'vue';
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
const { t } = useI18n();

import { Error } from 'bkui-vue/lib/icon'
const emits = defineEmits(['hidden-detail']);

const props = defineProps({
  groupInfo: Object,
  isShow: Boolean,
  isDetailLoading: Boolean,
});
const showDetail = ref(false);
const isLoading= ref(false);
const groupPermissionDetail = ref([]);

const fetchGroupPermissionDetail = async () => {
  const { id } = props.groupInfo;
  if (id) {
    await http.getGroupPermissionDetail(props.groupInfo.id).then(res => {
      groupPermissionDetail.value = res;
    }).catch(() => {
      groupPermissionDetail.value = [];
    });
    isLoading.value = false;
  }
};
watch(() => props.isShow, (val) => {
  showDetail.value = props.isShow;
  isLoading.value = props.isDetailLoading;
  if (val) fetchGroupPermissionDetail();
}, { 
  immediate: true,
});

const handleHidden = () => {
  emits('hidden-detail', false);
};
</script>

<template>
  <section>
    <bk-sideslider
      v-model:isShow="showDetail"
      :width="750"
      quick-close
      ext-cls="detail-side"
      @hidden="handleHidden"
    >
      <template #header>
        <div>
          <p class="group-name"> {{ t('用户组【】的详情', [groupInfo.name]) }}</p>
          <p class="group-id">ID: {{ groupInfo.id }}</p>
        </div>
      </template>
      <template #default>
        <div class="detail-content">
          <bk-loading :loading="isLoading">
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
          </bk-loading>
        </div>
      </template>
    </bk-sideslider>
  </section>
</template>

<style lang="postcss" scoped>
  .detail-side {
    :deep(.bk-sideslider-title) {
      line-height: normal !important;
    }
  }
  :deep(.bk-modal-content) {
    height: 100%;
  }
  :deep(.bk-table),
  :deep(.bk-table-body) {
    overflow: visible !important;
  }
  :deep(.bk-popover-content) {
    z-index: 99 !important;
  }
  .group-name {
    padding-top: 8px;
    font-size: 16px;
    color: #313238;
  }
  .group-id {
    padding-top: 3px;
    font-size: 12px;
    color: #979BA5;
  }
  .detail-content {
    padding: 20px;
    height: 100%;
  }
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
  .resources-tips {
    padding: 10px;
  }
</style>
