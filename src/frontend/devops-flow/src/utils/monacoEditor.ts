declare global {
  interface Window {
    BKCI_YAML_SCHEMA_URI?: string
  }
}

class MonacoEditor {
  static monaco?: typeof import('monaco-editor');


  static async instance() {
    if (!this.monaco) {
      this.monaco = await this.init()
      return this.monaco
    }
    return this.monaco
  }

  /**
   * Initialize Monaco Editor with YAML support
   */
  static async init() {
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
      /* webpackChunkName: "monaco-yaml" */
      'monaco-yaml'
    )

    // Configure YAML language support
    monacoYaml.configureMonacoYaml(monaco, {
      enableSchemaRequest: true,
      schemas: [
        {
          fileMatch: ['*.yml', '*.yaml'],
          uri: window.BKCI_YAML_SCHEMA_URI || ''
        }
      ]
    })

    // Define custom theme for code editor
    monaco.editor.defineTheme('flowCodeTheme', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'comment', foreground: '6A9955' },
        { token: 'keyword', foreground: '569CD6' },
        { token: 'string', foreground: 'CE9178' },
        { token: 'number', foreground: 'B5CEA8' }
      ],
      colors: {
        'editor.background': '#1E1E1E',
        'editor.foreground': '#D4D4D4',
        'editorLineNumber.foreground': '#858585',
        'editor.selectionBackground': '#264F78',
        'editor.inactiveSelectionBackground': '#3A3D41'
      }
    })

    // Set theme
    monaco.editor.setTheme('flowCodeTheme')

    return monaco
  }
}

export default MonacoEditor
