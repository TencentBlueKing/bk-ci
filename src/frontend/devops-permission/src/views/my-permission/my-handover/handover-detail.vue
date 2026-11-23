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
            <template v-if="isManagerApply">
              <span class="font-bold">
                {{ t('项目管理员') }} {{ data?.applicant }} {{ t('申请') }}<i18n-t
                  keypath="将授权人改为X共X项"
                  tag="span"
                >
                  <span class="count">{{ granteeText }}</span><span class="count">{{ data?.authorizationCount }}</span>
                </i18n-t>
              </span>
            </template>
            <template v-else>
              <span class="font-bold">{{ t('X的申请：', [data?.applicant]) }}</span>
              <i18n-t
                :keypath="keyPath"
                tag="div"
              >
              <span v-if="data?.groupCount" class="count">{{ data?.groupCount }}</span><span v-if="data?.authorizationCount" class="count">{{ data?.authorizationCount }}</span>
              <!-- 不能换行,换行会导致国际化获取变量 获取不到第二个 -->
              </i18n-t>
            </template>
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
      <div
        v-if="consequenceTips.length"
        class="consequence-alert-wrapper"
      >
        <bk-alert :theme="props.isGiven ? 'info' : 'warning'">
          <template #title>
            <span class="alert-title">{{ t('同意后') }}</span>
          </template>
          <p
            v-for="(tip, index) in consequenceTips"
            :key="index"
            class="alert-line"
          >
            {{ tip }}
          </p>
        </bk-alert>
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
              <span
                v-if="showDecision && getSectionCount(viewTable) >= 2"
                class="batch-decision"
              >
                <bk-button
                  size="small"
                  @click="handleSectionBatchDecision('AGREE', viewTable.key)"
                >
                  {{ t('全部同意') }}
                </bk-button>
                <bk-button
                  size="small"
                  class="batch-btn"
                  @click="handleSectionBatchDecision('REJECT', viewTable.key)"
                >
                  {{ t('全部驳回') }}
                </bk-button>
              </span>
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
                    :show-decision="showDecision"
                    :show-outcome="showOutcome"
                    :authorizer-label="viewTable?.isAuthorizations ? authorizerLabel : ''"
                    :decisions="decisions"
                    :batch-decision="batchDecision"
                    @page-limit-change="detailPageLimitChange"
                    @page-value-change="detailPageValueChange"
                    @decision-change="handleDecisionChange"
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
      <template v-if="hasRejectDecision">
        <p class="field">
          {{ t('审批意见') }}
          <span class="required-star">*</span>
        </p>
        <bk-input
          ref="remarkRef"
          class="remark-input"
          v-model="remark"
          :placeholder="t('请输入审核意见，拒绝时必填')"
          type="textarea"
          :rows="3"
          :maxlength="100"
        />
      </template>
      <bk-button
        theme="primary"
        class="agree-btn"
        :disabled="!allDecided || (hasRejectDecision && !remark)"
        :loading="approvalLoading"
        v-bk-tooltips="{
          content: !allDecided ? t('请为每条资源选择同意或驳回') : t('驳回时，审批意见必填'),
          disabled: allDecided && (!hasRejectDecision || !!remark)
        }"
        @click="handleSubmitDecision"
      >
        {{ t('提交') }}
      </bk-button>
      </div>
    <div
      class="slider-footer revoke-wrapper"
      v-if="showRevoke"
    >
      <bk-button
        :loading="revokeLoading"
        @click="handleRevoke"
      >
        {{ t('撤回') }}
      </bk-button>
    </div>
  </section>
</template>

