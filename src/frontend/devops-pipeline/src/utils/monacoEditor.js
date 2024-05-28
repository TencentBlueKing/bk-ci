import ciYamlTheme from '@/utils/ciYamlTheme'
class MonacoEditor {
    static monaco = null;

    static async instance () {
        if (!this.monaco) {
            this.monaco = await this.init()
            return this.monaco
        }
        return this.monaco
    }

    static async init () {
        const monaco = await import(
            /* webpackMode: "lazy" */
            /* webpackPrefetch: true */
            /* webpackPreload: true */
            /* webpackChunkName: "monaco-editor" */
            'monaco-editor'
        )
        const monacoYaml = await import(
            /* webpackMode: "lazy" */
            /* webpackPrefetch: true */
            /* webpackPreload: true */
            /* webpackChunkName: "monaco-editor" */
            'monaco-yaml'
        )
        monacoYaml.configureMonacoYaml(monaco, {
            enableSchemaRequest: true,
            schemas: [
                {
                    fileMatch: ['*'],
                    uri: window.BKCI_YAML_SCHEMA_URI
                }
            ]
        })

        monaco.editor.defineTheme('ciYamlTheme', ciYamlTheme)
        monaco.editor.setTheme('ciYamlTheme')

        return monaco
    }
}

export default MonacoEditor
