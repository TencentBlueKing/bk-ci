<template>
    <div>
        <bk-tag-input
            class="manage-user-selector"
            clearable
            v-model="tagValue"
            :placeholder="$t('输入交接人，选中回车进行校验')"
            :search-key="searchKeyArr"
            save-key="id"
            display-key="displayName"
            allow-auto-match
            :list="userList"
            :paste-fn="pasteFn"
            @inputchange="handleInputUserName"
            @change="handleChange"
            @removeAll="removeAll"
        >
        </bk-tag-input>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'

    export default ({
        name: 'ProjectUserSelector',
        props: {
            projectId: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                searchKeyArr: ['id', 'name'],
                userList: [],
                tagValue: []
            }
        },
        created () {
            this.fetchProjectMembers()
        },
        methods: {
            ...mapActions(['getProjectMembers']),
            async fetchProjectMembers (query) {
                const search = {
                    projectId: this.projectId,
                    params: query
                }
                const res = await this.getProjectMembers(search)
                this.userList = res.records.map(i => {
                    return {
                        ...i,
                        name: i.name || i.id,
                        displayName: i.type === 'user' ? (!i.name ? i.id : `${i.id} (${i.name})`) : i.id
                    }
                })
            },

            async handleInputUserName (val) {
                this.$emit('change', { list: val, userList: this.userList })
                if (val) {
                    const query = {
                        memberType: 'user',
                        page: 1,
                        pageSize: 400,
                        userName: val,
                        globalError: false
                    }
                    await this.fetchProjectMembers(query)
                }
            },

            handleChange (list) {
                // 确保只能输入或粘贴一个tag（:max-data="1" 时禁止粘贴）
                this.tagValue = list.slice(-1)
                this.$emit('change', { list: this.tagValue, userList: this.userList })
            },
            pasteFn (val) {
                if (this.tagValue) {
                    this.tagValue = []
                }
                return this.userList.filter(i => i.id === val || i.name === val)
            },

            removeAll (val) {
                this.$emit('removeAll', val)
            }
        }
    })
</script>
