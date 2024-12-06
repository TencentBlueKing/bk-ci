<script setup>
import { useI18n } from 'vue-i18n';
const { t } = useI18n();

</script>

<template>
  <aside class="auth-aside">
    <div class="menu-item active">
      <i class="permission-icon permission-icon-oauth" />
      <span>{{ t('我的OAUTH') }}</span>
    </div>
  </aside>
</template>

<style lang="scss" scoped>
.auth-aside {
  width: 240px;
  color: #63656E;
  padding-top: 5px;
  background-color: #fff;
  box-shadow: 1px 0 0 0 #DCDEE5;
}
.menu-item {
  display: flex;
  align-items: center;
  width: 240px;
  height: 40px;
  font-size: 14px;
  padding-left: 25px;
  line-height: 40px;
  background: #E1ECFF;
  &.active {
    color: #3A84FF;
  }
  .permission-icon {
    margin-right: 10px;
  }
}
</style>