<script lang="ts" setup>
  import http from '@/http/api';
  import { useI18n } from 'vue-i18n';
  import { storeToRefs } from 'pinia';
  import { ref, computed, watch } from 'vue';
  import { Message, InfoBox } from 'bkui-vue'
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
  const remarkRef = ref<any>(null)
  const approvalLoading = ref(false)
  const revokeLoading = ref(false)
  // 按条决策：{ `${handoverType}:${resourceType}:${itemId}`: 'AGREE' | 'REJECT' }
  const decisions = ref<Record<string, string>>({});
  // 批量决策（全部同意/全部驳回）：{ `${handoverType}:${resourceType}`: 'AGREE' | 'REJECT' }
  const batchDecision = ref<Record<string, string>>({});
  // 本次会话内已提交过（提交后单据保持打开并展示结果）
  const isSubmitted = ref(false);
  const userId = computed(() => (window as any).top?.userInfo?.username);
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
  // 确认人处理中：所有明细类型（授权资源 + 用户组）逐条展示同意/驳回
  const showDecision = computed(() => showApproval.value && !isSubmitted.value);
  // 处理结束（含提交后保持打开）：展示每条结果，授权人列名恢复为「授权人」
  const showOutcome = computed(() => props.data?.handoverStatus !== 'PENDING' || isSubmitted.value);
  const authorizerLabel = computed(() => (showOutcome.value ? t('授权人') : t('当前授权人')));
  // 是否存在驳回（含全部驳回），有驳回才展示审批意见
  const hasRejectDecision = computed(() => Object.values(decisions.value).includes('REJECT')
    || Object.values(batchDecision.value).includes('REJECT'));
  // 全部明细（授权 + 用户组）是否都已选择：跨分页累计判断，批量值视为全选
  const allDecided = computed(() => {
    const sources = detailSourceList.value.filter(item => (item.count ?? 0) > 0);
    if (!sources.length) return false;
    return sources.every((source) => {
      const batchKey = `${source.type}:${source.resourceType}`;
      if (batchDecision.value[batchKey]) return true;
      const prefix = `${batchKey}:`;
      const explicitCount = Object.keys(decisions.value).filter(key => key.startsWith(prefix)).length;
      return explicitCount >= (source.count ?? 0);
    });
  })
  const isManagerApply = computed(() => props.data?.applicationType === 'MANAGER_APPLY')
  const granteeText = computed(() => {
    const name = (props.isGiven ? props.data?.approver : t('你')) || '';
    return /^[a-zA-Z0-9]/.test(name) ? ` ${name} ` : name;
  });
  const consequenceTips = computed(() => props.data?.consequenceTips || []);
  const showRevoke = computed(() => props.isGiven
    && props.data?.handoverStatus === 'PENDING'
    && props.data?.canRevoke)
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
      },
      'PARTIAL_SUCCEED': {
        theme: 'warning',
        text: t('部分通过')
      },
      'FAILED': {
        theme: 'danger',
        text: t('未通过')
      }
    }
    return statusMap[props.data?.handoverStatus]
  })
  const infoField = computed(() => {
    return [
      // 管理员单展示「确认人」且确认人自己查看时不重复展示；个人单保持「交接人」
      ...((!isManagerApply.value || props.isGiven) ? [{
        field: 'approver',
        label: isManagerApply.value ? t('确认人') : t('交接人'),
        value: props.data?.approver,
      }] : []),
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
      // 还没有审批意见时不展示该字段
      ...((props.data?.remark) ? [{
        field: 'remark',
        label: t('审核意见'),
        value: props.data?.remark
      }] : []),
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

  /**
   * 单条决策变更
   */
  const handleDecisionChange = (key: string, val: string) => {
    decisions.value[key] = val;
  }
  /**
   * 全部同意/全部驳回
   */
  const handleSectionBatchDecision = (val: string, sectionKey: string) => {
    const section = viewTableList.value.find(v => v.key === sectionKey);
    section?.data.forEach((source: any) => {
      if (!source.count) return;
      const batchKey = `${source.type}:${source.resourceType}`;
      batchDecision.value[batchKey] = val;
      // 清除该类型下的单条覆盖，保证批量值对所有行生效
      const prefix = `${batchKey}:`;
      Object.keys(decisions.value).forEach((key) => {
        if (key.startsWith(prefix)) {
          delete decisions.value[key];
        }
      });
    });
  }
  /**
   * 区块明细总数
   */
  const getSectionCount = (viewTable: any) => viewTable.data.reduce((sum: number, item: any) => sum + (item.count ?? 0), 0);
  /**
   * 竞态处理：单据已被撤回/已结束/已被处理，刷新详情与列表并保持打开
   */
  const handleRaceError = (e: any) => {
    if ([2121101, 2121102, 2121092].includes(e?.code)) {
      fetchDetailList(defaultFetchParams);
      emit('success', true)
    }
  }
  /**
   * 按条提交处理结果：单条决策优先，未单独设置的用批量决策补齐
   */
  const handleSubmitDecision = async () => {
    if (!allDecided.value) {
      Message({ theme: 'error', message: t('请为每条资源选择同意或驳回') });
      return;
    }
    if (hasRejectDecision.value && !remark.value) {
      Message({ theme: 'error', message: t('驳回时，审批意见必填') });
      return;
    }
    approvalLoading.value = true;
    try {
      const sources = detailSourceList.value.filter((source: any) => (source.count ?? 0) > 0);
      // 收集整单内所有有效决策值（批量 + 单条）：全部一致时可整单提交，无需拉取明细
      const decisionValues = new Set<string>();
      sources.forEach((source: any) => {
        const batchKey = `${source.type}:${source.resourceType}`;
        if (batchDecision.value[batchKey]) {
          decisionValues.add(batchDecision.value[batchKey]);
        } else {
          const prefix = `${batchKey}:`;
          Object.keys(decisions.value).forEach((key) => {
            if (key.startsWith(prefix)) decisionValues.add(decisions.value[key]);
          });
        }
      });
      const payload: any = {
        projectCode: props.data?.projectCode,
        flowNo: props.data?.flowNo,
        operator: props.data?.approver,
        remark: remark.value,
      };
      if (decisionValues.size === 1) {
        // 整单全部同意 / 全部驳回 ，不传 itemDecisions
        payload.handoverAction = Array.from(decisionValues)[0];
      } else {
        // 混合审批：并行拉取全部明细（含未加载分页），构造覆盖整单的 itemDecisions
        const itemDecisions: any[] = [];
        await Promise.all(sources.map(async (source: any) => {
          const isGroup = source.type === 'GROUP';
          let records: any[] = source.tableData;
          if (records.length < source.count) {
            const fetchFn = isGroup ? http.listGroupsOfHandover : http.listAuthorizationsOfHandover;
            const res = await fetchFn({
              ...defaultFetchParams,
              resourceType: source.resourceType,
              page: 1,
              pageSize: source.count,
            });
            records = res?.records || [];
          }
          records.forEach((row: any) => {
            // 用户组明细用 iamGroupId，授权明细用 resourceCode
            const rowId = isGroup ? String(row.iamGroupId) : row.resourceCode;
            const decision = decisions.value[`${source.type}:${source.resourceType}:${rowId}`]
              || batchDecision.value[`${source.type}:${source.resourceType}`];
            if (decision) {
              itemDecisions.push({
                itemId: rowId,
                resourceType: source.resourceType,
                handoverType: source.type,
                decision,
              });
            }
          });
        }));
        // 未覆盖全部明细（存在未选择的其他分页数据）
        const totalDetailCount = (props.data?.authorizationCount || 0) + (props.data?.groupCount || 0);
        if (itemDecisions.length < totalDetailCount) {
          Message({ theme: 'error', message: t('请为每条资源选择同意或驳回') });
          return;
        }
        payload.handoverAction = 'AGREE';
        payload.itemDecisions = itemDecisions;
      }
      const res = await http.handleHanoverApplication(payload)
      if (res) {
        Message({ theme: 'success', message: t('已提交处理结果') })
        isSubmitted.value = true;
        decisions.value = {};
        batchDecision.value = {};
        remark.value = '';
        fetchDetailList(defaultFetchParams);
        emit('success', true)
      }
    } catch (e: any) {
      console.log(e);
      handleRaceError(e);
      if (e?.code === 2121104) {
        remarkRef.value?.focus?.();
      }
    } finally {
      approvalLoading.value = false
    }
  }
  /**
   * 撤回申请（仅申请人可操作）
   */
  const handleRevoke = () => {
    InfoBox({
      type: 'warning',
      title: t('确定撤回此申请吗？'),
      content: t('撤回后授权人不变，对方无需再处理。'),
      confirmText: t('撤回'),
      cancelText: t('取消'),
      onConfirm: async () => {
        try {
          revokeLoading.value = true
          await http.handleHanoverApplication({
            projectCode: props.data?.projectCode,
            flowNo: props.data?.flowNo,
            operator: userId.value,
            handoverAction: 'REVOKE',
          })
          Message({
            theme: 'success',
            message: t('已撤回申请')
          })
          emit('success')
        } catch (e: any) {
          console.log(e);
          handleRaceError(e);
        } finally {
          revokeLoading.value = false
        }
      },
    })
  }
  watch(() => props.isShow, (val) => {
    if (val) {
      // 重新打开单据时重置决策状态
      isSubmitted.value = false;
      decisions.value = {};
      batchDecision.value = {};
      remark.value = '';
      fetchDetailList(defaultFetchParams);
    }
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
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-weight: 700;
        font-size: 14px;
        color: #63656E;
        margin-bottom: 16px;
        letter-spacing: 0;
        line-height: 22px;

        .batch-decision {
          font-weight: 400;

          .batch-btn {
            margin-left: 8px;
          }
        }

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
      min-height: 200px;
      background: #FFFFFF;
      padding: 20px 48px;
      .field {
        color: #4D4F56;
        font-weight: 700;
        margin-bottom: 6px;

        .required-star {
          color: #EA3636;
        }
      }
      .agree-btn {
        margin-top: 20px;
        margin-right: 10px;
      }
      .error-text {
        color: #ff5656;
      }
    }
    .revoke-wrapper {
      min-height: 88px;
      padding: 20px;
    }
    .consequence-alert-wrapper {
      margin-bottom: 24px;

      .alert-title {
        font-weight: 600;
        color: #313238;
      }
      .alert-line {
        margin: 0;
        font-size: 12px;
        color: #4D4F56;
        margin-top: 8px;
        line-height: 22px;
      }
    }
  }
</style>
