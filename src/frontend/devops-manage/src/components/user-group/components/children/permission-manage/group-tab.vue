<template>
  <bk-loading :loading="groupTableStore.isLoading" class="group-tab">
    <div class="manage-content-project" v-if="projectTable">
      <p class="project-group">项目级用户组</p>
      <div class="project-group-table">
        <bk-collapse-panel v-model="activeFlag">
          <template #header>
            <p class="group-title">
              <i class="permission-icon permission-icon-down-shape"></i>
              {{ projectTable.groupItem }}
              <span class="group-num">{{projectTable.groupTotal}}</span>
            </p>
          </template>
          <template #content>
            <TabTable
              :is-show-operation="isShowOperation"
              :data="projectTable.tableData"
              :group-id="projectTable.id"
              :group-total="projectTable.groupTotal"
              :pagination="pagination"
              :selected-data="groupTableStore.selectedData"
              @handle-renewal="groupTableStore.handleRenewal"
              @handle-hand-over="groupTableStore.handleHandOver"
              @handle-remove="groupTableStore.handleRemove"
              @get-select-list="groupTableStore.getSelectList"
              @handle-select-all-data="groupTableStore.handleSelectAllData"
              @handle-load-more="groupTableStore.handleLoadMore"
              @handle-clear="groupTableStore.handleClear"
            />
          </template>
        </bk-collapse-panel>
      </div>
    </div>
    <div class="manage-content-resource" v-if="sourceTable.length">
      <p class="project-group">资源级用户组</p>
      <div class="project-group-table" v-for="item in sourceTable" :key="item.id">
        <bk-collapse-panel v-model="item.activeFlag" :item-click="collapseClick" :name="item.groupItem">
          <template #header>
            <p class="group-title">
              <i class="permission-icon permission-icon-down-shape"></i>
              {{item.groupItem}}
              <span class="group-num">{{item.groupTotal}}</span>
            </p>
          </template>
          <template #content>
            <TabTable
              :is-show-operation="isShowOperation"
              :data="item.tableData"
              :group-id="item.id"
              :group-total="item.groupTotal"
              :pagination="pagination"
              :selected-data="groupTableStore.selectedData"
              @handle-renewal="groupTableStore.handleRenewal"
              @handle-hand-over="groupTableStore.handleHandOver"
              @handle-remove="groupTableStore.handleRemove"
              @get-select-list="groupTableStore.getSelectList"
              @handle-select-all-data="groupTableStore.handleSelectAllData"
              @handle-load-more="groupTableStore.handleLoadMore"
              @handle-clear="groupTableStore.handleClear"
            />
          </template>
        </bk-collapse-panel>
      </div>
    </div>
  </bk-loading>
</template>

<script setup name="GroupTab">
import { ref, defineProps, defineEmits, computed } from 'vue';
import userGroupTable from "@/store/userGroupTable";
import TabTable from './tab-table.vue';

const activeFlag =  ref(true);
const groupTableStore = userGroupTable();
const projectTable = computed(() => props.sourceList[0]);
const sourceTable= computed(() => props.sourceList.slice(1));

const props = defineProps({
  isShowOperation: {
    type: Boolean,
    default: true,
  },
  pagination: {
    type: Object,
  },

  sourceList: {
    type: Array,
    default: () => [],
  },
});
const emit = defineEmits(['collapseClick']);

/**
 * 折叠面板点击事件
 * @param id 折叠面板唯一标志
 */
function collapseClick(id) {
  emit('collapseClick', id);
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
      padding: 5px 0 !important;
    }
  
    .group-title {
      width: 100%;
      height: 26px;
      line-height: 26px;
      margin-top: 16px;
      padding-left: 10px;
      margin-bottom: 4px;
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
