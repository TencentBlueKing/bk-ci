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

import { DownShape, RightShape } from 'bkui-vue/lib/icon'
const emits = defineEmits(['hidden-detail']);

const showDetail = ref(false);
const showInstancesDetail = ref(false);
const isLoading= ref(false);
const groupPermissionDetailMap = ref({});
const showPermFlagMap = ref([true, false]);
const relatedResourceInfo = ref({});
const groupId = route.query['group_id'] || ''

const fetchGroupPermissionDetailMap = async () => {
  if (groupId) {
    await http.getGroupPermissionDetail(groupId).then(res => {
      for (var item in res) {
        groupPermissionDetailMap.value[item] = res[item].map(i => {
          return {
              ...i,
              expand: false,
          }
        });
      }
    }).catch(() => {
      groupPermissionDetailMap.value = {};
    });
    isLoading.value = false;
  }
};

const handleShowInstances = (data, name) => {
  relatedResourceInfo.value = { ...data, actionName: name };
  showInstancesDetail.value = true;
}

const toggleContent = (index) => {
  showPermFlagMap.value[index] = !showPermFlagMap.value[index]
}
onMounted(() => {
    fetchGroupPermissionDetailMap();
});
</script>

<template>
  <section style="padding: 20px;">
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
                          <span v-else-if="data.relatedResourceInfo?.instances.type.includes('space')">
                            {{ t('共N个监控平台空间', [1, data.relatedResourceInfo?.instances.name]) }}
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
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="postcss" scoped>
    .resources-table {
      width: 682px !important;
        ::v-deep .cell {
            line-height: 46px !important;
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
        height: 24px;
        line-height: 24px;
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
