<template>
    <bk-tag-input
        allow-create
        :value="value"
        :disabled="disabled || isLoading"
        :placeholder="placeholder"
        :save-key="'english_name'"
        :display-key="'chinese_name'"
        :search-key="'english_name'"
        :list="list"
        :tag-tpl="renderMemberTag"
        :tpl="renderMerberList"
        :create-tag-validator="detect"
        :paste-fn="paste"
        @change="handleSelect"
    >
    </bk-tag-input>
    
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        name: 'staff-input',
        mixins: [atomFieldMixin],
        props: {
            value: {
                type: Array,
                required: true,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            },
            placeholder: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isLoading: false,
                curInsertVal: '',
                list: [],
                initData: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.init()
        },
        methods: {
            async init () {
                if (this.isLoading) return
                try {
                    this.isLoading = true
                    const prefix = `${location.host.indexOf('o.ied.com') > -1 ? OIED_URL : OPEN_URL}/component/compapi/tof3`
                    const { data } = await this.$ajax.jsonp(`${prefix}/get_all_staff_info`, {
                        query_type: 'simple_data',
                        app_code: 'workbench'
                    }, {
                        cache: true,
                        cacheKey: '__BK_TOF3_STAFFS__'
                    })
                    this.list = data
                } catch (error) {
                    console.error(error)
                } finally {
                    this.isLoading = false
                }
            },
            renderMemberTag (node) {
                return (
                    <div class="selected-staff-tag">
                        <img src={this.localCoverAvatar(node.english_name)} />
                        <span>{node.english_name}</span>
                    </div>
                )
            },
            renderMerberList (node, ctx, highlightKeyword) {
                const innerHtml = `${highlightKeyword(node.english_name)} (${node.chinese_name})`
                return (
                    <div class='bk-selector-node bk-selector-member'>
                        <img class='avatar' src={this.localCoverAvatar(node.english_name)} />
                        <span class='text' domPropsInnerHTML={innerHtml}></span>
                    </div>
                )
            },
            
            localCoverAvatar (data) {
                const member = data.isBkVar() ? 'un_know' : data
                return `${USER_IMG_URL}/avatars/${member}/avatar.jpg`
            },
            detect (val) {
                if (val.startsWith('$')) {
                    return val.isBkVar()
                }
                return true
            },
            async handleSelect (value) {
                try {
                    const currentValueMap = this.value.reduce((acc, item) => {
                        acc[item] = true
                        return acc
                    }, {})
                    
                    const res = await Promise.all(value.map(item => {
                        if (currentValueMap[item] || item.isBkVar()) {
                            return true
                        }
                        return this.detectIsInProject(item)
                    }))

                    const invalidUser = value.filter((_, index) => !res[index]).join(',')
                    if (invalidUser) {
                        this.$bkMessage({
                            theme: 'error',
                            message: this.$t('unAccessUser', [invalidUser])
                        })
                    }
                    this.handleChange(this.name, value.filter((_, index) => res[index]))
                } catch (error) {
                    console.error(error)
                    this.handleChange(this.name, this.value)
                }
            },
            async detectIsInProject (val) {
                try {
                    const res = await this.$ajax(`/project/api/user/projects/${this.projectId}/users/${val}/verify`)
                    return res.data
                } catch (error) {
                    return false
                }
            },
            paste (val) {
                this.handleSelect([
                    ...this.value,
                    ...val.split(',').filter(v => !this.value.includes(v))
                ])
            }
        }
    }
</script>

<style lang="scss">
    .selected-staff-tag {
        display: flex;
        align-items: center;
        grid-gap: 6px;
        height: 22px;
        font-size: 12px;
        padding: 0 4px;
        > img {
            width: 20px;
            height: 20px;
            border-radius: 50%;
        }
    }
</style>
