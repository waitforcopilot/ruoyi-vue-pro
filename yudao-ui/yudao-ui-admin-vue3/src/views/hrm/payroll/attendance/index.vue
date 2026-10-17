<template>
  <ContentWrap><RequirementContext module="ATT" /></ContentWrap>
  <AttendanceIssues />
</template>
<script setup lang="ts">
import RequirementContext from '../components/RequirementContext.vue'
import AttendanceIssues from './AttendanceIssues.vue'
defineOptions({ name: 'HrmPayrollAttendance' })
</script>
