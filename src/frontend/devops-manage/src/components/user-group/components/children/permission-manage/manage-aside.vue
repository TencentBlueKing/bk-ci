<template>
  <div>
    <div class="group-item">
      <img src="../../../svg/organization.svg?inline" class="group-icon">
      <p>{{ memberList[0].bgName }}</p>
    </div>
    <div
      :class="{'group-active': activeTab == item.expiredId }"
      class="group-item item-hover"
      v-for="item in memberList[0].subjectScopes"
      :key="item.expiredId"
      @click="handleClick(item.expiredId)"
    >
      <img v-if="activeTab != item.expiredId" src="../../../svg/user.svg?inline" class="group-icon">
      <img v-else src="../../../svg/user-active.svg?inline" class="group-icon" />
      <p>{{ item.full_name }}({{ item.name }})</p>
      <!-- v-if="activeTab == item.expiredId" -->
      <bk-popover
        transfer
        offset="15"
        :distance="0"
        :arrow="false"
        trigger="click"
        placement="bottom"
        theme="light dot-menu"
        :popover-delay="[100, 0]"
      >
        <i @click.stop class="more-icon manage-icon manage-icon-more-fill"></i>
        <template #content>
          <div class="menu-content">
            <bk-button
              class="btn"
              text
              @click="handleRemoval(item)">
              {{ t('移出项目') }}
            </bk-button>
          </div>
        </template>
      </bk-popover>
    </div>

    <bk-dialog
      :is-show="isShowDialog"
      :title="'这是标题'"
      :theme="'primary'"
      :size="'normal'"
      @closed="() => isShowDialog = false"
      @confirm="() => isShowDialog = false"
    >
      <div>normal</div>
    </bk-dialog>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
import { ref, defineProps, defineEmits } from 'vue';

const { t } = useI18n();
const activeTab = ref('1743602525');
const isShowDialog = ref(false);

defineProps({
  memberList: {
    type: Array,
    required: true,
  }
});

const emit = defineEmits(['handleChange']);
function handleClick(id) {
  activeTab.value = id;
  emit('handleClick', id);
}

function handleRemoval(item) {
  isShowDialog.value = true;
}

</script>

<style lang="scss" scoped>
.group-item {
  display: flex;
  width: 100%;
  padding: 0 18px;
  height: 40px;
  align-items: center;
  font-size: 14px;
  color: #63656E;
  cursor: pointer;

  p{
    flex: 1;
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: #63656E;
    letter-spacing: 0;
    line-height: 20px;
  }

  .group-icon {
    width: 15px;
    line-height: 20px;
    margin-right: 8px;
    filter: invert(70%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
  }

  .more-icon {
    border-radius: 50%;
    color: #63656e;
    padding: 1px;
    display: none;
  }

  .more-icon:hover {
    background-color: #DCDEE5;
    color: #3A84FF !important;
  }

  &:hover .more-icon{
    display: block;
  }

}

.item-hover:hover {
  background-color: #eaebf0;
}

.group-active {
  background-color: #E1ECFF !important;
  border-right: 1px solid #3A84FF;

  p{
    color: #3A84FF;
  }

  .group-icon {
    filter: invert(100%) sepia(0%) saturate(90%) hue-rotate(180deg) brightness(90%) contrast(180%);
  }
}
</style>
