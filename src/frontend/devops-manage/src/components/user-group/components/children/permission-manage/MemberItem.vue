<!-- MemberItem.vue -->
<template>
  <div @click="handleClick(member)" class="label-item">
    <i :class="iconClass" />
    <p
      v-if="isUser"
      class="item"
      :style="{width: isBatchOperate ? '128px' : '150px'}"
    >
      <bk-overflow-title type="tips">
        {{ member.id }}
        <span v-if="member.name && !member.departed"> ({{ member.name }}) </span>
        <bk-tag v-else size="small" theme="danger"> {{ t("已离职")}}</bk-tag>
      </bk-overflow-title>
    </p>
    <p
      v-else
      class="item"
      v-bk-tooltips="{
        content: member.name,
        placement: 'top',
        disabled: !truncateMiddleText(member.name).includes(' ... ')
      }"
      :style="{width: isBatchOperate ? '128px' : '150px'}"
    >
      {{ truncateMiddleText(member.name) }}
    </p>
    <bk-popover
      v-if="!isBatchOperate"
      :arrow="false"
      placement="bottom"
      trigger="click"
      theme="light dot-menu"
    >
      <i @click.stop class="more-icon manage-icon manage-icon-more-fill"></i>
      <template #content>
        <div class="menu-content">
          <!-- <bk-button
            v-if="item.type === 'department'"
            class="btn"
            text
            @click="handleShowPerson(item)"
          >
            {{t("人员列表")}}
          </bk-button> -->
          <bk-button
            class="btn"
            text
            @click="handleRemoval(member)">
            {{t("移出项目")}}
          </bk-button>
        </div>
      </template>
    </bk-popover>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
import { defineProps, defineEmits, computed } from 'vue';

const props = defineProps({
  member: Object,
  activeTab: String,
  isBatchOperate: Boolean
});
const emit = defineEmits(['handleRemoval', 'handleClick']);

const { t } = useI18n();
const isUser = computed(() => props.member.type === 'user')
const iconClass = computed(() => ({
  'group-icon': true,
  'manage-icon manage-icon-organization': props.member.type === 'department',
  'manage-icon manage-icon-user-shape': isUser.value,
  'active': props.activeTab === props.member.id
}))

function truncateMiddleText(text) {
  if (text.length <= 15) {
    return text;
  }

  const separator = ' ... ';
  const charsToShow = 15 - separator.length;
  const frontChars = Math.ceil(charsToShow / 2);
  const backChars = Math.floor(charsToShow / 2);

  return text.substr(0, frontChars) + separator + text.substr(text.length - backChars);
}
function handleRemoval(params) {
  emit('handleRemoval', params)
}
function handleClick(params) {
  emit('handleClick', params)
}
</script>

<style lang="scss" scoped>
.label-item {
  display: flex;
  align-items: center;
  .group-icon {
    width: 15px;
    margin-right: 8px;
    color: #9ea0a4;
    &.active {
      color: #0b76ff;
    }
  }
  .item {
    flex: 1;
    height: 20px;
    font-size: 12px;
    color: #63656E;
    line-height: 20px;

    .bk-tag {
      line-height: 16px;
      display: inline-block !important;
    }
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
}


</style>
