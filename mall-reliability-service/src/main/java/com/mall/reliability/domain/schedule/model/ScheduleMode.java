package com.mall.reliability.domain.schedule.model;

/** 定时任务计划方式；与 reliability_job_definition.schedule_mode 对齐。 */
public enum ScheduleMode {
    CRON(1),
    FIXED_DELAY(2);

    private final int code;
    ScheduleMode(int code) { this.code = code; }
    public int getCode() { return code; }
}
