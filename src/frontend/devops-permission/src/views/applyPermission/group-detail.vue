<script setup lang="ts">
import {
  ref,
  watch,
  computed,
} from 'vue';
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
const { t } = useI18n();

import { DownShape, RightShape } from 'bkui-vue/lib/icon'
const emits = defineEmits(['hidden-detail']);

const props = defineProps({
  groupInfo: Object,
  isShow: Boolean,
  isDetailLoading: Boolean,
});
const showDetail = ref(false);
const isLoading= ref(false);
const groupPermissionDetailMap = ref({});
const showPermFlagMap = ref([true, false]);

const fetchGroupPermissionDetailMap = async () => {
  const { id } = props.groupInfo;
  if (id) {
    await http.getGroupPermissionDetail(props.groupInfo.id).then(res => {
      for (var item in res) {
        groupPermissionDetailMap.value[item] = res[item].map(i => {
          return {
              ...i,
              expand: false,
              showExpandBtn: i.relatedResourceInfos.reduce((data: any, item :any) => data + item.instance.length, 0) >= 3
          }
        });
      }
    }).catch(() => {
      groupPermissionDetailMap.value = {};
    });
    isLoading.value = false;
  }
};

const toggleContent = (index) => {
  showPermFlagMap.value[index] = !showPermFlagMap.value[index]
};

watch(() => props.isShow, (val) => {
  showDetail.value = props.isShow;
  isLoading.value = props.isDetailLoading;
  if (val) {
    fetchGroupPermissionDetailMap();
  } else {
    showPermFlagMap.value = [true, false];
    groupPermissionDetailMap.value = {};
  }
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
      :width="800"
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
            <div class="user-group-perm">
              <div class="perm-item" v-for="(item, key, index) in groupPermissionDetailMap" :key="index">
                <header class="header" @click="toggleContent(index)">
                  <down-shape v-if="showPermFlagMap[index]" />
                  <right-shape v-else />
                  <span class="name">{{ key }}</span>
                </header>
                <div class="content" v-show="showPermFlagMap[index]">
                  <bk-table
                    v-bkloading="{ isLoading: !showPermFlagMap[index] }"
                    class="resources-table"
                    :data="item"
                    :border="['row', 'outer']">
                    <bk-table-column :label="t('操作')" width="250" show-overflow-tooltip>
                      <template #default="{ data }">
                        {{ data?.name }}
                      </template>
                    </bk-table-column>
                    <bk-table-column :label="t('操作对象')" width="430">
                      <template #default="{ data }">
                      <div v-if="data?.relatedResourceInfos.length" :class="{
                        'resources-info': true,
                        'show': data.expand
                      }">
                        <section>
                          <div v-for="(item, index) in data.relatedResourceInfos" :key="index">
                            <div class="resources-content">
                              <span v-if="item?.type.includes('pipeline')">
                                {{ t('共N条XX', [item?.instance.length, item?.name]) }}
                              </span>
                              <span v-else-if="item?.type.includes('project')">
                                {{ t('共N个XX', [1, item?.name]) }}
                              </span>
                              <span v-else-if="item?.type.includes('space')">
                                {{ t('共N个监控平台空间', [item?.instance.length, item?.name]) }}
                              </span>
                              <span v-else>
                                {{ t('共N个XX', [item?.instance.length, item?.name]) }}
                              </span>
                            </div>
                            <div v-if="item?.type.includes('project')" class="resources-content" >
                              <div>
                                {{ item?.instance[0][0].name }}
                              </div>
                            </div>
                            <div v-else class="resources-content" v-for="(path, pathIndex) in item?.instance" :key="pathIndex">
                              <div class="item">
                                <span v-for="(item, index) in path" :key="item.id">
                                  {{ item.name }} {{ index !== path.length -1 ? ' / ' : '' }} 
                                </span>
                              </div>
                            </div>
                          </div>
                        </section>
                        <bk-button class="expand-btn" v-if="data.showExpandBtn && !data.expand" text @click="data.expand = true">{{ t('展开') }}</bk-button>
                        <bk-button class="expand-btn" v-if="data.showExpandBtn && data.expand" text @click="data.expand = false">{{ t('收起') }}</bk-button>
                      </div>
                      <span v-else>--</span>
                      </template>
                    </bk-table-column>
                  </bk-table>
                </div>
              </div>
            </div>
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

  :deep(.bk-modal-content) {
    background-color: #f5f7fb;
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
  }
  .resources-table {
      width: 682px !important;
      ::v-deep .cell {
      line-height: 30px !important;
    }
  }
  .resources-info {
    display: flex;
    overflow: hidden;
    white-space: normal !important;
    text-overflow: ellipsis;
    word-wrap: break-word;
    max-height: 90px;
    &.show {
      max-height: 900px !important;
    }
  }
  .resources-content {
      .item {
          position: relative;
          display: inline-block;
          max-width: 380px;
          line-height: 15px;
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
      }
      .expand-btn {
          position: absolute;
          top: 10px;
          left: calc(100% + 20px);
      }
  }
  .expand-btn {
      color: #3c96ff;
      margin-left: 20px;
  }
  .resources-tips {
    padding: 10px;
  }

  .perm-item {
    background: #fff;
    border-radius: 2px;
    border: 1px solid #fff;
    box-shadow: 0 1px 2px 0 rgba(49,50,56,.1);
    margin-bottom: 20px;
    .header {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        position: relative;
        padding: 0 10px;
        height: 40px;
        line-height: 40px;
        font-size: 12px;
        color: #63656e;
        border-radius: 2px;
        cursor: pointer;
    }
    .content {
      padding: 0 30px 20px;
    }
    .shape-icon {
      position: relative;
      bottom: 1px;
    }
    .name {
      margin-left: 8px;
    }
}
</style>
