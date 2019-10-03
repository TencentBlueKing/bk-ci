<template>
    <vertical-tab :tabs="tabs"></vertical-tab>
</template>

<script>
    import VerticalTab from './VerticalTab'
    export default {
        name: 'notify-tab',
        components: {
            VerticalTab
        },
        props: {
            successSubscription: Object,
            failSubscription: Object,
            projectGroupAndUsers: Array,
            updateSubscription: Function
        },
        computed: {
            tabs () {
                return [{
                    id: 'success',
                    name: '构建成功时',
                    component: 'NotifySetting',
                    componentProps: {
                        subscription: this.successSubscription,
                        projectGroupAndUsers: this.projectGroupAndUsers,
                        updateSubscription: this.getUpdateFn(this.successSubscription)
                    }
                }, {
                    id: 'fail',
                    name: '构建失败时',
                    component: 'NotifySetting',
                    componentProps: {
                        subscription: this.failSubscription,
                        projectGroupAndUsers: this.projectGroupAndUsers,
                        updateSubscription: this.getUpdateFn(this.failSubscription)
                    }
                }]
            }
        },
        methods: {
            getUpdateFn (container) {
                return (name, value) => {
                    this.updateSubscription(container, name, value)
                }
            }
        }
    }
</script>
