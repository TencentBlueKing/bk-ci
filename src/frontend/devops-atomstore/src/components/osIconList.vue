<template>
    <span v-if="osList.length > 0">
        <template v-if="withParentheses">（</template>
        <i
            v-for="os in osList"
            :key="os"
            class="devops-icon"
            :class="osIconMap[os]"
        ></i>
        <template v-if="withParentheses">）</template>
    </span>
</template>

<script>
    export default {
        props: {
            jobTypeConfigs: {
                type: Array,
                default: () => []
            },
            withParentheses: {
                type: Boolean,
                default: false
            }
        },

        data () {
            return {
                osIconMap: {
                    LINUX: 'icon-linux-view',
                    WINDOWS: 'icon-windows',
                    MACOS: 'icon-macos'
                }
            }
        },

        computed: {
            osList () {
                if (!Array.isArray(this.jobTypeConfigs) || this.jobTypeConfigs.length === 0) return []
                return [...new Set(
                    this.jobTypeConfigs.reduce((list, config) => {
                        if (Array.isArray(config.osList)) {
                            list.push(...config.osList)
                        }
                        return list
                    }, [])
                )]
            }
        }
    }
</script>
