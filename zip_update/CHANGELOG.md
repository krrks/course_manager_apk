#!build
# Fix batch action dialog: pre-fill from first selected lesson, fix end-time bug

## LessonBatchActionDialog.kt
- On open, derive the first lesson (earliest date+startTime) from selectedIds
- Pre-fill newStart, newEnd, newStatus from that lesson instead of hardcoded defaults
- Fix StartTimeCompact callback to preserve duration when start time changes
  (previously newEnd was not updated, causing each lesson to keep its own original endTime)

## LessonTimeHelpers.kt
- Key DurationChipsCompact's `remember` on `startTime` so the hour/minute
  fields reinitialise correctly when the parent updates startTime
