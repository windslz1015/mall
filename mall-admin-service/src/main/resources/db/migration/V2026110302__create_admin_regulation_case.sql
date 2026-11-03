-- 平台监管案件：案件工作流属于管理服务；商家、商品和用户本身的状态归业务服务。
-- status: 0待处理 10审核中 20待处置 30已解决 40已关闭 50已驳回。
CREATE TABLE admin_regulation_case (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '监管案件主键',
    case_no VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '案件唯一编号',
    case_category VARCHAR(32) NOT NULL COMMENT '商家违规/商品违规/账号风险/订单风险/支付风险等',
    title VARCHAR(200) NOT NULL COMMENT '案件标题（不要包含敏感个人资料）',
    summary VARCHAR(2000) DEFAULT NULL COMMENT '脱敏的举报或发现情况摘要',
    priority TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '1低 2普通 3高 4紧急',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待处理 10审核中 20待处置 30已解决 40已关闭 50已驳回',
    opened_by_account_id BIGINT UNSIGNED NOT NULL COMMENT 'IAM管理员账号ID（逻辑引用）',
    assignee_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'IAM受理管理员账号ID（逻辑引用）',
    conclusion VARCHAR(1000) DEFAULT NULL COMMENT '最终处理结论（脱敏）',
    closed_at DATETIME(3) DEFAULT NULL COMMENT '案件终结时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '后续并发状态变更乐观锁',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_regulation_case_no (case_no),
    KEY idx_admin_regulation_case_status_priority (status, priority, created_at, id),
    KEY idx_admin_regulation_case_category_time (case_category, created_at, id),
    KEY idx_admin_regulation_case_assignee (assignee_account_id, status, created_at),
    KEY idx_admin_regulation_case_opened_by (opened_by_account_id, created_at),
    CONSTRAINT ck_admin_regulation_case_category CHECK
        (case_category IN ('MERCHANT_VIOLATION', 'PRODUCT_VIOLATION', 'ACCOUNT_RISK', 'ORDER_RISK', 'PAYMENT_RISK', 'OTHER')),
    CONSTRAINT ck_admin_regulation_case_priority CHECK (priority IN (1, 2, 3, 4)),
    CONSTRAINT ck_admin_regulation_case_status CHECK (status IN (0, 10, 20, 30, 40, 50)),
    CONSTRAINT ck_admin_regulation_case_closed CHECK
        (status NOT IN (40, 50) OR closed_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='平台监管案件；不直接改变商家/商品/账号订单状态';
