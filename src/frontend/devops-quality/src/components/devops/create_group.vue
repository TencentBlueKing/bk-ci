<template>
    <bk-dialog v-model="nodeSelectConf.isShow"
        :width="'640'"
        :ext-cls="'experience-group-wrapper'"
        :close-icon="nodeSelectConf.closeIcon"
        :show-footer="nodeSelectConf.hasFooter">
        <div v-if="nodeSelectConf.isShow"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="experience-group-header">
                <div class="title">{{nodeSelectConf.title}}</div>
            </div>

            <form class="bk-form create-group-form" id="create-group-form">
                <div class="bk-form-wrapper">
                    <bk-form :label-width="100" :model="createGroupForm">
                        <devops-form-item :label="$t('quality.通知组名称')" :required="true" :property="'name'"
                            :is-error="errors.has('groupName')"
                            :error-msg="errors.first('groupName')">
                            <bk-input
                                class="group-name-input"
                                :placeholder="$t('quality.最长不超过10个汉字')"
                                name="groupName"
                                v-model="createGroupForm.name"
                                v-validate="{
                                    required: true,
                                    max: 10
                                }">
                            </bk-input>
                        </devops-form-item>
                        <bk-form-item :label="$t('quality.通知人员：')" :property="'internal_list'">
                            <user-input
                                :handle-change="onChange"
                                name="innerList"
                                :value="createGroupForm.internal_list"
                                :placeholder="$t('quality.请输入通知人员')"
                            ></user-input>
                        </bk-form-item>
                        <bk-form-item :label="$t('quality.备注：')" :property="'desc'">
                            <bk-input
                                class="group-desc-textarea"
                                type="textarea"
                                name="groupDesc"
                                maxlength="100"
                                v-model="createGroupForm.desc"
                            ></bk-input>
                        </bk-form-item>
                    </bk-form>
                </div>
            </form>
            <div class="footer">
                <bk-button theme="primary" @click.native="confirm">{{$t('quality.确认')}}</bk-button>
                <bk-button @click="cancelFn">{{$t('quality.取消')}}</bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import UserInput from '@/components/devops/UserInput/index.vue'
    import clickoutside from '@/directives/clickoutside'

    export default {
        directives: {
            clickoutside
        },
        components: {
            UserInput
        },
        props: {
            nodeSelectConf: Object,
            createGroupForm: Object,
            loading: Object,
            errorHandler: Object,
            onChange: Function,
            onInit: Function,
            confirmFn: Function,
            cancelFn: Function,
            displayResult: Function
        },
        data () {
            return {
                isDropdownShow: false,
                placeholder: this.$t('quality.仅填写项目组内的人员有效'),
                userGroupList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            isDropdownShow (val) {
                if (!val) {
                    const resetList = []

                    this.$store.dispatch('experience/updateselectUserGroup', {
                        userList: resetList
                    })
                }
            }
        },
        created () {
            this.requestGroups()
        },
        methods: {
            importMember () {
                this.isDropdownShow = !this.isDropdownShow
                if (this.isDropdownShow) {
                    this.requestGroups()
                }
            },
            close () {
                this.isDropdownShow = false
            },
            async requestGroups () {
                try {
                    const res = await this.$store.dispatch('quality/requestUserGroup', {
                        projectId: this.projectId
                    })

                    this.userGroupList.splice(0, this.userGroupList.length)
                    if (res) {
                        res.forEach(item => {
                            this.userGroupList.push(item)
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
            selectUsers (entry) {
                this.isDropdownShow = false

                this.$store.dispatch('quality/updateselectUserGroup', {
                    userList: entry.users
                })
            },
            async confirm () {
                const isValid = await this.$validator.validate()
                if (isValid) {
                    const params = {
                        name: this.createGroupForm.name,
                        innerUsers: this.createGroupForm.internal_list,
                        outerUsers: this.createGroupForm.external_list,
                        remark: this.createGroupForm.desc
                    }
                    this.$emit('confirmFn', params)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .experience-group-wrapper{
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .experience-group-header {
            padding-left: 20px;
            height: 54px;
            line-height: 54px;
            border-bottom: 1px solid $borderWeightColor;
            font-weight: bold;
        }
        .create-group-form {
            margin: 24px 30px 22px 20px;
            .no-required .bk-label:after {
                display: none;
            }
            .bk-label {
                padding-right: 14px;
                width: 108px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 108px;
            }
        }
        .select-tags {
            max-height: 104px;
            overflow: auto;
        }
        .dropdown-menu {
            position: relative;
            width: 130px;
        }
        .dropdown-trigger {
            margin-top: 4px;
            width: 130px;
            height: 32px;
            border: 1px solid #c3cdd7;
            text-align: center;
            line-height: 32px;
            cursor: pointer;
            &:hover {
                background-color: #fafafa;
                color: #737987;
            }
            .devops-icon {
                display: inline-block;
                transition: all ease 0.2s;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }
        .dropdown-list {
            position: absolute;
            top: 35px;
            min-width: 100%;
            max-height: 250px;
            background: #fff;
            padding: 0;
            margin: 0;
            z-index: 99;
            overflow: auto;
            border-radius: 2px;
            border: 1px solid #c3cdd7;
            transition: all .3s ease;
            box-shadow: 0 2px 6px rgba(51,60,72,.1);
            a {
                display: block;
                line-height: 41px;
                padding: 0 15px;
                color: #737987;
                font-size: 14px;
                text-decoration: none;
                white-space: nowrap;
                &:hover {
                    background-color: #ebf4ff;
                    color: $primaryColor;
                }
            }
        }
        .bk-data-wrapper {
            padding: 5px;
            .placeholder-text {
                position: absolute;
                top: 10px;
                left: 10px;
                color: #c3cdd7
            }
        }
        .bk-data-editor {
            max-height: 70px;
            overflow: auto;
        }
        .footer {
            padding: 12px 24px;
            text-align: right;
            background: #FAFBFD;
            border-top: 1px solid #DDE4EB;
        }
    }
</style>
