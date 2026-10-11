import dayjs from 'dayjs'

/** Preserve yudao's millisecond LocalDateTime request contract. */
export const payrollDateTimes = (data: object, fields: string[]) => {
  const payload: Record<string, any> = { ...data }
  for (const field of fields) {
    const value = payload[field]
    if (value === undefined || value === null || value === '') {
      payload[field] = undefined
      continue
    }
    const date = dayjs(value)
    if (!date.isValid()) throw new Error('日期时间格式不正确，请重新选择')
    payload[field] = date.valueOf()
  }
  return payload
}
