-- =============================================================
-- 清理 API 集成测试残留数据
-- 数据库：eureka_contact（联系人服务）
-- 
-- 说明：
-- API 集成测试（UserContactApiTest / MerchantContactApiTest）
-- 未使用 @Transactional（因为 SpringBootTest RANDOM_PORT 模式下
-- @Transactional 不生效），因此测试运行后会留下持久数据。
-- 本脚本用于安全清理这些测试数据。
--
-- 清理范围：
--   t_contact        - 用户联系人              ~130 条
--   user_contact     - 用户-联系人关联           ~130 条
--   shop_address     - 商家地址                ~46 条
--   shop_address_rel - 商家-地址关联            ~46 条
--
-- 识别策略：
--   1. 用户联系人：通过已知测试 userId（1001～8001）及名称模式
--   2. 商家地址：通过 shop_id LIKE 'SHP-%' 模式
-- =============================================================

USE eureka_contact;

-- =============================================================
-- 1. 清理用户联系人数据
-- =============================================================
-- 1a. 删除用户-联系人关联（已知测试用户ID）
DELETE FROM user_contact
WHERE user_id IN ('1001','2001','3001','4001','5001','6001','7001','8001','99999');

-- 1b. 删除剩余无关联的联系人（测试中创建但被删除关联的遗留数据）
DELETE FROM t_contact
WHERE id NOT IN (SELECT DISTINCT contact_id FROM user_contact);

-- =============================================================
-- 2. 清理商家地址数据
-- =============================================================
-- 2a. 删除商家-地址关联（API 测试使用的 shop_id 模式：SHP- 前缀 + 1001）
DELETE FROM shop_address_rel
WHERE shop_id LIKE 'SHP-%' OR shop_id = '1001';

-- 2b. 删除无关联的商家地址
DELETE FROM shop_address
WHERE id NOT IN (SELECT DISTINCT address_id FROM shop_address_rel);

-- =============================================================
-- 完成提示
-- =============================================================
SELECT CONCAT(
    '清理完成，剩余 t_contact: ', (SELECT COUNT(*) FROM t_contact),
    ', user_contact: ', (SELECT COUNT(*) FROM user_contact),
    ', shop_address: ', (SELECT COUNT(*) FROM shop_address),
    ', shop_address_rel: ', (SELECT COUNT(*) FROM shop_address_rel)
) AS message;
