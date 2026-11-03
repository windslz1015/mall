package com.mall.reliability.domain.schedule.model;

/** 定时任务执行状态；与 reliability_job_execution.status 对齐。 */
public enum JobExecutionStatus {
    QUEUED(0),
    RUNNING(10),
    SUCCEEDED(20),
    FAILED(30),
    TIMED_OUT(40),
    SKIPPED(50);

    private final int code;
    JobExecutionStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
