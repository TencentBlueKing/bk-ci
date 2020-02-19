<template>
    <router-view :key="$route.path"></router-view>
</template>

<script>
    export default {
        data () {
            return {
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        created () {
            this.$store.commit('artifactory/updateRootSideMenu', {
                index: 3,
                key: 'isOpen',
                value: true
            })
        }
    }
</script>
