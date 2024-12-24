<template>
  <section class="detail-wrapper">
    <div
      :class="{
        'slider-content': true,
        'is-approval': showApproval
      }"
    >
      <div class="header-wrapper">
        <div class="title">
            <span class="font-bold">{{ t('X的申请：', [data.applicant]) }}</span>
            <i18n-t
              :keypath="keyPath"
              tag="div"
            >
            <span v-if="data.groupCount" class="count">{{ data.groupCount }}</span><span v-if="data.authorizationCount" class="count">{{ data.authorizationCount }}</span>
            <!-- 不能换行,换行会导致国际化获取变量 获取不到第二个 -->
            </i18n-t>
            <bk-tag
              class="status-tag"
              type="stroke"
              :theme="getStatus.theme"
            > {{ getStatus.text }} </bk-tag>
        </div>
        <div class="info">
          <div
            v-for="item in infoField"
            :key="item.field"
            class="info-item"
          >
            <p class="label">{{ item.label }}</p>
            <p class="value">
              <bk-overflow-title type="tips">{{ item.value }}</bk-overflow-title>
            </p>
          </div>
        </div>
      </div>
      <bk-loading :loading="isLoading" :zIndex="100" class="content-wrapper">
        <template v-for="viewTable in viewTableList">
          <div
            v-if="viewTable.data.length"
            class="manage-content-project"
            :key="viewTable.key" 
          >
            <p class="project-group">
              <span>{{ viewTable.title }}</span>
            </p>
            <div class="project-group-table" v-for="item in viewTable.data" :key="item.resourceType">
              <bk-collapse-panel 
                v-model="item.activeFlag"
                :item-click="({ name }) => detailCollapseClick(name, viewTable.type)"
                :name="item.resourceType"
              >
                <template #header>
                  <p class="group-title">
                    <i :class="{
                      'permission-icon permission-icon-down-shape': item.activeFlag, 
                      'permission-icon permission-icon-right-shape': !item.activeFlag,
                      'shape-icon': true,
                    }" />
                    <img
                      v-if="item.resourceType && getServiceIcon(item.resourceType)"
                      :src="getServiceIcon(item.resourceType)"
                      class="service-icon" alt=""
                    >
                    {{item.resourceTypeName}} ({{ item.resourceType }})
                    <span class="group-num">
                      {{item.count}}
                    </span>
                  </p>
                </template>
                <template #content>
                  <TabTable
                    v-if="item.tableData"
                    :data="item.tableData"
                    :isAuthorizations="viewTable?.isAuthorizations"
                    :pagination="item.pagination"
                    :resource-type="item.resourceType"
                    :resource-name="item.resourceTypeName"
                    :loading="item.tableLoading"
                    :group-name="item.resourceTypeName"
                    :type="viewTable.type"
                    @page-limit-change="detailPageLimitChange"
                    @page-value-change="detailPageValueChange"
                  />
                </template>
              </bk-collapse-panel>
            </div>
          </div>
        </template>
        
      </bk-loading>
    </div>
    <div
        class="slider-footer approval-wrapper"
        v-if="showApproval"
      >
        <p class="field">{{ t('备注') }}</p>
        <bk-input
          class="remark-input"
          v-model="remark"
          type="textarea"
          :rows="3"
          :maxlength="100"
        />
        <bk-button
          theme="primary"
          class="agree-btn"
          :loading="approvalLoading"
          @click="handleApproval('AGREE')"
        >
          {{ t('通过') }}
        </bk-button>
        <bk-button
          :loading="approvalLoading"
          @click="handleApproval('REJECT')"
        >
          {{ t('拒绝') }}
        </bk-button>
      </div>
  </section>
</template>

