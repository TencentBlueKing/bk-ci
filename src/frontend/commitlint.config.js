module.exports = {
    'extends': ['@commitlint/config-conventional'],
    'rules': {
        'type-enum': [2, 'always', ['feature', 'feat', 'bug', 'fix', 'bugfix', 'refactor', 'perf', 'test', 'docs', 'info', 'format', 'merge', 'depend', 'chore', 'del']],
        'subject-valid': [2, 'always']
    },
    'plugins': [
        {
            'rules': {
                'subject-valid': function ({ subject }) {
                    console.log('it is a subject', subject)
                    return [
                        /[story|bug|task]=\d+$/i.test(subject),
                        `commit-msg should end with (--[story|bug|task]={Id})`
                    ]
                }
            }
        }
    ]
}
