<template>
    <bk-dialog v-model="nodeSelectConf.isShow"
        width="640"
        header-position="left"
        :close-icon="nodeSelectConf.closeIcon"
        :has-header="nodeSelectConf.hasHeader"
        :quick-close="nodeSelectConf.quickClose"
        :title="nodeSelectConf.title"
        :loading="isLoading"
        @confirm="handleSubmit"
        @cancel="cancelFn"
    >
        <bk-form :model="createGroupForm" ref="createGroupForm" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
            <bk-form-item label="体验组名称" :required="true" :rules="[{ required: true, trigger: 'blur', message: '体验组名称不能为空' }]" property="name">
                <bk-input
                    placeholder="最长不超过10个汉字"
                    maxlength="10"
                    name="groupName"
                    v-model="createGroupForm.name"
                />
            </bk-form-item>
            <bk-form-item label="内部人员" property="internal_list">
                <bk-member-selector
                    name="innerList"
                    :placeholder="placeholder"
                    :value="createGroupForm.internal_list"
                    @change="onChange"
                />
                <bk-dropdown-menu style="margin-top: 4px;" @show="importMember">
                    <bk-button slot="dropdown-trigger">
                        <span>从用户组导入</span>
                        <i :class="['bk-icon icon-angle-down',{ 'icon-flip': isDropdownShow }]"></i>
                    </bk-button>
                    <ul class="bk-dropdown-list" slot="dropdown-content">
                        <li v-for="(entry, index) in userGroupList" :key="index">
                            <a href="javascript:;" @click="selectUsers(entry.users)">
                                {{ entry.groupName }}
                                <span>({{ entry.users.length }})</span>
                            </a>
                        </li>
                    </ul>
                </bk-dropdown-menu>
            </bk-form-item>
            <bk-form-item label="外部QQ" property="external_list">
                <bk-input
                    type="textarea"
                    placeholder="请输入QQ号，以逗号或分号分隔"
                    name="groupExternalList"
                    :onkeyup="displayResult()"
                    v-model="createGroupForm.external_list"
                />
            </bk-form-item>
            <bk-form-item label="描述" property="desc">
                <bk-input
                    type="textarea"
                    placeholder="请输入"
                    name="groupDesc"
                    v-model="createGroupForm.desc"
                />
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'

    export default {
        props: {
            nodeSelectConf: Object,
            createGroupForm: Object,
            loading: Object,
            errorHandler: Object,
            onChange: Function,
            cancelFn: Function,
            displayResult: Function
        },
        data () {
            return {
                isDropdownShow: false,
                placeholder: '仅填写项目组内的人员有效',
                userGroupList: [],
                isLoading: false
            }
        },
        computed: {
            ...mapGetters('experience', [
                'getUserGroup'
            ]),
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
            },
            getUserGroup (val) {
                this.selectUsers(val)
            }
        },
        created () {
            this.requestGroups()
        },
        methods: {
            ...mapActions('experience', [
                'requestUserGroup',
                'updateselectUserGroup',
                'editUserGroups',
                'createUserGroups'
            ]),
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
                    const res = await this.requestUserGroup({
                        projectId: this.projectId
                    })

                    this.userGroupList.splice(0, this.userGroupList.length)
                    res.map(item => {
                        this.userGroupList.push(item)
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            selectUsers (users) {
                // this.close()
                const keyMap = this.createGroupForm.internal_list.reduce((keyMap, item) => {
                    keyMap[item] = true
                    return keyMap
                }, {})
                this.onChange([
                    ...this.createGroupForm.internal_list,
                    ...users.filter(v => !keyMap[v])
                ])
            },
            async handleSubmit () {
                let message, theme
                try {
                    await this.$refs.createGroupForm.validate()

                    const { createGroupForm, editUserGroups, createUserGroups } = this
                    const params = {
                        name: createGroupForm.name,
                        innerUsers: createGroupForm.internal_list,
                        outerUsers: createGroupForm.external_list,
                        remark: createGroupForm.desc
                    }
                    const action = createGroupForm.isEdit ? editUserGroups : createUserGroups
                    await action({
                        projectId: this.projectId,
                        params: params,
                        ...(createGroupForm.isEdit ? { groupHashId: this.createGroupForm.groupHashId } : {})
                    })

                    message = '保存成功'
                    theme = 'success'
                    this.$emit('after-submit')
                } catch (e) {
                    message = e.message || e.content
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.isLoading = false
                }
            }
        }
    }
</script>
