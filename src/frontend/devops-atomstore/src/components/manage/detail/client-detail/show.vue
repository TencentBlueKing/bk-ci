<template>
    <article class="client-show">
        <client-basic-info
            :detail="detail"
            :show-colon="true"
        />
    </article>
</template>

<script>
    import ClientBasicInfo from '@/components/clientBasicInfo.vue'

    export default {
        name: 'ClientShow',
        components: {
            ClientBasicInfo
        },
        props: {
            detail: Object
        }
    }
</script>