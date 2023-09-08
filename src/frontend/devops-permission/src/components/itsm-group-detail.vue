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
      groupPermissionDetail.value = res.map(i => {
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
  <section style="padding: 20px;">
    <bk-table
        class="resources-table"
        :data="groupPermissionDetail"
        :border="['row', 'outer']">
        <bk-table-column :label="t('操作')" width="250" show-overflow-tooltip>
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
</style>
