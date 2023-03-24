<template>
  <article class="group-aside">
    <section class="group-list">
      <span class="group-title">{{ $t('权限角色') }}</span>
      <scroll-load-list
        class="loading-content"
        ref="loadList"
        :list="groupList"
        :hasLoadEnd="hasLoadEnd"
        :getDataMethod="handleGetData"
      >
        <template v-slot="{ data: group }">
          <div
            :class="{ 'group-item': true, 'group-active': activeTab === group.groupId }"
            @click="handleChangeTab(group)">
            <bk-input
              ref="renameInput"
              v-show="group.groupId === activeGroupId && isRename"
              v-model="displayGroupName"
              class="rename-input"
              @enter="handleRename"
              @blur="handleRename"
            >
            </bk-input>
            <div class="item-content" v-show="group.groupId !== activeGroupId">
              <span class="group-name" :title="group.name">{{ group.name }}</span>
              <span class="user-num">
                <i class="manage-icon small-size manage-icon-user-shape"></i>
                {{ group.userCount }}
              </span>
              <span class="group-num">
                <i class="manage-icon small-size manage-icon-organization"></i>
                {{ group.departmentCount }}
              </span>
              <bk-popover
                v-if="resourceType === 'project'"
                class="group-more-option"
                placement="bottom"
                trigger="click"
                theme="dot-menu light"
                :arrow="false"
                offset="15"
                :distance="0">
                <i class="more-icon manage-icon manage-icon-more-fill"></i>
                <template #content>
                  <div class="menu-content">
                    <bk-button
                      v-if="!group.defaultGroup"
                      class="btn"
                      text
                      @click="handleShowRename(group, index)"
                    >
                      {{ $t('重命名') }}
                    </bk-button>
                    <bk-button
                      :class="{
                        'btn': true,
                        'is-disable': group.defaultGroup
                      }"
                      :disabled="group.defaultGroup"
                      text
                      @click="handleShowDeleteGroup(group)">
                      {{ $t('删除') }}
                    </bk-button>
                  </div>
                </template>
              </bk-popover>
            </div>
          </div>
        </template>
      </scroll-load-list>
      <div class="line-split" />
      <div
        :class="{ 'group-item create-group-btn': true, 'group-active': activeTab === '' }"
        @click="handleCreateGroup">
        <span class="add-group-btn">
          <i class="manage-icon manage-icon-add-fill add-icon"></i>
          {{ $t('新建用户组') }}
        </span>
      </div>
    </section>
    <bk-dialog
      dialogType="show"
      header-align="center"
      theme="danger"
      :quick-close="false"
      extCls="delete-group-dialog"
      :is-show="deleteObj.isShow"
      :is-loading="deleteObj.isLoading"
    >
      <template #header>
        <div class="manage-icon manage-icon-warning-circle-fill title-icon"></div>
        <p class="delete-title">{{ $t('确认删除【】用户组？', [deleteObj.group.name]) }}</p>
      </template>
      <div class="delete-tips">
        <p>{{ $t('删除用户组【】将执行如下操作：', [deleteObj.group.name]) }}</p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ $t('将用户和组织从组中移除') }}
        </p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ $t('删除组内用户继承该组的权限') }}
        </p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ $t('删除组信息和组权限') }}
        </p>
      </div>
      <div class="confirm-delete">
        <i18n-t keypath="此操作提交后将不能恢复，为避免误删除，请再次确认你的操作：" style="color: #737987;font-size: 14px;" tag="div">
          <span style="color: red;">{{$t('不能恢复')}}</span>
        </i18n-t>
        <bk-input
          v-model="keyWords"
          :placeholder="$t('请输入待删除的用户组名')"
          class="confirm-input"
        ></bk-input>
      </div>
      <div class="option-btns">
        <bk-button
          class="btn"
          theme="danger"
          :disabled="disableDeleteBtn"
          @click="handleDeleteGroup"
        >
          {{ $t('删除') }}
        </bk-button>
        <bk-button
          class="btn"
          @click="handleHiddenDeleteGroup"
        >
          {{ $t('取消') }}
        </bk-button>
      </div>
    </bk-dialog>
  </article>
</template>

<script>
import { InfoBox } from 'bkui-vue'
import ScrollLoadList from './scroll-load-list'
import http from '@/http/api';

