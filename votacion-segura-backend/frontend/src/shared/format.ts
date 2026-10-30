export const formatDate = (date: string | null | undefined, withTime = true) => {
  if (!date) return '—'
  const parsed = new Date(date)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-EC', withTime
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'medium' }).format(parsed)
}

export const formatNumber = (number: number) => new Intl.NumberFormat('es-EC').format(number)

export const initials = (name: string) => name.split(/[ ._-]/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
