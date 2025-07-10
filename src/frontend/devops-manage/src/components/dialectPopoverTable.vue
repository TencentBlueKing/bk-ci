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
        <bk-table-column :label="t('差异项')" prop="difference" :width="140">
          <template #default="{ row }">
            <div
              class="label-column"
            >
              <p>{{ row.difference }}</p>
              <p>{{ row.differenceTip }}</p>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column :label="t('CLASSIC')" prop="classic" :width="290">
          <template #default="{ row }">
            <div
              class="label-column"
            >
              <div v-if="!row.classicKey">
                <p>{{ row.classic }}</p>
                <p>{{ row.classicExample }}</p>
              </div>
              <div v-else class="classic-desc">
                  <i18n-t
                      tag="div"
                      keypath="仅在流程控制选项X设置中可以使用"
                  >
                      <span class="classic-key">{{ t('自定义表达式满足时运行') }}</span>
                  </i18n-t>
              </div>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column :label="t('CONSTRAINED')" prop="constrainedMode">
          <template #default="{ row }">
            <div
              class="label-column"
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
        difference: t('表达式函数'),
        differenceTip: t('如contains、join、fromJSON'),
        classic: t('仅在流程控制选项X设置中可以使用'),
        classicKey: true,
        constrainedMode: t('流程控制选项、插件入参、Job设置等流水线配置中均可使用函数'),
        constrainedExample: t('如变量a值为Json字符串，则bash脚本中，可以使用fromJSON读取echo “a.node is ${{fromJSON(a).node}}”')
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
    white-space: pre-wrap;
  }
}
.classic-desc {
  white-space: pre-wrap;
}
.classic-key {
  font-weight: 700;
}
</style>