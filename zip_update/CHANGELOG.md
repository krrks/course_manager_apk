#!build
# UI: 时长 chips 与时分输入合并为一行

将 DurationChipsCompact 中原来竖排的两行（chips 行 + 时/分输入行）
合并为单行 Row，同时减小 chip 高度（28dp）和输入框宽度（52→48dp）
以节约空间。AddAttendanceFromScheduleDialog 和 AttendanceFormDialog 同步生效。
