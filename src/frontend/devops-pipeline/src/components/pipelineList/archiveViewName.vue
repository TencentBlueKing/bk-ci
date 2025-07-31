<template>
    <span>
        {{ $t('archive.archiveLibrary') }}
        <i
            class="bk-icon icon-info-circle-shape tips-icon"
            v-bk-tooltips="archiveConfig"
        />
        <div
            id="archiveConfig"
            class="archive-config"
        >
            <p>{{ $t('archive.tips') }}</p>
        </div>
    </span>
</template>

<script>
    export default {
        data () {
            return {
                archiveConfig: {
                    allowHTML: true,
                    width: 350,
                    content: '#archiveConfig',
                    placement: 'right-start'
                }
            }
        }
    }
</script>

<style lang="scss">
.tips-icon {
  vertical-align: middle;
  color: #979BA5;
  margin-left: 4px;
}
.archive-config {
  white-space: pre-line;
}
</style>
