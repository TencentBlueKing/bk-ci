<template>
  <bk-loading :loading="loading" :zIndex="100">
    <bk-table
      class="detail-table"
      ref="refTable"
      :max-height="!isShowOperation && 464"
      :data="data"
      show-overflow-tooltip
      :pagination="pagination"
      :border="border"
      remote-pagination
      empty-cell-text="--"
      @page-limit-change="pageLimitChange"
      @page-value-change="pageValueChange"
    >
      <bk-table-column :label="groupName" prop="resourceName" />
      <template v-if="!isAuthorizations">
        <bk-table-column :label="t('用户组')" prop="groupName" />
      </template>
      <bk-table-column v-else :label="authorizerLabel || t('授权人')">
        <template v-slot="{ row }">
          {{ row.currentHandoverFrom || row.handoverFrom || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="showOutcome"
        :label="t('结果')"
        width="220"
      >
        <template v-slot="{ row }">
          <span :class="['outcome-tag', `is-${normalizeOutcome(row.detailStatus)}`]">
            {{ outcomeText(row.detailStatus) }}
          </span>
          <p
            v-if="normalizeOutcome(row.detailStatus) === 'failed' && row.failedMessage"
            class="outcome-reason"
          >
            {{ row.failedMessage }}
          </p>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="showDecision"
        :label="t('处理')"
        width="200"
      >
        <template v-slot="{ row }">
          <bk-radio-group
            :model-value="getDecision(row)"
            @change="(val) => emit('decision-change', getRowKey(row), val)"
          >
            <bk-radio label="AGREE">{{ t('同意') }}</bk-radio>
            <bk-radio label="REJECT">{{ t('驳回') }}</bk-radio>
          </bk-radio-group>
        </template>
      </bk-table-column>
    </bk-table>
  </bk-loading>
</template>

<script setup name="TabTable">
import { useI18n } from 'vue-i18n';
import { ref, defineProps, defineEmits } from 'vue';

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  pagination: Object,
  resourceType: String,
  resourceName: String,
  loading: Boolean,
  groupName: String,
  isAuthorizations: {
    type: Boolean,
    default: false
  },
  type: String,
  // 确认人处理中：每行展示「同意/驳回」
  showDecision: {
    type: Boolean,
    default: false,
  },
  // 处理结束：展示每条结果（已生效/已驳回/未生效）
  showOutcome: {
    type: Boolean,
    default: false,
  },
  // 授权人列名：待处理为「当前授权人」，处理结束为「授权人」
  authorizerLabel: {
    type: String,
    default: '',
  },
  // 按条决策 { `${handoverType}:${resourceType}:${itemId}`: 'AGREE' | 'REJECT' }
  decisions: {
    type: Object,
    default: () => ({}),
  },
  // 批量决策 { `${handoverType}:${resourceType}`: 'AGREE' | 'REJECT' }（全部同意/全部驳回设置的值）
  batchDecision: {
    type: Object,
    default: () => ({}),
  },
});
const emit = defineEmits([
  'pageLimitChange',
  'pageValueChange',
  'decision-change',
]);
const { t } = useI18n();
const refTable = ref(null);
const border = ['row', 'outer'];

function getRowId (row) {
  // 用户组明细用 iamGroupId，授权明细用 resourceCode
  return props.type === 'GROUP' ? String(row.iamGroupId) : row.resourceCode;
}
function getRowKey (row) {
  return `${props.type}:${props.resourceType}:${getRowId(row)}`;
}
function getDecision (row) {
  return props.decisions[getRowKey(row)]
    || props.batchDecision[`${props.type}:${props.resourceType}`]
    || '';
}
/**
 * 明细结果状态归一化（兼容后端返回的不同取值）
 */
const outcomeMap = {
  SUCCEED: 'succeed',
  applied: 'succeed',
  EFFECTED: 'succeed',
  SUCCESS: 'succeed',
  REJECT: 'reject',
  rejected: 'reject',
  FAILED: 'failed',
  failed: 'failed',
  INVALID: 'failed',
  UN_EFFECTIVE: 'failed',
  NOT_EFFECTIVE: 'failed',
};
function normalizeOutcome (status) {
  return outcomeMap[status] || 'pending';
}
function outcomeText (status) {
  const textMap = {
    succeed: t('已生效'),
    reject: t('已驳回'),
    failed: t('未生效'),
    pending: t('待处理'),
  };
  return textMap[normalizeOutcome(status)];
}

function pageLimitChange(limit) {
  emit('pageLimitChange',limit, props.resourceType, props.type);
}
function pageValueChange(value) {
  emit('pageValueChange',value, props.resourceType, props.type);
}
</script>

<style lang="less" scoped>
.detail-table {
  margin-top: 4px;
  border: 1px solid #DCDEE5;

  .hover-link {
    cursor: pointer;
    &:hover {
      color: #3a84ff;
    }
  }
  .outcome-tag {
    display: inline-block;
    padding: 0 8px;
    line-height: 22px;
    border-radius: 11px;
    font-size: 12px;

    &.is-succeed {
      color: #2DCB56;
      background: #E5F6EA;
    }
    &.is-reject {
      color: #EA3636;
      background: #FEE9E9;
    }
    &.is-failed {
      color: #FF9C01;
      background: #FFF3E1;
    }
    &.is-pending {
      color: #63656E;
      background: #F0F1F5;
    }
  }
  .outcome-reason {
    margin: 4px 0 0 0;
    font-size: 12px;
    color: #979BA5;
    line-height: 18px;
  }
}
</style>
