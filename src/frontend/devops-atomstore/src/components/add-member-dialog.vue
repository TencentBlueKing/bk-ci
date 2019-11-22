<template>
    <bk-dialog
        class="add-member-dialog"
        v-model="showDialog"
        :title="$t('新增成员')"
        :ok-text="$t('保存')"
        :width="580"
        :close-icon="addMemberConf.closeIcon"
        :quick-close="addMemberConf.quickClose"
        @confirm="toConfirm"
        @cancel="toCloseDialog"
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
                            <bk-input type="text" :placeholder="$t('请输入成员名称')"
                                name="memberName"
                                v-model="memberForm.memberName"
                                v-validate="{
                                    required: true
                                }"
                                :class="{ 'is-danger': errors.has('memberName') }">
                            </bk-input>
                            <div v-if="errors.has('memberName')" class="error-tips"> {{ $t('成员名称不能为空') }} </div>
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
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            showDialog: Boolean,
            projectCode: String,
            permissionList: Array
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
            'memberForm.type': {
                handler (val) {
                    this.permissionList.forEach((item) => {
                        item.active = (item.type === val || val === 'ADMIN')
                    })
                },
                immediate: true
            },
            showDialog (val) {
                if (!val) {
                    this.memberForm.memberName = ''
                    this.memberForm.type = 'ADMIN'
                }
            }
        },
        methods: {
            toConfirm () {
                if (!this.memberForm.memberName) {
                    this.nameError = true
                    this.$bkMessage({
                        message: this.$t('请输入成员名称'),
                        theme: 'error'
                    })
                    this.$emit('cancelHandle')
                } else {
                    const params = {
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
