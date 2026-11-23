export function classNames(...values: Array<string | false | null | undefined>) {
  return values.filter((value): value is string => Boolean(value)).join(' ')
}