<script lang="ts" setup>
  import http from '@/http/api';
  import { useI18n } from 'vue-i18n';
  import { storeToRefs } from 'pinia';
  import { ref, computed, watch } from 'vue';
  import { Message } from 'bkui-vue'
  import userDetailGroupTable from "@/store/userDetailGroupTable";
  import TabTable from '@/components/permission-manage/detail-tab-table.vue';

  const { t } = useI18n();
  const emit = defineEmits(['success']);
  const props = defineProps({
    data: Object,
    isGiven: Boolean,
    isShow: Boolean,
  });
  const remark = ref('')
  const approvalLoading = ref(false)
  const defaultFetchParams = {
    projectCode: props.data?.projectCode,
    queryChannel: 'HANDOVER_APPLICATION',
    flowNo: props.data?.flowNo
  }
  const detailGroupTable = userDetailGroupTable();
  const {
    isLoading,
    detailSourceList,
  } = storeToRefs(detailGroupTable);
  const {
    fetchDetailList,
    getServiceIcon,
    detailCollapseClick,
    detailPageLimitChange,
    detailPageValueChange
  } = detailGroupTable;
  const showApproval = computed(() => !props.isGiven && props.data?.handoverStatus === 'PENDING')
  const authTable = computed(() => detailSourceList.value.filter(item => item.type === 'AUTHORIZATION'));
  const projectTable = computed(() => detailSourceList.value.filter(item => item.type === 'GROUP' && item.resourceType === 'project')); 
  const sourceTable = computed(() => detailSourceList.value.filter(item => item.type === 'GROUP' && item.resourceType !== 'project')); 
  const viewTableList = computed(() => {
    return [
      {
        type: 'GROUP',
        key: 'project',
        data: projectTable.value,
        title: t('项目级用户组')
      },
      {
        type: 'GROUP',
        key: 'source',
        data: sourceTable.value,
        title: t('资源级用户组')
      },
      {
        type: 'AUTHORIZATION',
        key: 'auth',
        data: authTable.value,
        title: t('授权资源'),
        isAuthorizations: true
      }
    ]
  })
  const getStatus = computed(() => {
    const statusMap = {
      'SUCCEED': {
        theme: 'success',
        text: t('已通过')
      },
      'REJECT': {
        theme: 'danger',
        text: t('已拒绝')
      },
      'PENDING': {
        theme: 'info',
        text: t('待处理')
      },
      'REVOKE': {
        theme: '',
        text: t('已撤销')
      }
    }
    return statusMap[props.data?.handoverStatus]
  })
  const infoField = computed(() => {
    return [
      {
        field: 'approver',
        label: t('交接人'),
        value: props.data?.approver,
      },
      {
        field: 'projectName',
        label: t('项目'),
        value: props.data?.projectName
      },
      {
        field: 'createTime',
        label: t('申请时间'),
        value: props.data?.createTime
        
      },
      {
        field: 'remark',
        label: t('备注'),
        value: props.data?.remark || '--'
      },
    ]
  }) 
  const keyPath = computed(() => {
    if (props.data?.groupCount && props.data?.authorizationCount) {
      return '移交X个用户组权限，同时移交X个授权资源'
    } else if (props.data?.groupCount) {
      return '移交X个用户组权限'
    } else if (props.data?.authorizationCount) {
      return '移交X个授权资源'
    }
    return ''
  })

  const handleApproval = async (val: string) => {
    try {
      approvalLoading.value = true
      const res = await http.handleHanoverApplication({
        projectCode: props.data?.projectCode,
        flowNo: props.data?.flowNo,
        operator: props.data?.approver,
        handoverAction: val,
        remark: remark.value
      })
      if (res) {
        const message = val === 'AGREE' ? t('移交权限已通过') : t('移交权限已拒绝')
        Message({
          theme: 'success',
          message
        })
        emit('success')
      }
    } catch (e: any) {
      Message({
        theme: 'error',
        message: e.message || e
      })
    } finally {
      approvalLoading.value = false

    }
  }
  watch(() => props.isShow, (val) => {
    if (val) fetchDetailList(defaultFetchParams);
  }, {
    immediate: true,
    deep: true
  })
</script>

<style lang="scss" scoped>
  .detail-wrapper {
    .slider-content {
      padding: 20px;
      overflow: auto;
      &.is-approval {
        height: calc(100vh - 240px);
      }
      &::-webkit-scrollbar-thumb {
        background-color: #c4c6cc !important;
        border-radius: 5px !important;
        &:hover {
          background-color: #979ba5 !important;
        }
      }

      &::-webkit-scrollbar {
        width: 8px !important;
        height: 8px !important;
      }
    }
    .header-wrapper {
      width: 100%;
      border-radius: 2px;
      border: 1px solid #699DF4;
      margin-bottom: 24px;
      .title {
        display: flex;
        align-items: center;
        position: relative;
        height: 40px;
        line-height: 40px;
        color: #4D4F56;
        padding: 0 20px;
        background: #F0F5FF;
      }
      .font-bold {
        font-weight: 700;
        font-size: 14px;
      }
      .count {
        color: #699DF4;
      }
      .status-tag {
        position: absolute;
        right: 20px;
      }
      .info {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 80px;
        padding: 12px 20px;
        background: #FFFFFF;
      }
      .info-item {
        max-width: 200px;
        margin-right: 20px;
        .label {
          color: #979BA5;
        }
        .value {
          display: block;
          width: 200px;
          color: #313238;
          margin-top: 10px;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
    .content-wrapper {
      width: calc(100% - 8px);
      overflow-y: auto;

      &::-webkit-scrollbar-thumb {
        background-color: #c4c6cc !important;
        border-radius: 5px !important;
        &:hover {
          background-color: #979ba5 !important;
        }
      }
      &::-webkit-scrollbar {
        width: 8px !important;
        height: 8px !important;
      }

      .manage-content-project,
      .manage-content-resource {
        background: #FFFFFF;
        padding: 16px 24px;
        box-shadow: 0 2px 4px 0 #1919290d;
      }
      
      .manage-content-project {
        margin-bottom: 15px;
      }
      
      .project-group {
        font-weight: 700;
        font-size: 14px;
        color: #63656E;
        margin-bottom: 16px;
        letter-spacing: 0;
        line-height: 22px;

        .describe {
          display: inline-block;
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
    .approval-wrapper {
      position: fixed;
      bottom: 0;
      width: 960px;
      height: 200px;
      background: #FFFFFF;
      padding: 24px 48px;
      .field {
        color: #4D4F56;
        font-weight: 700;
        margin-bottom: 6px;
      }
      .remark-input {
        margin-bottom: 20px;
      }
      .agree-btn {
        margin-right: 10px;
      }
    }
  }
</style>
