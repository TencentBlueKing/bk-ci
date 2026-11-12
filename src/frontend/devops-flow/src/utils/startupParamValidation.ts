import { getVariableValueIssue, VAR_MAX_LENGTH } from '../../../common-lib/variableValue'

export { VAR_MAX_LENGTH, getVariableValueIssue }

export function validateStartupValues(
  values: Record<string, unknown>,
  t: (key: string, params: Record<string, string | number>) => string,
): void {
  for (const [key, value] of Object.entries(values)) {
    const issue = getVariableValueIssue(value)
    if (issue) {
      throw new Error(t(
        issue.reference ? 'flow.variable.overflowReference' : 'flow.variable.valueTooLong',
        { key, length: issue.length ?? 0, max: VAR_MAX_LENGTH },
      ))
    }
  }
}
