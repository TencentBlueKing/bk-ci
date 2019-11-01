<template>
    <bk-dialog
        class="add-member-dialog"
        v-model="showDialog" :title="$t('新增成员')"
        :ok-text="$t('保存')"
        :width="580"
        :close-icon="addMemberConf.closeIcon"
        :quick-close="addMemberConf.quickClose"
    >
        <main class="member-logo-content"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="add-member-content">
                <form class="bk-form add-member-form g-form-radio" onsubmit="return false">
                    <div class="bk-form-item member-form-item is-required">
                        <label class="bk-label"> {{ $t('成员名称：') }} </label>
                        <div class="bk-form-content member-item-content">
                            <bk-select
                                searchable
                                multiple
                                show-select-all
                                v-model="memberForm.list"
                                @selected="selectMember"
                            >
                                <bk-option v-for="(option, index) in memberList"
                                    :key="index"
                                    :id="option.id"
                                    :name="option.name">
                                </bk-option>
                            </bk-select>
                            <div class="prompt-tips"> {{ $t('若列表中找不到用户，请先将其添加为插件所属调试项目的成员') }} </div>
                            <div class="error-tips" v-if="nameError"> {{ $t('成员名称不能为空') }}</div>
                        </div>
                    </div>
                    <div class="bk-form-item member-form-item is-required">
                        <label class="bk-label"> {{ $t('角色：') }} </label>
                        <div class="bk-form-content member-item-content">
                            <bk-radio-group v-model="memberForm.type" class="radio-group">
                                <bk-radio :value="entry.value" v-for="(entry, key) in typeList" :key="key">{{entry.label}}</bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                    <div class="bk-form-item member-form-item is-required">
                        <label class="bk-label"> {{ $t('权限列表：') }} </label>
                        <div class="bk-form-content permission-list-content">
                            <div class="permission-name" :class="{ 'active-item': entry.active }" v-for="(entry, index) in permissionList" :key="index">
                                {{ entry.name }}
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </main>
        <template slot="footer">
            <div class="bk-dialog-outer">
                <template>
                    <bk-button theme="primary" class="bk-dialog-btn bk-dialog-btn-confirm bk-btn-primary"
                        @click="toConfirm"> {{ $t('保存') }} </bk-button>
                    <bk-button class="bk-dialog-btn bk-dialog-btn-cancel" @click="toCloseDialog"> {{ $t('取消') }} </bk-button>
                </template>
            </div>
        </template>
    </bk-dialog>
</template>

<script>
    import { mapGetters } from 'vuex'

    export default {
        props: {
            showDialog: Boolean
        },
        data () {
            return {
                width: 520,
                nameError: false,
                memberList: [],
                typeList: [
                    { label: 'Owner', value: 'ADMIN' },
                    { label: 'Developer', value: 'DEVELOPER' }
                ],
                permissionList: [
                    { name: this.$t('插件开发'), active: true },
                    { name: this.$t('版本发布'), active: true },
                    { name: this.$t('私有配置'), active: true },
                    { name: this.$t('审批'), active: true },
                    { name: this.$t('可见范围'), active: true },
                    { name: this.$t('成员管理'), active: true }
                ],
                memberForm: {
                    list: [],
                    type: 'ADMIN'
                },
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            ...mapGetters('store', {
                'currentAtom': 'getCurrentAtom'
            }),
            atomCode () {
                return this.$route.params.atomCode
            },
            addMemberConf () {
                return {
                    hasHeader: false,
                    hasFooter: true,
                    closeIcon: false,
                    quickClose: false
                }
            }
        },
        watch: {
            'memberForm.type' (val) {
                if (val === 'ADMIN') {
                    this.permissionList.map(item => {
                        item.active = true
                    })
                } else {
                    this.permissionList[1].active = false
                    this.permissionList[2].active = false
                    this.permissionList[3].active = false
                    this.permissionList[4].active = false
                }
            },
            showDialog (val) {
                if (!val) {
                    this.nameError = false
                    // this.$refs.memberSelector.$children[0].localTagList = []
                    this.memberForm.list = []
                    this.memberForm.type = 'ADMIN'
                }
            },

            currentAtom (newVal) {
                if (newVal) {
                    this.getMemberList()
                }
            }
        },

        created () {
            if (this.currentAtom.projectCode) {
                this.getMemberList()
            }
        },

        methods: {
            async getMemberList () {
                try {
                    const res = await this.$store.dispatch('store/requestProjectMember', {
                        projectCode: this.currentAtom.projectCode
                    })
                    this.memberList.splice(0, this.memberList.length)
                    if (res) {
                        res.map(item => {
                            this.memberList.push({
                                id: item,
                                name: item
                            })
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },

            selectMember (data) {
                this.memberForm.list = data
                this.nameError = false
            },

            handleChange () {
                this.nameError = false
            },

            async toConfirm () {
                const valid = await this.$validator.validate()
                if (valid) {
                    const params = {
                        storeCode: this.atomCode,
                        type: this.memberForm.type,
                        member: this.memberForm.list
                    }
                    this.$emit('confirmHandle', params)
                }
            },
            toCloseDialog () {
                this.$emit('cancelHandle')
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';
    .add-member-dialog {
        .add-member-content {
            display: flex;
            justify-content: space-between;
            text-align: center;
            .member-form-item {
                display: block;
            }
        }
        .add-member-form {
            flex: 1;
            padding: 25px 0 15px;
            text-align: left;
            .bk-label {
                padding-right: 18px;
                width: 95px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 95px;
            }
            .prompt-tips {
                font-size: 12px;
                color: #939393;
            }
        }
        .bk-member-selector {
            .bk-selector-member {
                padding: 4px 10px;
                display: flex;
                align-items: center;
            }
            .avatar {
                width: 28px;
                height: 28px;
                border-radius: 50%;
            }
        }
        .permission-list-content {
            display: flex;
            justify-content: space-between;
            .permission-name {
                padding: 4px 9px;
                border: 1px solid $borderColor;
                border-radius: 22px;
                font-size: 12px;
                color: $fontLigtherColor;
            }
            .active-item {
                border-color: $primaryColor;
                color: $fontBoldColor;
                .bk-icon {
                    color: $primaryColor;
                }
            }
        }
        .handle-footer {
            padding: 10px 20px;
            border: 1px solid #DDE4EB;
            text-align: right;
            button {
                margin-top: 0;
                margin-right: 0;
                width: 70px;
                min-width: 70px;
            }
        }
        .bk-selector {
            .bk-form-checkbox {
                display: block;
                padding: 12px 0;
            }
        }
    }
</style>
