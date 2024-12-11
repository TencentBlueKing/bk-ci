<template>
  <bk-loading
    :loading="detailGroupTable.isLoading"
    :zIndex="100"
    class="group-tab"
  >
    <div class="manage-content-project" v-if="projectTable.length">
      <p class="project-group">
        <span>{{t("资源授权")}}</span>
        <i18n-t v-overflow-title class="describe" keypath="你是以下X条流水线权限代持人，直接退出将导致流水线运行失败，需先移交" tag="div">
          <span class="text-blue">{{ authorizationsLength }}</span>
        </i18n-t>
      </p>
      <div
        class="project-group-table"
        v-for="item in projectTable"
        :key="item.resourceType"
      >
        <bk-collapse-panel
          v-model="item.activeFlag"
          :item-click="(type) => collapseClick(type, 'AUTHORIZATION')"
          :name="item.resourceType"
        >
          <template #header>
            <p class="group-title">
              <i :class="{
                'manage-icon manage-icon-down-shape': item.activeFlag,
                'manage-icon manage-icon-right-shape': !item.activeFlag,
                'shape-icon': true,
              }" />
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
              :type="'AUTHORIZATION'"
              @page-limit-change="pageLimitChange"
              @page-value-change="pageValueChange"
            />
          </template>
        </bk-collapse-panel>
      </div>
    </div>
    <div class="manage-content-resource" v-if="sourceTable.length">
      <p class="project-group">
        <span>{{t("影响流水线代持权限及唯一拥有者用户组")}}</span>
        <!-- <i18n-t class="describe" keypath="你是以下X个用户组的唯一管理员，直接退出将导致对应资源无管理人，需先移交" tag="div">
          <span class="text-blue">{{ 3 }}</span>
        </i18n-t> -->
      </p>
      <div
        class="project-group-table"
        v-for="item in sourceTable"
        :key="item.resourceType"
      >
        <bk-collapse-panel
          v-model="item.activeFlag"
          :item-click="(type) => collapseClick(type, 'GROUP')"
          :name="item.resourceType"
        >
          <template #header>
            <p class="group-title">
              <i :class="{
                'manage-icon manage-icon-down-shape': item.activeFlag,
                'manage-icon manage-icon-right-shape': !item.activeFlag,
                'shape-icon': true,
              }" />
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
              :type="'GROUP'"
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
import userDetailGroupTable from '@/store/userDetailGroupTable';
import TabTable from './detail-tab-table.vue';
import pipelineIcon from '@/css/svg/color-logo-pipeline.svg';
import codelibIcon from '@/css/svg/color-logo-codelib.svg';
import codeccIcon from '@/css/svg/color-logo-codecc.svg';
import environmentIcon from '@/css/svg/color-logo-environment.svg';
import experienceIcon from '@/css/svg/color-logo-experience.svg';
import qualityIcon from '@/css/svg/color-logo-quality.svg';
import ticketIcon from '@/css/svg/color-logo-ticket.svg';
import turboIcon from '@/css/svg/color-logo-turbo.svg';  // 编译加速

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
const projectTable = computed(() => props.sourceList.filter(item => item.type === "AUTHORIZATION"));
const sourceTable= computed(() => props.sourceList.filter(item => item.type === "GROUP"));

const authorizationsLength = computed(() => {
  return projectTable.value.reduce((sum, current) => {
    return sum + current.count
  }, 0)
})

const emit = defineEmits(['collapseClick']);

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
    font-family: MicrosoftYaHei-Bold;
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
