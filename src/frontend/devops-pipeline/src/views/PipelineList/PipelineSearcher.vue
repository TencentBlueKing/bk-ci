<template>
    <bk-search-select
        class="search-pipeline-input"
        clearable
        :show-condition="false"
        :filter="true"
        :data="dropList"
        :placeholder="$t('searchPipelinePlaceholder')"
        :value="value"
        :values="initValues"
        @change="handleChange"
        v-on="$listeners"
    />
</template>

<script>
    import { mapState, mapGetters, mapActions } from 'vuex'
    import {
        PIPELINE_FILTER_PIPELINENAME,
        PIPELINE_FILTER_CREATOR,
        PIPELINE_FILTER_VIEWIDS,
        PIPELINE_FILTER_LABELS
    } from '@/utils/pipelineConst'

    export default {
        name: 'pipeline-searcher',
        props: {
            value: {
                type: Array,
                default: () => ({})
            }
        },
        data () {
            return {
                initValues: []
            }
        },
        computed: {
            ...mapState('pipelines', [
                'tagGroupList'
            ]),
            ...mapGetters('pipelines', [
                'groupMap'
            ]),
            searchConditions () {
                return {
                    [PIPELINE_FILTER_PIPELINENAME]: this.$t('pipelineName'),
                    [PIPELINE_FILTER_CREATOR]: this.$t('creator'),
                    [PIPELINE_FILTER_VIEWIDS]: this.$t('projectViewList')
                }
            },
            dropList () {
                const originList = [
                    {
                        id: PIPELINE_FILTER_PIPELINENAME,
                        classify: PIPELINE_FILTER_PIPELINENAME,
                        name: this.$t('pipelineName')

                    },
                    {
                        id: PIPELINE_FILTER_CREATOR,
                        classify: PIPELINE_FILTER_CREATOR,
                        name: this.$t('creator')

                    },
                    {
                        id: PIPELINE_FILTER_VIEWIDS,
                        classify: PIPELINE_FILTER_VIEWIDS,
                        name: this.$t('projectViewList'),
                        multiable: true,
                        children: Object.values(this.groupMap).filter(item => item.projected && item.viewType === 2).map(item => ({
                            id: item.id,
                            name: item.name
                        }))
                    },
                    ...this.tagGroupList.filter(item =>
                        Array.isArray(item.labels) && item.labels.length > 0
                    ).map(item => ({
                        classify: PIPELINE_FILTER_LABELS,
                        id: item.id,
                        name: item.name,
                        multiable: true,
                        children: item.labels
                    }))
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
            }
        },
        watch: {
            dropList () {
                this.initValues = this.parseQuery(this.value)
            }
        },
        mounted () {
            this.init()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTagList'
            ]),
            async init () {
                await this.requestTagList(this.$route.params)
            },
            getFilterLabelValues (keyName, ids) {
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
                        classify: keyName,
                        id: key,
                        name: this.tagGroupMap[key].name,
                        values: value
                    }
                })
            },
            parseQuery (query) {
                return this.filterKeys.map(key => {
                    const item = query[key]

                    if (item) {
                        const values = item.split(',')
                        switch (key) {
                            case PIPELINE_FILTER_LABELS:
                                return this.getFilterLabelValues(key, values)
                            case PIPELINE_FILTER_VIEWIDS:
                                return {
                                    classify: key,
                                    id: key,
                                    name: this.searchConditions[key],
                                    values: values.map(id => ({
                                        id,
                                        name: this.groupMap[id]?.name ?? 'invalid'
                                    }))
                                }
                            default:
                                return {
                                    classify: key,
                                    name: this.searchConditions[key],
                                    values: values.map(name => ({
                                        id: name,
                                        name
                                    }))
                                }
                        }
                    }
                    return null
                }).filter(item => item !== null).flat()
            },
            formatValue (originVal) {
                return originVal.reduce((acc, filter) => {
                    if (acc[filter.classify]) {
                        acc[filter.classify] += `,${filter.values.map(val => val.id).join(',')}`
                    } else {
                        acc[filter.classify] = filter.values.map(val => val.id).join(',')
                    }
                    return acc
                }, {})
            },
            handleChange (value) {
                const formatVal = this.formatValue(value)
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
