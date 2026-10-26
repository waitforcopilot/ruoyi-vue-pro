// TEST attendance discussion adapter. Original punches are never rewritten by supporting documents.
import { demo } from './demo'
export type AttendanceKind = 'NORMAL' | 'LATE' | 'EARLY' | 'MISSING' | 'ABSENCE'
export interface AttendanceDay {
  id: string
  date: string
  scheduled: number
  rawPunch: number | null
  kind: AttendanceKind
  note: string
}
export interface AttendanceDocument {
  id: string
  recordId: string
  type: 'LEAVE' | 'TRAVEL' | 'OUTING' | 'CORRECTION'
  status: 'APPROVED' | 'PENDING' | 'REJECTED'
  hours: number
}
export interface AttendanceExample {
  label: string
  explanation: string
  days: readonly AttendanceDay[]
  documents: readonly AttendanceDocument[]
}
export const attendanceKinds: Record<AttendanceKind, string> = {
  NORMAL: '正常',
  LATE: '迟到（TEST阈值）',
  EARLY: '早退（TEST阈值）',
  MISSING: '缺卡（不等于缺勤）',
  ABSENCE: '已核实缺勤（TEST）'
}
export const attendanceDocumentTypes = {
  LEAVE: '请假',
  TRAVEL: '出差',
  OUTING: '外出',
  CORRECTION: '补卡'
}
export const attendanceDocumentStates = {
  APPROVED: '已批准（TEST）',
  PENDING: '待批准',
  REJECTED: '已驳回'
}
const normalDays: AttendanceDay[] = [
  {
    id: 'TEST-D01',
    date: '2026-10-01',
    scheduled: 8,
    rawPunch: 8,
    kind: 'NORMAL',
    note: '虚构班次1'
  },
  {
    id: 'TEST-D02',
    date: '2026-10-02',
    scheduled: 8,
    rawPunch: 0,
    kind: 'NORMAL',
    note: '批准带薪假覆盖整班8小时；原始打卡保持0'
  },
  {
    id: 'TEST-D03',
    date: '2026-10-03',
    scheduled: 8,
    rawPunch: 8,
    kind: 'NORMAL',
    note: '虚构班次3'
  },
  {
    id: 'TEST-D04',
    date: '2026-10-04',
    scheduled: 8,
    rawPunch: 8,
    kind: 'NORMAL',
    note: '虚构班次4'
  },
  {
    id: 'TEST-REST16',
    date: '其余16班汇总',
    scheduled: 128,
    rawPunch: 128,
    kind: 'NORMAL',
    note: '16班×8小时，均为TEST正常记录；不声称自然工作日口径'
  }
]
const leave: AttendanceDocument = {
  id: 'TEST-LEAVE-01',
  recordId: 'TEST-D02',
  type: 'LEAVE',
  status: 'APPROVED',
  hours: 8
}
const changedDays = (id: string, change: Partial<AttendanceDay>) =>
  normalDays.map((day) => (day.id === id ? { ...day, ...change } : { ...day }))
const missingTravel = changedDays('TEST-D03', {
  rawPunch: null,
  kind: 'MISSING',
  note: '原始缺卡不改写；对照同记录TEST-D03关联出差单批准前后'
})
const missingCorrection = changedDays('TEST-D04', {
  rawPunch: null,
  kind: 'MISSING',
  note: '原始缺卡不改写；对照同记录TEST-D04补卡前后'
})
const travel: AttendanceDocument = {
  id: 'TEST-TRAVEL-01',
  recordId: 'TEST-D03',
  type: 'TRAVEL',
  status: 'APPROVED',
  hours: 8
}
const correction: AttendanceDocument = {
  id: 'TEST-CORRECTION-01',
  recordId: 'TEST-D04',
  type: 'CORRECTION',
  status: 'APPROVED',
  hours: 8
}
const outing: AttendanceDocument = {
  id: 'TEST-OUTING-01',
  recordId: 'TEST-D03',
  type: 'OUTING',
  status: 'APPROVED',
  hours: 4
}
const outingDays = changedDays('TEST-D03', {
  rawPunch: 4,
  note: '打卡4小时＋外出单4小时；认可与原始打卡分开'
})
export const attendanceExamples = {
  NORMAL: {
    label: '正常日→月主样例',
    days: normalDays,
    documents: [leave],
    explanation: '20班×8＝160；认可出勤152＋批准带薪假8＝160；认可加班8独立列示。'
  },
  TRAVEL_PENDING: {
    label: '缺卡＋出差单未批（补正前）',
    days: missingTravel,
    documents: [leave, { ...travel, status: 'PENDING' }],
    explanation: 'TEST-D03缺卡、出差单未批准；出勤及工资影响保持未知，不用未批单覆盖记录。'
  },
  TRAVEL_APPROVED: {
    label: '缺卡＋出差单已批（补正后）',
    days: missingTravel,
    documents: [leave, travel],
    explanation:
      '同一TEST-D03认可出差8小时；原始打卡仍未知，月认可恢复152＋8。只是TEST认可，不改采集原件。'
  },
  CORRECTION_PENDING: {
    label: '缺卡＋补卡单未批（补正前）',
    days: missingCorrection,
    documents: [leave, { ...correction, status: 'PENDING' }],
    explanation: 'TEST-D04补卡待批准，保持原始缺卡及认可未知，不自动当缺勤或扣工资。'
  },
  CORRECTION_APPROVED: {
    label: '补卡已批＋重复单据（补正后）',
    days: missingCorrection,
    documents: [leave, correction, { ...correction }],
    explanation: '同一补卡单重复出现只认可一次8小时；同记录替换认可结果，不将原月数再叠加8。'
  },
  OUTING_APPROVED: {
    label: '外出已批：原始4→认可8',
    days: outingDays,
    documents: [leave, outing],
    explanation: 'TEST-D03原始打卡4小时不改写；批准外出4小时形成认可出勤8，月认可仍152＋8。'
  },
  OUTING_PENDING: {
    label: '外出未批：不覆盖原始4',
    days: outingDays,
    documents: [leave, { ...outing, status: 'PENDING' }],
    explanation:
      '未批准外出单不增加认可；已知出勤148＋假8＝156，缺口4小时待核对，不据此自动生成扣款。'
  },
  ANOMALIES: {
    label: '迟到/早退/缺卡/缺勤分别核对',
    days: [
      { ...normalDays[0], rawPunch: 7.5, kind: 'LATE', note: 'TEST迟到0.5小时，扣款规则待确认' },
      normalDays[1],
      { ...normalDays[2], rawPunch: 7, kind: 'EARLY', note: 'TEST早退1小时，扣款规则待确认' },
      { ...normalDays[3], rawPunch: null, kind: 'MISSING', note: '缺卡时长未知，不能自动判缺勤' },
      {
        id: 'TEST-D05',
        date: '2026-10-05',
        scheduled: 8,
        rawPunch: 0,
        kind: 'ABSENCE',
        note: '独立TEST核实缺勤8小时；扣款金额仍需规则依据'
      },
      {
        ...normalDays[4],
        id: 'TEST-REST15',
        date: '其余15班汇总',
        scheduled: 120,
        rawPunch: 120,
        note: '15班×8小时，正常记录'
      }
    ],
    documents: [leave],
    explanation: '四类异常分别标识。缺卡不等于已核实缺勤；未定义扣款规则，不将异常直接换算工资。'
  }
} satisfies Record<string, AttendanceExample>
export type AttendanceExampleKey = keyof typeof attendanceExamples
const knownHours = (value: number | null): value is number =>
  value !== null && Number.isFinite(value) && value >= 0
