<template>
  <bk-popover
    theme="light"
    :width="900"
    :placement="placement"
    extCls="dialect-popover"
  >
    <label class="label">{{ t('变量语法风格') }}</label>
    <template #content>
      <h3 class="title">{{ t('语法差异') }}</h3>
      <bk-table :data="namingConventionData" show-overflow-tooltip>
        <bk-table-column :label="$t('差异项')" prop="difference" :width="140" />
        <bk-table-column :label="$t('传统风格')" prop="classIc" :width="290">
          <template #default="{ row }">
            <div
              class="label-column"
              v-bk-tooltips="{
                content: row.classIcExample ? `${row.classIc};  ${row.classIcExample}` : row.classIc
              }"
            >
              <p>{{ row.classIc }}</p>
              <p>{{ row.classIcExample }}</p>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column :label="$t('制约风格')" prop="constrained">
          <template #default="{ row }">
            <div
              class="label-column"
              v-bk-tooltips="{
                content: row.constrainedExample ? `${row.constrained};  ${row.constrainedExample}` : row.constrained
              }"
            >
              <p>{{ row.constrained }}</p>
              <p>{{ row.constrainedExample }}</p>
            </div>
          </template>
        </bk-table-column>
      </bk-table>
    </template>
  </bk-popover>
</template>

<script setup name="DialectPopoverTable">
import { useI18n } from 'vue-i18n';
const { t } = useI18n();
defineProps({
  placement: {
    type: String,
    default: 'top-start'
  }
})
const namingConventionData = [
    {
        difference: t('表达式格式'),
        classIc: t('单花括号或双花括号'),
        classIcExample: t('如：${var}、${{var}}'),
        constrained: t('仅支持双花括号，避免出现 bash 脚本变量在执行前被系统赋值的问题'),
        constrainedExample: t('如：${{variables.var}}'),
    },
    {
        difference: t('引用方式'),
        classIc: t('直接引用变量名或上下文方式引用'),
        constrained: t('仅支持上下文方式引用'),
        constrainedExample: t('如：${{ci.pipeline_id}}、${{variables.a}}、${{steps.get_code.outputs.a}}'),
    },
    {
        difference: t('环境变量'),
        classIc: t('流水线变量、插件输出变量会自动设置为环境变量'),
        constrained: t('需要时通过在 Job/Step 上按需指定，降低环境污染风险'),
    },
    {
        difference: t('重置内置只读变量'),
        classIc: t('不生效，仅警告未报错'),
        constrained: t('将报错，运行失败'),
    },
    {
        difference: t('变量值超长'),
        classIc: t('仅警告未报错'),
        constrained: t('将报错，运行失败'),
    },
    {
        difference: t('变量不存在'),
        classIc: t('未报错，继续执行，依赖执行逻辑自行检查'),
        constrained: t('将报错，运行失败'),
    },
    {
        difference: t('变量 ID 规范'),
        classIc: t('未限制'),
        constrained: t('不支持中文 ID，减少不同构建环境下的兼容问题'),
    }
]
</script>

<style lang="scss" scoped>
.dialect-popover {
  padding: 16px !important;
}  
.label {
  font-size: 12px;
  padding: 4px 0;
  border-bottom: 1px dashed #979BA5;
}
.title {
  margin-bottom: 16px;
  font-weight: 700;
  font-size: 14px;
  color: #63656E;
}
.label-column {
  padding: 4px 0;
  p {
    line-height: 20px;
    width: 100%;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }
}
  
</style>