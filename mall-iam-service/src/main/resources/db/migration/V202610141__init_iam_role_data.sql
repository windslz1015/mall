INSERT IGNORE INTO iam_role (
    role_code,
    role_name,
    account_type,
    status,
    built_in,
    created_at,  -- 假设表里有这个字段
    updated_at   -- 假设表里有这个字段
) VALUES (
    'NORMAL_USER',
    '普通用户',
    NULL,
    1,
    1,
    NOW(),
    NOW()
);