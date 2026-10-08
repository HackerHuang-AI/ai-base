-- 人工执行：初始化个人账户 13780527645。
-- 请勿放入 db/migration，避免应用启动时自动创建该账户。

START TRANSACTION;

INSERT INTO `base_user` (
    `id`,
    `user_id`,
    `mobile`,
    `username`,
    `password_hash`,
    `name`,
    `status`,
    `valid`,
    `version`
) VALUES (
    2104843632644194305,
    'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3',
    '13780527645',
    '13780527645',
    '$2a$10$0hwR/vKOCZDrL9aouGFRm.gC1GzvMAUZSvBF6lpBpFwpseFGffsOy',
    NULL,
    1,
    1,
    0
);

INSERT INTO `user_identity` (
    `id`,
    `user_id`,
    `identity_type`,
    `identity_provider`,
    `identity_value`,
    `verified_at`,
    `valid`,
    `version`
) VALUES (
    2104843632644194306,
    'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3',
    'ACCOUNT',
    'LOCAL',
    '13780527645',
    NOW(),
    1,
    0
);

INSERT INTO `tenant` (
    `id`,
    `tenant_id`,
    `tenant_name`,
    `tenant_type`,
    `credit_code`,
    `status`,
    `valid`,
    `version`
) VALUES (
    2104843632644194307,
    'ten_01a0ec2d-6e59-703e-8ef9-c56de7fc5b06',
    '13780527645 的个人空间',
    1,
    NULL,
    1,
    1,
    0
);

INSERT INTO `tenant_user` (
    `id`,
    `tenant_id`,
    `user_id`,
    `status`,
    `joined_at`,
    `creator_user_id`,
    `updater_user_id`,
    `valid`,
    `version`
) VALUES (
    2104843632644194308,
    'ten_01a0ec2d-6e59-703e-8ef9-c56de7fc5b06',
    'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3',
    1,
    NOW(),
    'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3',
    'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3',
    1,
    0
);

COMMIT;