export default {
  name: 'GroupAside',
  components: {
    ScrollLoadList
  },
  props: {
    // 资源类型
    resourceType: {
      type: String,
      default: '',
    },
    // 资源ID
    resourceCode: {
      type: String,
      default: '',
    },
    // 项目id => englishName
    projectCode: {
      type: String,
      default: '',
    },
    closeManage: {
      type: Function,
      default: () => {},
    },
    deleteGroup: {
      type: Function,
      default: () => {},
    },
    activeIndex: {
      type: Boolean,
      default: 0,
    },
    renameGroupName: {
      type: Function,
      default: () => {},
    }
  },
  emits: ['create-group', 'choose-group', 'delete-group'],
  data() {
    return {
      activeTab: '',
      deleteObj: {
        group: {},
        isShow: false,
        isLoading: false,
      },
      keyWords: '',
      isRename: false,
      activeGroupId: 0,
      displayGroupName: '',
      groupList: [],
      hasLoadEnd: false,
    };
  },
  computed: {
    disableDeleteBtn() {
      return !(this.keyWords === this.deleteObj.group.name);
    },
  },
  watch: {
    activeIndex(newVal) {
      this.activeTab = this.groupList[newVal]?.groupId || '';
    },
  },
  methods: {
    handleGetData (page, pageSize) {
      return http
        .fetchUserGroupList(
          {
            resourceType: this.resourceType,
            resourceCode: this.resourceCode,
            projectCode: this.projectCode,
          },
          {
            page,
            pageSize
          }
        )
        .then((data) => {
          this.hasLoadEnd = !data.hasNext;
          this.groupList.push(...data.records);
          // 首页需要加载
          if (page === 1) {
            this.handleChangeTab(this.groupList[0])
          }
        });
    },
    handleShowRename (group, index) {
      this.isRename = true;
      this.activeGroupId = group.groupId;
      this.displayGroupName = group.name;
      setTimeout(() => {
        this.$refs.renameInput[index].focus();
      });
    },
    handleRename () {
      const group = this.groupList.find(i => i.groupId === this.activeGroupId);
      if (this.displayGroupName === group.name) {
        this.isRename = false;
        this.activeGroupId = 0;
        this.displayGroupName = '';
        return
      }
      return this
        .renameGroupName({
          groupId: this.activeGroupId,
          groupName: this.displayGroupName,
        })
        .then(() => {
          group.name = this.displayGroupName;
        })
        .finally(() => {
          this.isRename = false;
          this.activeGroupId = 0;
          this.displayGroupName = '';
        })
    },
    handleShowDeleteGroup(group) {
      this.deleteObj.group = group;
      this.deleteObj.isShow = true;
    },
    handleHiddenDeleteGroup() {
      this.deleteObj.isShow = false;
      this.deleteObj.group = {};
      this.keyWords = '';
    },
    handleDeleteGroup() {
      this.deleteObj.isLoading = true;
      return http
        .deleteGroup(this.deleteObj.group)
        .then(() => {
          this.deleteObj.isLoading = false;
          this.handleHiddenDeleteGroup();
          this.groupList = [];
          this.hasLoadEnd = false;
          this.$refs.loadList.resetList()
        });
    },
    handleChangeTab(group) {
      this.activeTab = group.groupId;
      this.$emit('choose-group', group);
    },
    handleCreateGroup() {
      this.activeTab = '';
      this.$emit('create-group');
    },
    handleCloseManage() {
      this.closeManage();
    },
  },
};
</script>

<style lang="postcss" scoped>
  .group-list {
    height: 100%;
  }
  .loading-content {
    min-height: 100px;
    max-height: calc(100% - 160px);
    height: auto;
    &::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
  }
  .group-title {
    display: inline-block;
    line-height: 50px;
    padding-left: 24px;
    width: 100%;
    /* background-image: linear-gradient(transparent 49px, rgb(220, 222, 229) 1px); */
    font-size: 14px;
    margin-bottom: 8px;
    font-weight: 700;
  }
  .group-aside {
    width: 240px;
    height: 100%;
    background-color: #fff;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    border-right: 1px solid #dde0e6;
  }
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
      .small-size {
        color: #fff
      }
      .add-icon {
        color: #3f86ff;
      }
      .more-icon {
        color: #63656e;
      }
    }
  }
  .group-item:hover {
    color: #3A84FF;
    background-color: #E1ECFF;
  }
  .group-item:hover .user-num,
  .group-item:hover .group-num {
    background-color: #A3C5FD;
    color: #fff;
  }
  .item-content {
    display: flex;
    align-items: center;
  }
  .rename-input {
    width: 90%;
  }
  .group-active {
    color: #3A84FF;
    background-color: #E1ECFF;
    .user-num, .group-num {
      background-color: #A3C5FD;
      color: #fff;
    }
    .small-size {
      color: #fff
    }
    .add-icon {
      color: #3f86ff;
    }
    .more-icon {
      color: #63656e;
    }
  }
  .user-num,
  .group-num {
    background-color: #A3C5FD;
    color: #fff;
  }
  .group-name {
    display: inline-block;
    width: 100px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
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
    color: #C4C6CC;
  }
  .more-icon {
    border-radius: 50%;
    padding: 1px;
  }
  .more-icon:hover {
    background-color: #d5ddef;
    color: #3A84FF !important;
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
  }
  .add-icon {
    margin-right: 8px;
    color: #979BA5;
  }

  .group-more-option .btn {
    width: 60px;
    height: 32px;
    line-height: 32px;
    text-align: center;
    font-size: 12px;
    margin-top: 0;
    color: #63656E;
  }
  .group-more-option .btn:hover {
    background-color: #F5F7FA;
  }
  .menu-content {
    display: flex;
    flex-direction: column;
    .is-disable {
      color: #dcdee5;
    }
  }
  :deep(.bk-popover-content) {
    padding: 0;
  }
  .close-btn {
    margin-bottom: 20px;
    text-align: center;
  }
  .small-size {
    scale: 0.9;
    color: #C4C6CC;
  }
  .delete-title {
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .delete-tips {
    padding: 10px 25px;
    background-color: #F5F6FA;
    p {
      display: flex;
      align-items: center;
    }
  }
  .warning-icon {
    margin-right: 5px;
    color: #FF9C01;
  }
</style>

<style lang="postcss">
  .delete-group-dialog {
    .title-icon {
      font-size: 42px;
      color: #ff9c01;
      margin-bottom: 15px;
    }
    .delete-title {
      white-space: normal !important;
    }
    .bk-dialog-header {
      padding: 15px 0;
    }
    .bk-dialog-title {
      height: 26px !important;
      overflow: initial !important; 
    }
    .confirm-delete {
      margin: 15px 0;
    }
    .confirm-input {
      margin-top: 15px;
    }
    .option-btns {
      text-align: center;
      margin-top: 20px;
      .btn {
        width: 88px;
        margin-right: 10px;
      }
    }
  }
</style>
