<template>
    <article class="group-aside" v-bkloading="{ isLoading: !groupList.length }">
        <span class="group-title">{{ $t('权限角色') }}</span>
        <scroll-load-list
            class="group-list"
            ref="loadList"
            :list="groupList"
            :has-load-end="hasLoadEnd"
            :get-data-method="handleGetData"
        >
            <template v-slot="{ data: group }">
                <div
                    :class="{ 'group-item': true, 'group-active': activeTab === group.groupId }"
                    @click="handleChangeTab(group)"
                >
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
            </template>
        </scroll-load-list>
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
        <div class="close-btn">
            <bk-button @click="showCloseManageDialog">{{ $t('关闭权限管理') }}</bk-button>
        </div>
        <bk-dialog
            header-align="center"
            theme="danger"
            :quick-close="false"
            ext-cls="close-manage-dialog"
            :show-footer="false"
            :value="closeObj.isShow"
            @cancel="handleHiddenCloseManage"
        >
            <template #header>
                <img src="./svg/warning-circle-fill.svg" style="width: 42px;">
                <p class="close-title">{{ $t('确认关闭【】的权限管理？', [resourceType === 'pipeline' ? projectName : groupName]) }}</p>
            </template>
            <div class="close-tips">
                
                <p>{{ $t('关闭流水线权限管理，将执行如下操作：', [resourceType === 'pipeline' ? $t('流水线') : $t('流水线组')]) }}</p>
                <p>
                    <img src="./svg/warning-circle-fill.svg" style="width: 14px;">
                    {{ $t('将编辑者、执行者、查看者中的用户移除') }}
                </p>
                <p>
                    <img src="./svg/warning-circle-fill.svg" style="width: 14px;">
                    {{ $t('删除对应组内用户继承该组的权限') }}
                </p>
                <p>
                    <img src="./svg/warning-circle-fill.svg" style="width: 14px;">
                    {{ $t('删除对应组信息和组权限') }}
                </p>
            </div>
            <div class="confirm-close">
                <i18n path="提交后，再次开启权限管理时对应组内用户将不能恢复，请谨慎操作!" style="color: #737987;font-size: 14px;" tag="div">
                    <span style="color: red;">{{$t('不能恢复')}}</span>
                </i18n>
            </div>
            <div class="option-btns">
                <bk-button
                    class="close-btn"
                    theme="danger"
                    @click="handleCloseManage"
                >
                    {{ $t('关闭权限管理') }}
                </bk-button>
                <bk-button
                    class="btn"
                    @click="handleHiddenCloseManage"
                >
                    {{ $t('取消') }}
                </bk-button>
            </div>
        </bk-dialog>
    </article>
</template>

<script>
    import ScrollLoadList from '../scroll-load-list'
    export default {
        name: 'GroupAside',
        components: {
            ScrollLoadList
        },
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
            closeManage: {
                type: Function,
                default: () => {}
            },
            activeIndex: {
                type: Boolean,
                default: 0
            },
            showCreateGroup: {
                type: Boolean
            },
            projectName: {
                type: String
            },
            groupName: {
                type: String
            }
        },
        data () {
            return {
                activeTab: '',
                closeObj: {
                    isShow: false,
                    isLoading: false
                },
                groupList: [],
                hasLoadEnd: false
            }
        },
        watch: {
            activeIndex (newVal) {
                this.activeTab = this.groupList[newVal]?.groupId || ''
            }
        },
        methods: {
            handleGetData (page, pageSize) {
                return this
                    .$ajax
                    .get(`/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/listGroup?page=${page}&pageSize=${pageSize}`)
                    .then(({ data }) => {
                        this.hasLoadEnd = !data.hasNext
                        this.groupList.push(...data.records)
                        // 首页需要加载
                        if (page === 1) {
                            this.handleChangeTab(this.groupList[0])
                        }
                    })
            },
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
                    .$ajax
                    .delete(`/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.deleteObj.group.groupId}`)
                    .then(() => {
                        this.deleteObj.isLoading = false
                        this.handleHiddenDeleteGroup()
                        this.groupList = []
                        this.hasLoadEnd = false
                        this.$refs.loadList.resetList()
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
            showCloseManageDialog () {
                this.closeObj.isShow = true
                // this.closeManage()
            },
            handleHiddenCloseManage () {
                this.closeObj.isShow = false
            },
            handleCloseManage () {
                console.log(123)
            }
        }
    }
</script>

<style lang="scss" scoped>
.group-aside {
  width: 240px;
  height: 100%;
  background-color: #fff;
  border-right: 1px solid #dde0e6;
}
.group-list {
  max-height: calc(100% - 62px);
  height: auto;
  overflow-y: auto;
}
.group-title {
  display: inline-block;
  line-height: 50px;
  padding-left: 24px;
  width: 100%;
  // background-image: linear-gradient(transparent 49px, rgb(220, 222, 229) 1px);
  font-size: 14px;
  margin-bottom: 8px;
  font-weight: 700;
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
  margin-right: 8px;
  color: #979BA5;
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

.close-manage-dialog {
    .title-icon {
        font-size: 42px;
        color: #ff9c01;
        margin-bottom: 15px;
    }
    .close-title {
        margin-top: 10px;
        white-space: normal !important;
    }
    .bk-dialog-header {
        padding: 15px 0;
    }
    .bk-dialog-title {
        height: 26px !important;
        overflow: initial !important;
    }
    .confirm-close {
        margin: 15px 0 30px;
    }
    .close-tips {
        padding: 10px;
        background: #f5f6fa;
    }
    .option-btns {
        text-align: center;
        margin-top: 20px;
        .close-btn {
            margin-right: 10px;
            margin-bottom: 0 !important;
        }
        .btn {
            width: 88px;
        }
    }
}
</style>
<style lang="scss">
.group-more-option .bk-tooltip-ref {
  height: 18px;
  display: flex;
  align-items: center;
}
</style>
