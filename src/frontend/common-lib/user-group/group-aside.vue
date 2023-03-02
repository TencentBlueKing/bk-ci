<template>
    <article class="group-aside" v-bkloading="{ isLoading: !groupList.length }">
        <section class="group-list">
            <span class="group-title">{{ $t('权限角色') }}</span>
            <div
                :class="{ 'group-item': true, 'group-active': activeTab === group.groupId }"
                v-for="(group, index) in groupList"
                :key="index"
                @click="handleChangeTab(group)"
                v-bkloading="{ isLoading: !groupList.length }">
                <span class="group-name" :title="group.name">{{ group.name }}</span>
                <span class="user-num">
                    <img src="./svg/user.svg" class="group-icon">
                    {{ group.userCount }}
                </span>
                <span class="group-num">
                    <img src="./svg/organization.svg" class="group-icon">
                    {{ group.departmentCount }}
                </span>
                <bk-popover
                    v-if="resourceType === 'project'"
                    class="group-more-option"
                    placement="bottom"
                    theme="dot-menu light"
                    :arrow="false"
                    offset="15"
                    :distance="0">
                    <img src="./svg/more.svg" class="more-icon">
                    <template #content>
                        <bk-button
                            class="btn"
                            :disabled="[1, 2].includes(group.id)"
                            text
                            @click="handleShowDeleteGroup(group)">
                            {{ $t('删除') }}
                        </bk-button>
                    </template>
                </bk-popover>
            </div>
            <div class="line-split" />
            <div
                v-if="showCreateGroup"
                :class="{ 'group-item': true, 'group-active': activeTab === '' }"
                @click="handleCreateGroup">
                <span class="add-group-btn">
                    <i class="bk-icon bk-icon-add-fill add-icon"></i>
                    {{ $t('新建用户组') }}
                </span>
            </div>
        </section>
        <div class="close-btn">
            <bk-button @click="handleCloseManage">{{ $t('关闭权限管理') }}</bk-button>
        </div>
        <bk-dialog
            header-align="center"
            theme="danger"
            quick-close
            :value="deleteObj.isShow"
            :title="$t('删除')"
            :is-loading="deleteObj.isLoading"
            @cancel="handleHiddenDeleteGroup"
            @confirm="handleDeleteGroup"
        >
            {{ $t('是否删除用户组', [deleteObj.group.name]) }}
        </bk-dialog>
    </article>
</template>

<script>
    export default {
        name: 'GroupAside',
        props: {
            // 资源类型
            resourceType: {
                type: String,
                default: ''
            },
            // 资源ID
            resourceCode: {
                type: String,
                default: ''
            },
            // 项目id => englishName
            projectCode: {
                type: String,
                default: ''
            },
            groupList: {
                type: Array,
                default: () => []
            },
            closeManage: {
                type: Function,
                default: () => {}
            },
            deleteGroup: {
                type: Function,
                default: () => {}
            },
            activeIndex: {
                type: Boolean,
                default: 0
            },
            showCreateGroup: {
                type: Boolean
            }
        },
        data () {
            return {
                activeTab: '',
                deleteObj: {
                    group: {},
                    isShow: false,
                    isLoading: false
                }
            }
        },
        watch: {
            groupList: {
                handler () {
                    if (this.groupList.length) {
                        this.handleChangeTab(this.groupList[0])
                    }
                },
                immediate: true
            },
            activeIndex (newVal) {
                this.activeTab = this.groupList[newVal]?.groupId || ''
            }
        },
        methods: {
            handleShowDeleteGroup (group) {
                this.deleteObj.group = group
                this.deleteObj.isShow = true
            },
            handleHiddenDeleteGroup () {
                this.deleteObj.isShow = false
                this.deleteObj.group = {}
            },
            handleDeleteGroup () {
                this.deleteObj.isLoading = true
                return this
                    .deleteGroup(this.deleteObj.group)
                    .then(() => {
                        this.deleteObj.isLoading = false
                        this.handleHiddenDeleteGroup()
                    })
            },
            handleChangeTab (group) {
                this.activeTab = group.groupId
                this.$emit('choose-group', group)
            },
            handleCreateGroup () {
                this.activeTab = ''
                this.$emit('create-group')
            },
            handleCloseManage () {
                this.closeManage()
            }
        }
    }
</script>

<style lang="scss" scoped>
.group-aside {
  width: 240px;
  height: 100%;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.group-list {
  max-height: calc(100% - 62px);
  overflow-y: auto;
}
.group-title {
  display: inline-block;
  line-height: 50px;
  padding-left: 24px;
  width: 100%;
  background-image: linear-gradient(transparent 49px, rgb(220, 222, 229) 1px);
  font-size: 14px;
  margin-bottom: 8px;
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
    .group-icon {
      filter: invert(100%) sepia(0%) saturate(1%) hue-rotate(151deg) brightness(104%) contrast(101%);
    }
  }
}
.group-item:hover .user-num,
.group-item:hover .group-num {
  background-color: #A3C5FD;
  color: #fff;
}

.group-active {
  color: #3A84FF;
  background-color: #E1ECFF;
  .user-num, .group-num {
    background-color: #A3C5FD;
    color: #fff;
  }
  .group-icon {
    filter: invert(100%) sepia(0%) saturate(1%) hue-rotate(151deg) brightness(104%) contrast(101%);
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
.group-icon {
  height: 12px;
  width: 12px;
  filter: invert(89%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
}
.more-icon {
  height: 18px;
  filter: invert(89%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
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
  margin-right: 10px;
}

.group-more-option {
  height: 18px;
  display: flex;
  align-items: center;
}
.close-btn {
  margin-bottom: 20px;
  text-align: center;
}
.small-size {
  scale: 0.9;
}
</style>
<style lang="scss">
.group-more-option .bk-tooltip-ref {
  height: 18px;
  display: flex;
  align-items: center;
}
</style>
