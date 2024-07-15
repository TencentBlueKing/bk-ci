<template>
  <bk-loading :loading="groupTableStore.isLoading" class="group-tab">
    <div class="manage-content-project" v-if="projectTable">
      <p class="project-group">{{t("项目级用户组")}}</p>
      <div class="project-group-table">
        <bk-collapse-panel v-model="activeFlag">
          <template #header>
            <p class="group-title">
              <i class="permission-icon permission-icon-down-shape"></i>
              {{ projectTable.resourceTypeName }} ({{ projectTable.resourceType }})
              <span class="group-num">{{projectTable.count}}</span>
            </p>
          </template>
          <template #content>
            <TabTable
              v-if="projectTable.count && projectTable.tableData.length"
              :is-show-operation="isShowOperation"
              :data="projectTable.tableData"
              :resource-type="projectTable.resourceType"
              :group-total="projectTable.count"
              :limit="projectTable.limit"
              :pagination="projectTable.pagination"
              :current="projectTable.current"
              :selected-data="selectedData"
              :has-next="projectTable.hasNext"
              :group-name="t('用户组')"
              @handle-renewal="handleRenewal"
              @handle-hand-over="handleHandOver"
              @handle-remove="handleRemove"
              @get-select-list="getSelectList"
              @handle-select-all-data="handleSelectAllData"
              @handle-load-more="handleLoadMore"
              @handle-clear="handleClear"
              @page-limit-change="pageLimitChange"
              @page-value-change="pageValueChange"
            />
          </template>
        </bk-collapse-panel>
      </div>
    </div>
    <div class="manage-content-resource" v-if="sourceTable.length">
      <p class="project-group">{{t("资源级用户组")}}</p>
      <div class="project-group-table" v-for="item in sourceTable" :key="item.resourceType">
        <bk-collapse-panel v-model="item.activeFlag" :item-click="collapseClick" :name="item.resourceType">
          <template #header>
            <p class="group-title">
              <i class="permission-icon permission-icon-down-shape"></i>
              {{item.resourceTypeName}} ({{ item.resourceType }})
              <span class="group-num">{{item.count}}</span>
            </p>
          </template>
          <template #content>
            <TabTable
              v-if="item.count"
              :is-show-operation="isShowOperation"
              :data="item.tableData"
              :resource-type="item.resourceType"
              :group-total="item.count"
              :pagination="item.pagination"
              :limit="item.limit"
              :current="item.current"
              :selected-data="selectedData"
              :has-next="item.hasNext"
              :loading="item.tableLoading"
              :group-name="item.resourceTypeName"
              @handle-renewal="handleRenewal"
              @handle-hand-over="handleHandOver"
              @handle-remove="handleRemove"
              @get-select-list="getSelectList"
              @handle-select-all-data="handleSelectAllData"
              @handle-load-more="handleLoadMore"
              @handle-clear="handleClear"
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
import { ref, defineProps, defineEmits, computed, watch } from 'vue';
import userGroupTable from "@/store/userGroupTable";
import TabTable from './tab-table.vue';

const { t } = useI18n();
const activeFlag =  ref(true);
const groupTableStore = userGroupTable();
const projectTable = computed(() => props.sourceList.find(item => item.resourceType == 'project'));
const sourceTable= computed(() => props.sourceList.filter(item => item.resourceType != 'project'));


const {
  handleRenewal,
  handleHandOver,
  handleRemove,
  getSelectList,
  handleLoadMore,
  handleSelectAllData,
  handleClear,
  pageLimitChange,
  pageValueChange,
} = groupTableStore;

const props = defineProps({
  isShowOperation: {
    type: Boolean,
    default: true,
  },
  selectedData: {
    type: Object,
  },
  sourceList: {
    type: Array,
    default: () => [],
  },
  asideItem: {
    type: Object,
  },
});
const emit = defineEmits(['collapseClick']);

/**
 * 折叠面板点击事件
 * @param id 折叠面板唯一标志
 */
function collapseClick(resourceType) {
  emit('collapseClick', resourceType.name);
}
</script>

<style lang="less" scoped>
.group-tab {
  width: 100%;
  height: 100%;


  .manage-content-common {
    background: #FFFFFF;
    padding: 16px 24px;
    box-shadow: 0 2px 4px 0 #1919290d;
  }
  .manage-content-project {
    max-height: 630px;
    margin-bottom: 15px;
    .manage-content-common();
  }
   
  .manage-content-resource {
    .manage-content-common();
  }
  
  .project-group {
    font-family: MicrosoftYaHei-Bold;
    font-weight: 700;
    font-size: 14px;
    color: #63656E;
    margin-bottom: 16px;
    letter-spacing: 0;
    line-height: 22px;
  }
  
  .project-group-table {
    width: 100%;
    height: 100%;
    margin-bottom: 16px;
  
    .bk-table {
      border: 1px solid #dcdee5;
    }
  
    ::v-deep .bk-collapse-content {
      padding: 0 !important;
    }
  
    .group-title {
      width: 100%;
      height: 26px;
      line-height: 26px;
      padding-left: 10px;
      background: #EAEBF0;
      border-radius: 2px;
      font-family: MicrosoftYaHei;
      font-size: 12px;
      color: #313238;
      cursor: pointer;
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
    }
  
    .operation-btn {
      display: flex;
      justify-content: space-around;
    }
  }
}
</style>
