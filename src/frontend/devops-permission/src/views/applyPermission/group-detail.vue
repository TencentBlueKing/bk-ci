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
const showInstancesDetail = ref(false);
const isLoading= ref(false);
const groupPermissionDetail = ref([]);
const relatedResourceInfo = ref({});

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

const handleShowInstances = (data, name) => {
  relatedResourceInfo.value = { ...data, actionName: name };
  showInstancesDetail.value = true;
}
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
              :data="groupPermissionDetail"
              :border="['row', 'outer']">
              <bk-table-column :label="t('操作')" width="150" show-overflow-tooltip>
                <template #default="{ data }">
                  {{ data?.name }}
                </template>
              </bk-table-column>
              <bk-table-column :label="t('资源实例')" show-overflow-tooltip>
                <template #default="{ data }">
                  <div v-if="data?.relatedResourceInfo" class="resources-info">
                    <bk-popover
                      theme="light"
                    >
                      <span v-if="data.relatedResourceInfo?.instances?.path.length > 1">
                        {{ data.relatedResourceInfo?.name }}: {{ t('已选择个流水线', [data.relatedResourceInfo?.instances?.path.length]) }}
                      </span>
                      <span v-else>
                        {{ data.relatedResourceInfo?.name }}:
                        <span v-for="(item, index) in data.relatedResourceInfo?.instances?.path[0]" :key="item.id">
                          {{ item.name }}{{ index !== data.relatedResourceInfo?.instances?.path[0].length - 1 ? ' / ' : '' }} 
                        </span>
                      </span>
                      <template #content>
                        <div v-if="data.relatedResourceInfo?.instances?.path.length > 1" class="resources-tips">
                          <div v-for="(path, pathIndex) in data.relatedResourceInfo?.instances?.path" :key="pathIndex" class="path-item">
                            <span v-for="(item, index) in path" :key="item.id">
                              {{ item.name }} {{ index !== path.length -1 ? ' / ' : '' }} 
                            </span>
                          </div>
                        </div>
                        <div v-else class="resources-tips">
                          <span v-for="(item, index) in data.relatedResourceInfo?.instances?.path[0]" :key="item.id">
                            {{ item.name }} {{ index !== data.relatedResourceInfo?.instances?.path[0].length -1 ? ' / ' : '' }} 
                          </span> 
                        </div>
                      </template>
                    </bk-popover>
                    <i class="permission-icon permission-icon-review review-icon" @click="handleShowInstances(data.relatedResourceInfo, data.name)"></i>
                  </div>
                  <span v-else>--</span>
                </template>
              </bk-table-column>
            </bk-table>
          </bk-loading>
        </div>
      </template>
    </bk-sideslider>
    <bk-sideslider
      v-model:isShow="showInstancesDetail"
      :width="500"
      :title="t('操作【】的资源实例', [relatedResourceInfo.actionName])"
      quick-close
    >
      <template #default>
        <bk-tab
          active="instances"
          type="unborder-card"
        >
          <bk-tab-panel
            name="instances"
            :label="`${relatedResourceInfo.name} ${t('实例')}`"
          >
            <div class="resource-instance">
              <div class="header">
                {{ t('拓扑实例') }}:
              </div>
              <div class="content">
                <p class="instance-title">{{ relatedResourceInfo.instances.name}}({{ relatedResourceInfo.instances.path.length }})</p>
                <div class="instance-item">
                  <div v-for="(path, pathIndex) in relatedResourceInfo?.instances?.path" :key="pathIndex">
                    <span v-for="(item, index) in path" :key="item.id">
                      {{ item.name }} {{ index !== path.length -1 ? ' / ' : '' }} 
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </bk-tab-panel>
        </bk-tab>
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
  .resources-tips {
    padding: 10px;
  }
  .resources-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .path-item {
      height: 30px;
      line-height: 30px;
    }
  }
  .review-icon {
    margin-right: 20px;
    color: #3a84ff;
    cursor: pointer;
  }
  .detail-side {
    z-index: 1111;
  }
  .resource-instance {
    position: relative;
    border: 1px solid #dcdee5;
    border-radius: 2px;
    background: #fff;
    box-shadow: 0 1px 2px 0 hsl(0deg 0% 100% / 30%);
    z-index: 1;
    &:before {
      content: "";
      position: absolute;
      top: 20px;
      width: 3px;
      height: 14px;
      background: #3a84ff;
    }
    .header {
      padding: 19px 20px;
      display: flex;
      justify-content: space-between;
      font-size: 12px;
    }
    .content {
      padding: 0 20px 19px;
    }
    .instance-title {
      font-size: 12px;
      color: #63656e;
    }
    .instance-item {
      margin-top: 10px;
      padding: 13px 16px;
      max-height: 186px;
      overflow-y: auto;
      background: #f7f9fb;
      font-size: 12px;
      color: #63656e;
    }
  }
</style>
