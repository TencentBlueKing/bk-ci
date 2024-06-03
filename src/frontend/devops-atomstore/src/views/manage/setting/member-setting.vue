<template>
    <article class="member-setting">
        <h5 class="member-header">
            <bk-button theme="primary" @click="openAddMember" :disabled="!userInfo.isProjectAdmin">{{ $t('store.新增成员') }}</bk-button>
            <span>{{ $t('store.目前有') }} <span>{{ memberCount }}</span> {{ $t('store.名成员') }}</span>
        </h5>

        <section v-bkloading="{ isLoading }" class="g-scroll-table">
            <bk-table :data="memberList" :outer-border="false" :header-border="false" :header-cell-style="{ background: '#fff' }" v-if="!isLoading">
                <bk-table-column :label="$t('store.成员')" prop="userName" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.调试项目')" show-overflow-tooltip>
                    <template slot-scope="props">
                        <section class="member-project">
                            <template v-if="props.row.editing">
                                <bk-select :disabled="false" v-model="props.row.projectCode" :loading="isLoadingProject" searchable style="width: 250px">
                                    <bk-option v-for="option in projectList"
                                        :key="option.projectCode"
                                        :id="option.projectCode"
                                        :name="option.projectName"
                                        :disabled="!option.enabled">
                                    </bk-option>
                                </bk-select>
                                <i class="bk-icon icon-check-1" @click="saveChangeProject(props.row)"></i>
                                <i class="bk-icon icon-close" @click="props.row.editing = false"></i>
                            </template>
                            <template v-else>
                                <span>{{ props.row.projectName }}</span>
                                <i class="bk-icon icon-edit2" @click="startEditProject(props.row)" v-if="userInfo.isProjectAdmin || userInfo.userName === props.row.userName"></i>
                            </template>
                        </section>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.角色')" width="160" prop="type" :formatter="typeFormatter" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.描述')" prop="type" :formatter="desFormatter" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="120" class-name="handler-btn">
                    <template slot-scope="props">
                        <span :class="[{ 'disable': !userInfo.isProjectAdmin } ,'update-btn']" @click="handleDelete(props.row)"> {{ $t('store.删除') }} </span>
                    </template>
                </bk-table-column>
            </bk-table>

            <bk-sideslider
                :is-show.sync="addMemberObj.isShow"
                :quick-close="true"
                :title="$t('store.新增成员')"
                :width="640"
                :before-close="closeAddMember">
                <bk-form :label-width="100" :model="addMemberObj.form" slot="content" class="add-member" ref="addForm">
                    <bk-form-item :label="$t('store.成员名称')" :desc="$t('store.若列表中找不到用户，请先将其添加为调试项目的成员')" :required="true" :rules="[requireRule($t('store.成员名称'))]" property="memberName" error-display-type="normal">
                        <bk-input v-model="addMemberObj.form.memberName" @change="handleChangeForm"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.角色')" property="type">
                        <bk-radio-group v-model="addMemberObj.form.type" @change="handleChangeForm" class="radio-group">
                            <bk-radio :value="key" v-for="(entry, key) in memberType" :key="key">{{entry}}</bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.权限列表')">
                        <labelList :label-list="getPermissionList(addMemberObj.form.type)"></labelList>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click="saveMember" :loading="isSaving">{{ $t('store.保存') }}</bk-button>
                        <bk-button @click="closeAddMember" :disabled="isSaving">{{ $t('store.取消') }}</bk-button>
                    </bk-form-item>
                </bk-form>
            </bk-sideslider>
        </section>

        <bk-dialog v-model="deleteObj.show"
            :loading="deleteObj.loading"
            @confirm="requestDeleteMember"
            @cancel="deleteObj.show = false"
            :title="$t('store.删除')"
        >
            {{`${$t('store.确定删除成员')}(${deleteObj.user})？`}}
        </bk-dialog>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import labelList from '@/components/labelList.vue'
    import api from '@/api'

    export default {
        components: {
            labelList
        },

        data () {
            return {
                memberCount: 0,
                memberList: [],
                memberType: {
                    ADMIN: 'Owner',
                    DEVELOPER: 'Developer'
                },
                permissionMap: {
                    atom: [
                        { name: this.$t('store.插件开发'), active: false, type: 'DEVELOPER' },
                        { name: this.$t('store.版本发布'), active: false, type: 'DEVELOPER' },
                        { name: this.$t('store.私有配置'), active: false, type: 'DEVELOPER' },
                        { name: this.$t('store.审批'), active: false, type: 'ADMIN' },
                        { name: this.$t('store.成员管理'), active: false, type: 'ADMIN' }
                    ],
                    image: [
                        { name: this.$t(this.$t('store.镜像发布')), active: false, type: 'DEVELOPER' },
                        { name: this.$t('store.审批'), active: false, type: 'ADMIN' },
                        { name: this.$t('store.成员管理'), active: false, type: 'ADMIN' },
                        { name: this.$t('store.可见范围'), active: false, type: 'ADMIN' }
                    ]
                },
                addMemberObj: {
                    isShow: false,
                    form: {
                        memberName: '',
                        type: 'ADMIN'
                    }
                },
                isLoading: true,
                isSaving: false,
                deleteObj: {
                    show: false,
                    loading: false,
                    user: '',
                    id: ''
                },
                projectList: []
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail',
                userInfo: 'getUserInfo'
            }),

            storeType () {
                const typeMap = {
                    atom: 'ATOM',
                    image: 'IMAGE'
                }
                const type = this.$route.params.type
                return typeMap[type]
            },

            storeCode () {
                const keyMap = {
                    atom: 'atomCode',
                    image: 'imageCode'
                }
                const type = this.$route.params.type
                const key = keyMap[type]
                return this.detail[key]
            }
        },

        created () {
            this.initData()
        },

        methods: {
            saveChangeProject (row) {
                this.isLoading = true
                const data = {
                    storeMember: row.userName,
                    projectCode: row.projectCode,
                    storeCode: this.storeCode,
                    storeType: this.storeType
                }
                api.requestChangeProject(data).then(() => {
                    return this.initData()
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            startEditProject (row) {
                row.editing = true
                row.isLoadingProject = true
                this.$store.dispatch('store/requestProjectList', { enabled: true }).then((res) => {
                    this.projectList = res || []
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    row.isLoadingProject = false
                })
            },

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            },

            getPermissionList (userType) {
                const type = this.$route.params.type
                const currentPermission = this.permissionMap[type] || []
                const filterPermission = currentPermission.filter(x => userType === 'ADMIN' || x.type === userType)
                return filterPermission.map(x => x.name)
            },

            openAddMember () {
                window.changeFlag = false
                this.addMemberObj.isShow = true
            },

            saveMember () {
                this.$refs.addForm.validate().then(() => {
                    this.isSaving = true
                    const postData = {
                        type: this.addMemberObj.form.type,
                        member: [this.addMemberObj.form.memberName],
                        storeCode: this.storeCode,
                        storeType: this.storeType
                    }
                    api.requestAddMember(postData).then(() => {
                        this.addMemberObj.form = {
                            memberName: '',
                            type: 'ADMIN'
                        }
                        setTimeout(() => {
                            this.addMemberObj.isShow = false
                        })
                        this.initData()
                    }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isSaving = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            closeAddMember () {
                if (window.changeFlag) {
                    this.$bkInfo({
                        title: this.$t('确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('离开将会导致未保存信息丢失')),
                        okText: this.$t('离开'),
                        confirmFn: () => {
                            this.addMemberObj.form = {
                                memberName: '',
                                type: 'ADMIN'
                            }
                            setTimeout(() => {
                                this.addMemberObj.isShow = false
                            })
                            return true
                        }
                    })
                } else {
                    this.addMemberObj.isShow = false
                    this.addMemberObj.form = {
                        memberName: '',
                        type: 'ADMIN'
                    }
                }
            },

            typeFormatter (row, column, cellValue, index) {
                return this.memberType[cellValue]
            },

            desFormatter (row, column, cellValue, index) {
                const permissionList = this.getPermissionList(cellValue)
                return permissionList.join(' ')
            },

            initData () {
                this.isLoading = true
                const data = {
                    storeCode: this.storeCode,
                    storeType: this.storeType
                }
                api.getMemberList(data).then((res) => {
                    this.memberCount = res.length
                    this.memberList = (res || []).map(x => {
                        x.editing = false
                        x.isLoadingProject = false
                        return x
                    })
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            handleDelete (row) {
                if (!this.userInfo.isProjectAdmin) return
                this.deleteObj.show = true
                this.deleteObj.user = row.userName
                this.deleteObj.id = row.id
            },

            requestDeleteMember () {
                this.deleteObj.loading = true
                const data = {
                    id: this.deleteObj.id,
                    storeCode: this.storeCode,
                    storeType: this.storeType
                }
                api.requestDeleteMember(data).then(() => {
                    this.memberCount--
                    const index = this.memberList.findIndex(x => x.id === this.deleteObj.id)
                    this.memberList.splice(index, 1)
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.deleteObj.loading = false
                    this.deleteObj.show = false
                })
            },

            handleChangeForm () {
                window.changeFlag = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    .member-setting {
        background: #fff;
        padding: 3.2vh;
        .member-header {
            margin-bottom: 3.2vh;
            color: #666;
            font-size: 14px;
            font-weight: normal;
            button {
                margin-right: 14px;
            }
        }
        .add-member {
            padding: 32px;
            ::v-deep .bk-form-radio:not(:last-child) {
                margin-right: 32px;
            }
        }
        .member-project {
            display: flex;
            align-items: center;
            .bk-icon {
                font-size: 24px;
                cursor: pointer;
            }
        }
        .g-scroll-table {
            ::v-deep .bk-table .bk-table-body-wrapper {
                height: calc(100% - 43px);
            }
        }
    }
</style>
