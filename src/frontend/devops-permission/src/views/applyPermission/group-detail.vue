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

const fetchGroupPermissionDetail = async () => {
  const { id } = props.groupInfo;
  if (id) {
    await http.getGroupPermissionDetail(props.groupInfo.id).then(res => {
      groupPermissionDetail.value = res;
    }).catch(() => []);
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

const handleShowInstances = () => {
  showInstancesDetail.value = true;
}
</script>

<template>
  <bk-sideslider
    v-model:isShow="showDetail"
    :width="650"
    quick-close
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
                {{ data.name }}
              </template>
            </bk-table-column>
            <bk-table-column :label="t('资源实例')" show-overflow-tooltip>
              <template #default="{ data }">
                <div v-if="data.relatedResourceInfo" class="resources-info">
                  <bk-popover
                    theme="light"
                    placement="bottom"
                  >
                    {{ data.relatedResourceInfo?.name }}: {{ t('已选择个流水线', [data.relatedResourceInfo?.instances?.path.length]) }}
                    <template #content>
                      <div class="resources-tips">
                        <div>1</div>
                        <div>1</div>
                        <div>1</div>
                        <div>1</div>
                      </div>
                    </template>
                  </bk-popover>
                  <i class="permission-icon permission-icon-review review-icon" @click="handleShowInstances"></i>
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
    :width="400"
    quick-close
  >
    <template #default>
      123123
    </template>
  </bk-sideslider>
</template>

<style lang="postcss" scoped>
  :deep(.bk-sideslider-title) {
    line-height: normal !important;
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
  .resources-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .resources-tips {
      padding: 0 10px;
    }
  }
  .review-icon {
    margin-right: 20px;
    color: #3a84ff;
    cursor: pointer;
  }
</style>
