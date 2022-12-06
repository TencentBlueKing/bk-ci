<template>
  <article class="group-aside">
    <section class="group-list">
      <div
        :class="{ 'group-item': true, 'group-active': activeTab === group.name }"
        v-for="(group, index) in groupList"
        :key="index"
        @click="handleChangeTab(group)">
        <span class="name">{{ group.name }}</span>
        <span class="user-num">
          <i class="manage-icon manage-icon-user-shape"></i>
          {{ group.user }}
        </span>
        <span class="group-num">
          <i class="manage-icon manage-icon-user-shape"></i>
          {{ group.group }}
        </span>
        <bk-popover
          class="group-more-option"
          placement="bottom"
          trigger="click"
          theme="dot-menu light"
          :arrow="false"
          offset="15"
          :distance="0">
          <i class="more-icon manage-icon manage-icon-more-fill"></i>
          <template #content>
            <bk-button
              class="btn"
              :disabled="[1, 2].includes(group.id)"
              text
              @click="handleDeleteGroup(group)">
              {{ t('删除') }}
            </bk-button>
          </template>
        </bk-popover>
      </div>
      <div class="line-split" />
      <div
        :class="{ 'group-item': true, 'group-active': activeTab === '' }"
        @click="handleCreateGroup">
        <span class="add-group-btn">
          <i class="manage-icon manage-icon-add-fill add-icon"></i>
          {{ t('新建用户组') }}
        </span>
      </div>
    </section>
    <div class="close-btn">
      <bk-button @click="handleCloseManage">{{ t('关闭权限管理') }}</bk-button>
    </div>
  </article>
</template>

<script lang="ts">
import { InfoBox } from 'bkui-vue';
import { useI18n } from 'vue-i18n';

export default {
  name: 'GroupAside',
  props: {
  },
  emits: ['create-group', 'choose-group'],
  data() {
    const { t } = useI18n();

    return {
      activeTab: '管理员',
      groupList: [
        {
          name: '管理员',
          id: 1,
          user: 10,
          group: 10,
        },
        {
          name: '查看项目权限组',
          id: 2,
          user: 10,
          group: 10,
        },
        {
          name: '开发人员',
          user: 10,
          group: 10,
          id: 3,
        },
        {
          name: '产品人员',
          user: 10,
          group: 10,
          id: 4,
        },
        {
          name: '测试人员',
          user: 10,
          group: 10,
          id: 5,
        },
        {
          name: '运维人员',
          user: 10,
          group: 10,
          id: 6,
        },
        {
          name: '质管人员',
          user: 10,
          group: 10,
          id: 7,
        },
      ],
      t,
    };
  },
  mounted() {
  },
  methods: {
    handleDeleteGroup(group: any) {
      const { t } = this;
      InfoBox({
        title: t('是否删除用户组', [group.name]),
        contentAlign: 'center',
        headerAlign: 'center',
        footerAlign: 'center',
        onConfirm() {
          return '';
        },
      });
    },
    handleChangeTab(group: any) {
      this.activeTab = group.name;
      this.$emit('choose-group');
    },
    handleCreateGroup() {
      this.activeTab = '';
      this.$emit('create-group');
    },
    handleCloseManage() {
      console.log('关闭权限管理');
    },
  },
};
</script>

<style lang="postcss" scoped>
  .group-aside {
    width: 240px;
    height: 100%;
    background-color: #FAFBFD;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  .group-list {
    margin-top: 8px;
    .group-item {
      display: flex;
      align-items: center;
      width: 100%;
      height: 40px;
      line-height: 40px;
      font-size: 14px;
      padding-left: 24px;
      color: #63656E;
      cursor: pointer;
      &:hover {
        color: #3A84FF;
        background-color: #E1ECFF;
        .user-num,
        .group-num {
          background-color: #A3C5FD;
          color: #fff;
        }
      }
    }
    .group-active {
      color: #3A84FF;
      background-color: #E1ECFF;
      .user-num,
      .group-num {
        background-color: #A3C5FD;
        color: #fff;
      }
    }
    .name {
      display: inline-block;
      min-width: 110px;
    }
    .user-num,
    .group-num {
      display: flex;
      align-items: center;
      justify-content: space-evenly;
      width: 40px;
      height: 16px;
      background: #F0F1F5;
      border-radius: 2px;
      font-size: 12px;
      line-height: 16px;
      margin-right: 3px;
      text-align: center;
      color: #979BA5;
    }
    .more-icon {
      border-radius: 50%;
      padding: 1px;
      &:hover {
        background-color: #d5ddef;
        color: #3A84FF !important;
      }
    }
    .line-split {
      width: 80%;
      height: 1px;
      background: #ccc;
      margin: 10px auto;
    }
    .add-group-btn {
      display: flex;
      align-items: center;
      .add-icon {
        margin-right: 10px;
      }
    }
    .group-more-option {
      .btn {
        width: 60px;
        height: 32px;
        line-height: 32px;
        text-align: center;
        font-size: 12px;
        &:hover {
          background-color: #F5F7FA;
        }
      }
    }
  }
  ::v-deep {
    .bk-popover-content {
      padding: 0;
    }
  }
  .close-btn {
    margin-bottom: 20px;
    text-align: center;
  }
</style>

