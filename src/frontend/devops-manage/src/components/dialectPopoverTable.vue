<template>
  <bk-popover
    theme="light"
    :width="900"
    extCls="dialect-popover"
    :componentEventDelay="300"
    autoPlacement
  >
    <label class="label">{{ t('变量语法风格') }}</label>
    <template #content>
      <h3 class="title">{{ t('语法差异') }}</h3>
      <bk-table :data="namingConventionData" show-overflow-tooltip>
        <bk-table-column :label="t('差异项')" prop="difference" :width="140" />
        <bk-table-column :label="t('传统风格')" prop="classic" :width="290">
          <template #default="{ row }">
            <div
              class="label-column"
              v-bk-tooltips="{
                content: row.classicExample ? `${row.classic};  ${row.classicExample}` : row.classic
              }"
            >
              <p>{{ row.classic }}</p>
              <p>{{ row.classicExample }}</p>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column :label="t('制约风格')" prop="constrainedMode">
          <template #default="{ row }">
            <div
              class="label-column"
              v-bk-tooltips="{
                content: row.constrainedExample ? `${row.constrainedMode};  ${row.constrainedExample}` : row.constrainedMode
              }"
            >
              <p>{{ row.constrainedMode }}</p>
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
const namingConventionData = [
    {
        difference: t('表达式格式'),
        classic: t('单花括号或双花括号'),
        classicExample: t('如：${var}、${{var}}'),
        constrainedMode: t('仅支持双花括号，避免出现 bash 脚本变量在执行前被系统赋值的问题'),
        constrainedExample: t('如：${{variables.var}}'),
    },
    {
        difference: t('变量值超长'),
        classic: t('仅警告未报错'),
        constrainedMode: t('将报错，运行失败'),
    },
    {
        difference: t('变量不存在'),
        classic: t('未报错，继续执行，依赖执行逻辑自行检查'),
        constrainedMode: t('将报错，运行失败'),
    },
    {
        difference: t('变量 ID 规范'),
        classic: t('未限制'),
        constrainedMode: t('不支持中文 ID，减少不同构建环境下的兼容问题'),
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