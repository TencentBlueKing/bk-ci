<template>
    <vertical-tab :tabs="tabs"></vertical-tab>
</template>

<script>
    import VerticalTab from './VerticalTab'
    export default {
        name: 'auth-tab',
        components: {
            VerticalTab
        },
        props: {
            pipelineAuthority: Object,
            projectGroupAndUsers: Array,
            updateAuthority: Function
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            tabs () {
                return [{
                    id: 'role',
                    name: this.$t('settings.accordingRoles'),
                    component: 'AuthoritySetting',
                    componentProps: {
                        authList: this.pipelineAuthority.role,
                        projectGroupAndUsers: this.projectGroupAndUsers,
                        setType: 'role_code',
                        titleName: this.$i18n.locale === 'en-US' ? 'role_code' : 'role_name',
                        handleUpdate: this.generateUpdateAuthCb('role'),
                        isLoading: !this.pipelineAuthority.role
                    }
                }, {
                    id: 'func',
                    name: this.$t('settings.accordingFunc'),
                    component: 'AuthoritySetting',
                    componentProps: {
                        authList: this.pipelineAuthority.policy,
                        projectGroupAndUsers: this.projectGroupAndUsers,
                        setType: 'policy_code',
                        titleName: this.$i18n.locale === 'en-US' ? 'policy_code' : 'policy_name',
                        handleUpdate: this.generateUpdateAuthCb('policy'),
                        isLoading: !this.pipelineAuthority.policy
                    }
                }]
            }
        },
        methods: {
            generateUpdateAuthCb (name) {
                return value => {
                    this.updateAuthority(name, value)
                }
            }
        }
    }
</script>
