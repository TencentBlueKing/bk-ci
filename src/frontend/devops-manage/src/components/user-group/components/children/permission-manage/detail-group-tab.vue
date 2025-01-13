<template>
  <bk-loading
    :loading="detailGroupTable.isLoading"
    :zIndex="100"
    class="group-tab"
  >
    <div class="manage-content-project" v-if="projectTable.length">
      <p class="project-group">
        <span>{{t("资源授权")}}</span>
      </p>
      <div
        class="project-group-table"
        v-for="item in projectTable"
        :key="item.resourceType"
      >
        <bk-collapse-panel
          v-model="item.activeFlag"
          :item-click="(type) => collapseClick(type, HandoverType.AUTHORIZATION)"
          :name="item.resourceType"
        >
          <template #header>
            <p class="group-title">
              <i :class="getShapeIconClass(item.activeFlag)" />
              <img
                v-if="item.resourceType && detailGroupTable.getServiceIcon(item.resourceType)"
                :src="detailGroupTable.getServiceIcon(item.resourceType)"
                class="service-icon"
              >
              {{item.resourceTypeName}} ({{ item.resourceType }})
              <span class="group-num">{{item.count}}</span>
            </p>
          </template>
          <template #content>
            <TabTable
              v-if="item.tableData"
              :data="item.tableData"
              :isAuthorizations="true"
              :pagination="item.pagination"
              :resource-type="item.resourceType"
              :resource-name="item.resourceTypeName"
              :loading="item.tableLoading"
              :group-name="item.resourceTypeName"
              :type="HandoverType.AUTHORIZATION"
              @page-limit-change="pageLimitChange"
              @page-value-change="pageValueChange"
            />
          </template>
        </bk-collapse-panel>
      </div>
    </div>
    <div class="manage-content-resource" v-if="sourceTable.length">
      <p class="project-group">
        <span>{{t("用户组")}}</span>
      </p>
      <div
        class="project-group-table"
        v-for="item in sourceTable"
        :key="item.resourceType"
      >
        <bk-collapse-panel
          v-model="item.activeFlag"
          :item-click="(type) => collapseClick(type, HandoverType.GROUP)"
          :name="item.resourceType"
        >
          <template #header>
            <p class="group-title">
              <i :class="getShapeIconClass(item.activeFlag)" />
              <img
                v-if="item.resourceType && detailGroupTable.getServiceIcon(item.resourceType)"
                :src="detailGroupTable.getServiceIcon(item.resourceType)"
                class="service-icon"
              >
              {{item.resourceTypeName}} ({{ item.resourceType }})
              <span class="group-num">{{item.count}}</span>
            </p>
          </template>
          <template #content>
            <TabTable
              v-if="item.tableData"
              :data="item.tableData"
              :pagination="item.pagination"
              :resource-type="item.resourceType"
              :resource-name="item.resourceTypeName"
              :loading="item.tableLoading"
              :group-name="item.resourceTypeName"
              :type="HandoverType.GROUP"
              @page-limit-change="pageLimitChange"
              @page-value-change="pageValueChange"
            />
          </template>
        </bk-collapse-panel>
      </div>
    </div>
  </bk-loading>
</template>

<script setup name="GroupTab">
import { useI18n } from 'vue-i18n';
import { defineProps, defineEmits, computed } from 'vue';
import userDetailGroupTable, { HandoverType } from '@/store/userDetailGroupTable';
import TabTable from './detail-tab-table.vue';

const props = defineProps({
  sourceList: {
    type: Array,
    default: () => [],
  },
  pageLimitChange: {
    type: Function,
    default: () => {},
  },
  pageValueChange: {
    type: Function,
    default: () => {},
  },
});
const { t } = useI18n();
const detailGroupTable = userDetailGroupTable();
const projectTable = computed(() => props.sourceList.filter(item => item.type === HandoverType.AUTHORIZATION));
const sourceTable= computed(() => props.sourceList.filter(item => item.type === HandoverType.GROUP));

const emit = defineEmits(['collapseClick']);

function getShapeIconClass(activeFlag) {
  return `shape-icon manage-icon manage-icon-${activeFlag ? 'down' : 'right'}-shape`
}
/**
 * 折叠面板点击事件
 * @param id 折叠面板唯一标志
 */
function collapseClick(resourceType, flag) {
  emit('collapseClick', resourceType.name, flag);
}
</script>

<style lang="less" scoped>
.group-tab {
  width: calc(100% - 8px);

  .manage-content-common {
    background: #FFFFFF;
    padding: 16px 24px;
    box-shadow: 0 2px 4px 0 #1919290d;
  }
  .manage-content-project {
    margin-bottom: 15px;
    .manage-content-common();
  }
   
  .manage-content-resource {
    .manage-content-common();
  }
  
  .project-group {
    display: flex;
    font-weight: 700;
    font-size: 14px;
    color: #63656E;
    margin-bottom: 16px;
    letter-spacing: 0;
    line-height: 22px;

    .describe {
      width: 640px;
      margin-left: 24px;
      font-size: 12px;
      color: #4D4F56;
      font-weight: 500;
    }

    .text-blue{
      color: #699DF4;
    }
  }
  
  .project-group-table {
    width: 100%;
    height: 100%;
    margin-bottom: 16px;

    .permission {
      margin-bottom: 10px;
    }
  
    .bk-table {
      border: 1px solid #dcdee5;
    }
  
    ::v-deep .bk-collapse-content {
      padding: 0 !important;
    }
  
    .group-title {
      display: flex;
      align-items: center;
      width: 100%;
      height: 26px;
      line-height: 26px;
      padding-left: 10px;
      background: #EAEBF0;
      border-radius: 2px;
      font-size: 12px;
      color: #313238;
      cursor: pointer;

      .service-icon {
        width: 14px;
        height: 14px;
        margin-right: 5px;
      }

      .shape-icon {
        color: #989ca7;
        margin-right: 10px;
      }
    }
  
    .group-num {
      display: inline-block;
      width: 23px;
      height: 16px;
      line-height: 16px;
      background: #F0F1F5;
      border-radius: 2px;
      font-size: 12px;
      color: #979BA5;
      letter-spacing: 0;
      text-align: center;
      margin-left: 5px;
    }
  
    .operation-btn {
      display: flex;
      justify-content: space-around;
    }
  }
}
</style>
