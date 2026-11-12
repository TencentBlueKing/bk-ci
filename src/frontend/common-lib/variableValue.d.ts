export const VAR_MAX_LENGTH: 4000
export const PIPELINE_VARIABLES_OVERFLOW_PREFIX: string
export function isOverflowReference(value: unknown): boolean
export function getOverflowLength(value: unknown): number | null
export function getVariableValueText(value: unknown): string
export function getVariableValueIssue(value: unknown): { reference: boolean; length: number | null } | null