const sumHours = (values: readonly (number | null)[]) =>
  values.length && values.every(knownHours)
    ? Math.round(values.reduce<number>((sum, value) => sum + (value as number), 0) * 100) / 100
    : null
export function resolveAttendanceDay(day: AttendanceDay, documents: readonly AttendanceDocument[]) {
  const linked = documents.filter((doc) => doc.recordId === day.id)
  const approved = linked.filter((doc) => doc.status === 'APPROVED')
  const unique = new Map<string, AttendanceDocument>()
  let conflict = false
  for (const doc of approved) {
    const old = unique.get(doc.id)
    if (old && JSON.stringify(old) !== JSON.stringify(doc)) conflict = true
    else unique.set(doc.id, doc)
  }
  const attendance: number[] = [],
    leaveHours: number[] = []
  for (const doc of unique.values()) {
    if (!knownHours(doc.hours) || doc.hours > day.scheduled) {
      conflict = true
      continue
    }
    if (doc.type === 'LEAVE') leaveHours.push(doc.hours)
    else if (doc.type === 'OUTING') {
      if (knownHours(day.rawPunch)) attendance.push(day.rawPunch + doc.hours)
      else conflict = true
    } else attendance.push(doc.hours)
  }
  if (new Set(attendance).size > 1 || new Set(leaveHours).size > 1) conflict = true
  const paidLeave = leaveHours[0] ?? 0
  let recognized: number | null = attendance[0] ?? (paidLeave === day.scheduled ? 0 : day.rawPunch)
  if (
    !knownHours(recognized) ||
    !knownHours(day.scheduled) ||
    recognized + paidLeave > day.scheduled
  )
    recognized = null
  if (conflict) recognized = null
  return {
    ...day,
    linked,
    recognized,
    paidLeave: conflict ? null : paidLeave,
    covered: sumHours([recognized, conflict ? null : paidLeave]),
    approvedDocumentCount: unique.size,
    reason: conflict
      ? '单据冲突/重叠，认可保持未知，需独立核对'
      : approved.length
        ? '仅按批准单据形成认可；原始打卡不改写，重复单不累加'
        : linked.length
          ? '单据未批准/已驳回，保留原始记录，不新增认可'
          : '无补正单据，保留原始记录；缺卡不自动转缺勤'
  }
}
export function summarizeAttendance(example: AttendanceExample) {
  const days = example.days.map((day) => resolveAttendanceDay(day, example.documents))
  const duplicateRecords = new Set(days.map((day) => day.id)).size !== days.length
  const scheduled = sumHours(days.map((day) => day.scheduled))
  const rawPunch = sumHours(days.map((day) => day.rawPunch))
  const recognized = duplicateRecords ? null : sumHours(days.map((day) => day.recognized))
  const paidLeave = duplicateRecords ? null : sumHours(days.map((day) => day.paidLeave))
  const covered = sumHours([recognized, paidLeave])
  const uncovered =
    scheduled !== null && covered !== null ? Math.round((scheduled - covered) * 100) / 100 : null
  return {
    days,
    scheduled,
    rawPunch,
    recognized,
    paidLeave,
    covered,
    uncovered,
    duplicateRecords,
    wageDeduction: uncovered === 0 ? 0 : null,
    wageExplanation:
      uncovered === 0
        ? '本TEST完整覆盖应计，无缺勤扣款；主链保持不变。'
        : '缺记录/单据待批/异常或规则待确认：工资影响未知，不自动归零或扣款。'
  }
}
export const attendanceHourLabel = (value: number | null) =>
  value === null ? '未知（不归零）' : `${value}小时`
export const attendanceMainOvertime = demo.overtimeHours
