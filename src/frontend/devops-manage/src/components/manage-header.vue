<script setup lang="ts">
import { AngleRight } from 'bkui-vue/lib/icon';
import { useI18n } from 'vue-i18n';
const { t } = useI18n();

defineProps<{
  name: string
}>();

const goToManage = () => {
  window.parent.location.href = `${location.origin}/console/pm`
};
</script>

<template>
  <section class="header-wrapper">
    <div class="manage-header">
      <bk-breadcrumb
        class="manage-breadcrumb"
        separator="/"
      >
        <bk-breadcrumb-item
          to="#"
          @click.native.stop="goToManage"
        >
          {{ t('项目管理') }}
          <template #separator>
            <angle-right class="manage-icon" />
          </template>
        </bk-breadcrumb-item>
        <bk-breadcrumb-item>
          {{ name }}
        </bk-breadcrumb-item>
      </bk-breadcrumb>
      <slot></slot>
    </div>
  </section>
</template>

<style lang="postcss" scoped>
.header-wrapper {
  display: flex;
  flex-direction: column;
}
.manage-header {
  height: 60px;
  background: #FFFFFF;
  box-shadow: 0 2px 5px 0 rgba(51,60,72,0.03);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 25px;
  z-index: 100;
}
.manage-breadcrumb {
  position: absolute;
}
.manage-icon {
  font-size: 18px;
  margin-right: 1px;
}
.bk-breadcrumb-item {
  :deep(.bk-breadcrumb-item-inner) {
    color: #313238;
  }
  :deep(.is-link) {
    color: #3A84FF;
  }
}
</style>
