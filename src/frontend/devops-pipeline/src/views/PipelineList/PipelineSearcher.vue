<template>
    <search-select
        class="search-pipeline-input"
        :data="dropList"
        :placeholder="placeholder"
        :values="initValues"
        @change="handleChange"
    >
    </search-select>
</template>

<script>
    import {
        PIPELINE_FILTER_CREATOR,
        PIPELINE_FILTER_LABELS,
        PIPELINE_FILTER_PIPELINENAME,
        PIPELINE_FILTER_VIEWIDS
    } from '@/utils/pipelineConst'
    import TenantSingleton from '@/utils/tenant'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import { mapActions, mapGetters, mapState } from 'vuex'
    
    export default {
        name: 'PipelineSearcher',
        components: {
            SearchSelect
        },
        props: {
            value: {
                type: Array,
                default: () => ([])
            },
            isArchiveView: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                initValues: []
            }
        },
        computed: {
            ...mapState('pipelines', [
                'tagGroupList',
                'allPipelineGroup'
            ]),
            ...mapGetters('pipelines', [
                'groupMap'
            ]),
            searchConditions () {
                return {
                    [PIPELINE_FILTER_PIPELINENAME]: this.$t('pipelineName'),
                    [PIPELINE_FILTER_CREATOR]: this.$t('creator'),
                    [PIPELINE_FILTER_VIEWIDS]: this.$t('pipelineGroup')
                }
            },
            dropList () {
                const originList = [
                    {
                        id: PIPELINE_FILTER_PIPELINENAME,
                        default: true,
                        name: this.$t('pipelineName')

                    },
                    {
                        id: PIPELINE_FILTER_CREATOR,
                        default: true,
                        name: this.$t('creator'),
                        remoteMethod: TenantSingleton.fetchTenantUsers
                    },
                    ...(!this.isArchiveView
                        ? [{
                               id: PIPELINE_FILTER_VIEWIDS,
                               name: this.$t('pipelineGroup'),
                               default: true,
                               multiable: true,
                               children: this.allPipelineGroup.filter(item => item.viewType !== -1)
                           },
                           ...this.tagGroupList.filter(item =>
                               Array.isArray(item.labels) && item.labels.length > 0
                           ).map(item => ({
                               id: item.id,
                               name: item.name,
                               default: true,
                               multiable: true,
                               children: item.labels
                           }))]
                        : [])
                    
                ]
                return originList.filter(item => !this.value[item.id])
            },
            tagGroupMap () {
                return this.tagGroupList.reduce((acc, tagGroup) => {
                    acc[tagGroup.id] = tagGroup
                    return acc
                }, {})
            },
            labelMap () {
                return this.tagGroupList.reduce((acc, tagGroup) => {
                    return acc.concat(tagGroup.labels)
                }, []).reduce((acc, label) => {
                    acc[label.id] = label
                    return acc
                }, {})
            },
            filterKeys () {
                return [
                    PIPELINE_FILTER_PIPELINENAME,
                    PIPELINE_FILTER_CREATOR,
                    PIPELINE_FILTER_VIEWIDS,
                    PIPELINE_FILTER_LABELS
                ]
            },
            placeholder () {
                return !this.isArchiveView ? this.$t('searchPipelinePlaceholder') : this.$t('searchPipelineArchivePlaceholder')
            }
        },

        async created () {
            await this.requestTagList(this.$route.params)
            await this.parseQuery(this.value)
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTagList'
            ]),
            getFilterLabelValues (ids) {
                const tagGroupIdMap = ids.reduce((acc, id) => {
                    const label = this.labelMap[id]
                    if (label) {
                        acc[label.groupId] = [
                            ...(acc[label.groupId] ?? []),
                            label
                        ]
                    }
                    return acc
                }, {})

                return Object.entries(tagGroupIdMap).map(([key, value]) => {
                    return {
                        id: key,
                        name: this.tagGroupMap[key].name,
                        values: value
                    }
                })
            },
            async parseQuery (query) {
                const filters = await Promise.all(this.filterKeys.map(async key => {
                    const item = query[key]
                    if (typeof item === 'string'  && item) {
                        const values = item.split(',')
                        switch (key) {
                            case PIPELINE_FILTER_LABELS:
                                return this.getFilterLabelValues(values)
                            case PIPELINE_FILTER_VIEWIDS:
                                return {
                                    id: key,
                                    name: this.searchConditions[key],
                                    values: values.map(id => ({
                                        id,
                                        name: this.groupMap[id]?.name ?? 'invalid'
                                    }))
                                }
                            case PIPELINE_FILTER_CREATOR:
                                const ids = values.map(id => id)
                                const initRes = await TenantSingleton.fetchTenantDisplayNames(ids)
                                
                                return {
                                    id: key,
                                    name: this.searchConditions[key],
                                    values: initRes
                                }
                            default:
                                return {
                                    id: key,
                                    name: this.searchConditions[key],
                                    values: values.map(name => ({
                                        id: name,
                                        name
                                    }))
                                }
                        }
                    }
                    return Promise.resolve(null)
                }))
                this.initValues = filters.filter(item => item !== null).flat()
            },
            formatValue (originVal) {
                return originVal.reduce((acc, filter) => {
                    if (this.tagGroupMap[filter.id]) {
                        const tagIds = filter.values.map(val => val.id)
                        acc[PIPELINE_FILTER_LABELS] = (acc[PIPELINE_FILTER_LABELS] ? [acc[PIPELINE_FILTER_LABELS], ...tagIds] : tagIds).join(',')
                    } else {
                        acc[filter.id] = filter.values?.map?.(val => val.id).join(',')
                    }
                    return acc
                }, {})
            },
            handleChange (value) {
                const formatVal = this.formatValue(value)
                console.log(formatVal)
                this.filterKeys.forEach(key => {
                    if (!formatVal[key]) {
                        formatVal[key] = undefined
                    }
                })
                this.$emit('input', formatVal)
                this.$emit('update:value', formatVal)
            },
            clear () {
                this.initValues = []
            }
        }
    }
</script>
