<template>
    <div>
        <bk-tag-input
            ref="tagInputRef"
            class="manage-user-selector"
            clearable
            :placeholder="$t('输入授权人，选中回车进行校验')"
            :search-key="searchKeyArr"
            save-key="id"
            display-key="displayName"
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
                searchValue: null,
                pasteValue: ''
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
                // this.searchValue = null
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
                    return
                }
                this.userList = []
            },

            handleChange (list) {
                // this.searchValue = list
                this.$emit('change', { list: list, userList: this.userList })
            },
            pasteFn (val) {
                if (this.$refs.tagInputRef) {
                    this.$refs.tagInputRef.curInputValue = val
                    this.handleInputUserName(val)
                }
                return []
            },

            removeAll (val) {
                this.$emit('removeAll', val)
            }
        }
    })
</script>
