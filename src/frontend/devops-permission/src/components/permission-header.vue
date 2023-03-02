<script setup lang="ts">
import { AngleRight } from 'bkui-vue/lib/icon';
import { useI18n } from 'vue-i18n';

defineProps<{
  navs: { name: string, url: string }[]
}>();

const { t } = useI18n();
const goToUrl = (url) => {
  if (url) {
    location = url;
  }
};

</script>

<template>
  <section class="header-wrapper">
    <div class="permission-header">
      <bk-breadcrumb
        class="permission-breadcrumb"
        separator="/"
      >
        <bk-breadcrumb-item
          v-for="(nav, index) in navs"
          :key="index"
          :class="{
            'bk-breadcrumb-item-inner': nav.url
          }"
          @click.native.stop="goToUrl(nav.url)"
        >
          <span
            :class="{
              'nav-item': index === 0 && navs.length > 1,
            }"
          >{{ nav.name }}</span>
          <template
            #separator
            v-if="index < navs.length - 1"
          >
            <angle-right class="permission-icon" />
          </template>
        </bk-breadcrumb-item>
      </bk-breadcrumb>
      <!-- <bk-button text theme="primary">{{ t('返回旧版') }}</bk-button> -->
    </div>
  </section>
</template>

<style lang="postcss" scoped>
.header-wrapper {
  display: flex;
  flex-direction: column;
  z-index: 1000;
}
.permission-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 25px;
  background: #FFFFFF;
  box-shadow: 0 2px 5px 0 #333c48;
}
.permission-icon {
  font-size: 18px;
  margin-right: 1px;
}
.bk-breadcrumb-item {
  :deep(.nav-item) {
    cursor: pointer;
    color: #3A84FF;
  }
  :deep(.bk-breadcrumb-item-inner) {
    color: #313238;
  }
  :deep(.is-link) {
    color: #3A84FF;
  }
}
</style>
