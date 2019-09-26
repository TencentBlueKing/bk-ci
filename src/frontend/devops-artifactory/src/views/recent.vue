<template>
    <div class="code-repo-recent">
        <app-exception :type="'building'"></app-exception>
    </div>
</template>

<script>
    import appException from '@/components/common/exception'

    export default {
        components: {
            'app-exception': appException
        }
    }
</script>
