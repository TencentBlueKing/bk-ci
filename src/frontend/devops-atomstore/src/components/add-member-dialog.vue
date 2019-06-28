<template>
    <bk-dialog
        class="add-member-dialog"
        v-model="showDialog"
        title="新增成员"
        ok-text="保存"
        :width="width"
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
                        <label class="bk-label">成员名称：</label>
                        <div class="bk-form-content member-item-content">
                            <input type="text" class="bk-form-input member-name-input" placeholder="请输入成员名称"
                                name="memberName"
                                v-model="memberForm.memberName"
                                v-validate="{
                                    required: true
                                }"
                                :class="{ 'is-danger': errors.has('memberName') }">
                            <div v-if="errors.has('memberName')" class="error-tips">成员名称不能为空</div>
                        </div>
                    </div>
                    <div class="bk-form-item member-form-item is-required">
                        <label class="bk-label">角色：</label>
                        <div class="bk-form-content member-item-content">
                            <bk-radio-group v-model="memberForm.type" class="radio-group">
                                <bk-radio :value="entry.value" v-for="(entry, key) in typeList" :key="key">{{entry.label}}</bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                    <div class="bk-form-item member-form-item is-required">
                        <label class="bk-label">权限列表：</label>
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
                        @click="toConfirm">
                        保存
                    </bk-button>
                    <bk-button class="bk-dialog-btn bk-dialog-btn-cancel" @click="toCloseDialog">
                        取消
                    </bk-button>
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
                    { name: '插件开发', active: true },
                    { name: '版本发布', active: true },
                    { name: '成员管理', active: true }
                ],
                memberForm: {
                    memberName: '',
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
                    this.permissionList[2].active = false
                    this.permissionList[3].active = false
                }
            },
            showDialog (val) {
                if (!val) {
                    this.$validator.reset()
                    this.memberForm.memberName = ''
                    this.memberForm.type = 'ADMIN'
                }
            }
        },
        methods: {
            async toConfirm () {
                const valid = await this.$validator.validate()
                if (valid) {
                    const params = {
                        atomCode: this.atomCode,
                        type: this.memberForm.type,
                        member: []
                    }
                    params.member.push(this.memberForm.memberName)
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
            padding: 25px 0 15px;
            width: 96%;
            text-align: left;
            .bk-label {
                width: 100px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 100px;
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
            .permission-name {
                margin-left: 16px;
                padding: 4px 6px;
                border: 1px solid $borderColor;
                border-radius: 22px;
                font-size: 12px;
                color: $fontLigtherColor;
                &:first-child{
                    margin-left: 0;
                }
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
