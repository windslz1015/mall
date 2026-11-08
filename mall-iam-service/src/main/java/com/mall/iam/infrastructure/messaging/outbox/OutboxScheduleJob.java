
package com.mall.iam.infrastructure.messaging.outbox;

import com.mall.iam.domain.outbox.repository.OutboxEventRepository;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxScheduleJob {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxScheduleJob.class);

    // 每次最多扫描100条
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository repository;

    private final OutboxDispatchService dispatcher;

    public OutboxScheduleJob(
            OutboxEventRepository repository,
            OutboxDispatchService dispatcher) {

        this.repository = repository;
        this.dispatcher = dispatcher;
    }

    /**
     * IAM Outbox 定时补偿任务。
     *
     * 由 XXL-JOB Admin 定时触发。
     */
    @XxlJob("iamOutboxDispatchJobHandler")
    public void scanAndDispatch() {
        log.info("IAM Outbox 定时补偿任务开始");
        XxlJobHelper.log("IAM Outbox 定时补偿任务开始");
        try {

            List<Long> ids = repository.findDueIds(BATCH_SIZE);// 1扫描数据库，查询最多100条符合条件的事件
            if (ids.isEmpty()) {  //2没有需要补偿的事件
                log.info("Outbox 暂无待补偿事件");
                XxlJobHelper.log("Outbox 暂无待补偿事件");
                return;
            }
            log.info("Outbox 本次扫描到 {} 条待补偿事件", ids.size());
            XxlJobHelper.log("本次扫描到 " + ids.size() + " 条待补偿事件");

            int submitted = 0;
            int rejected = 0;


            for (Long id : ids) {// 3逐条提交到 OutboxDispatchService 线程池
                boolean accepted = dispatcher.submitIfAbsent(id);
                if (accepted) {submitted++;} else {rejected++;}
            }

            // 4输出任务执行统计
            log.info("Outbox 补偿任务提交结束，扫描数量={}，已提交或正在执行={}，线程池拒绝={}", ids.size(), submitted, rejected);
            XxlJobHelper.log("扫描数量：" + ids.size() + "，已提交或正在执行：" + submitted + "，线程池拒绝：" + rejected
            );

            if (rejected > 0) {
                XxlJobHelper.log("部分事件未能提交线程池，将由下一轮扫描继续补偿");
            }

        } catch (RuntimeException e) {
            log.error("IAM Outbox 定时补偿扫描失败", e);
            XxlJobHelper.log("IAM Outbox 定时补偿扫描失败：" + e.getMessage());
            // 让 XXL-JOB 记录本次扫描任务失败
            throw e;
        }

        log.info("IAM Outbox 定时补偿任务结束");
        XxlJobHelper.log("IAM Outbox 定时补偿任务结束");
    }
}
