-- ============================================================================
-- 光影票务系统 · 种子数据
-- 需先执行 01_schema.sql
-- 说明：所有账号密码明文均为 123456（此处以 SHA-256 存储）。
-- ============================================================================
USE movie_ticket;

-- ----------------------------------------------------------------------------
-- 会员等级目录
-- threshold_fen = 累计消费晋升门槛（分）：已支付订单金额合计达标即自动升档，
-- 与 MembershipLevel.thresholdFen() 保持一致。sort_order 即升级顺序。
-- ----------------------------------------------------------------------------
INSERT INTO membership_level (code, label, discount_basis_points, threshold_fen, sort_order) VALUES
('NORMAL',   '普通会员', 1000, 0,      0),
('SILVER',   '银卡会员', 950,  20000,  1),
('GOLD',     '金卡会员', 880,  80000,  2),
('PLATINUM', '铂金会员', 840,  200000, 3),
('DIAMOND',  '钻石会员', 800,  500000, 4);

-- ----------------------------------------------------------------------------
-- 账号（密码统一为 123456 的 SHA-256 摘要）
-- ----------------------------------------------------------------------------
INSERT INTO account (id, username, password_hash, display_name, phone, role, enabled, membership_code) VALUES
('A-admin', 'admin',    SHA2('123456', 256), '管理员',   '13800000001', 'ADMIN',    1, 'NORMAL'),
('U-001',   'customer', SHA2('123456', 256), '演示用户', '13800138000', 'CUSTOMER', 1, 'SILVER'),
('U-002',   'chen',     SHA2('123456', 256), '陈一诺',   '13911110001', 'CUSTOMER', 1, 'GOLD'),
('U-003',   'lin',      SHA2('123456', 256), '林远',     '13911110002', 'CUSTOMER', 1, 'NORMAL'),
('U-004',   'zhou',     SHA2('123456', 256), '周晴',     '13911110003', 'CUSTOMER', 1, 'SILVER'),
('U-005',   'xu',       SHA2('123456', 256), '徐舟',     '13911110004', 'CUSTOMER', 0, 'NORMAL');

-- ----------------------------------------------------------------------------
-- 电影（poster_path 指向 resources/posters/ 下的海报图片）
-- ----------------------------------------------------------------------------
INSERT INTO movie
    (id, title, genre, director, starring, duration_minutes, format, release_date, synopsis, rating, poster_path, poster_palette)
VALUES
('M001', '泰坦尼克号', '爱情', '詹姆斯·卡梅隆', '莱昂纳多·迪卡普里奥、凯特·温斯莱特', 194, '2D',   '1997-12-19',
 '1912年，穷画家杰克与富家少女罗丝在泰坦尼克号上坠入爱河，巨轮撞上冰山沉没，留下了一段刻骨铭心的爱情绝唱。', 9.5, 'posters/M001.jpg', 0),
('M002', '阿凡达',     '科幻', '詹姆斯·卡梅隆', '萨姆·沃辛顿、佐伊·索尔达娜',           162, 'IMAX', '2009-12-18',
 '前海军陆战队员杰克前往潘多拉星球，操控“阿凡达”分身融入纳美族，在人类与纳美人的冲突中重新认识生命的意义。', 8.8, 'posters/M002.jpg', 1),
('M003', '盗梦空间',   '悬疑', '克里斯托弗·诺兰', '莱昂纳多·迪卡普里奥、玛丽昂·歌迪亚', 148, '2D',   '2010-07-16',
 '造梦师柯布带领团队潜入他人梦境窃取机密，层层嵌套的梦境让现实与虚幻的边界逐渐模糊。', 9.4, 'posters/M003.jpg', 2),
('M004', '肖申克的救赎', '剧情', '弗兰克·德拉邦特', '蒂姆·罗宾斯、摩根·弗里曼',         142, '2D',   '1994-09-23',
 '银行家安迪被冤判入狱，在肖申克监狱中用希望与智慧救赎自己，也照亮了狱中好友瑞德的人生。', 9.7, 'posters/M004.jpg', 3),
('M005', '千与千寻',   '动画', '宫崎骏',       '配音：柊瑠美、入野自由',                 125, '2D',   '2001-07-20',
 '少女千寻随父母误入神灵世界，父母因贪吃变成猪，她在汤屋打工并帮助白龙找回名字，最终救回父母回到人间。', 9.4, 'posters/M005.jpg', 4),
('M006', '大话西游之大圣娶亲', '喜剧', '刘镇伟', '周星驰、朱茵',                         95,  '2D',   '1995-02-04',
 '至尊宝为救白晶晶穿越时空，却与紫霞仙子相遇相爱，“爱你一万年”的告白成为一代人的经典记忆。', 9.2, 'posters/M006.jpg', 5);

-- ----------------------------------------------------------------------------
-- 影厅
-- ----------------------------------------------------------------------------
INSERT INTO cinema_hall (id, name, specs, row_count, col_count) VALUES
('H01', '1号杜比厅', '杜比全景声', 8, 12),
('H02', '2号激光厅', '激光放映',   6, 10),
('H03', '3号IMAX厅', 'IMAX',       9, 14);

-- ----------------------------------------------------------------------------
-- 座位：按每个影厅的行列数自动生成（A1、A2、…）
-- ----------------------------------------------------------------------------
INSERT INTO seat (hall_id, seat_row, seat_col, seat_code, seat_type)
WITH RECURSIVE nums (n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM nums WHERE n < 14
)
SELECT h.id,
       CHAR(65 + a.n),
       b.n + 1,
       CONCAT(CHAR(65 + a.n), b.n + 1),
       'NORMAL'
FROM cinema_hall h
         JOIN nums a ON a.n < h.row_count
         JOIN nums b ON b.n < h.col_count;

-- ----------------------------------------------------------------------------
-- 场次（日期基于 2026-09-09；结束时间 = 开始 + 片长）
-- ----------------------------------------------------------------------------
INSERT INTO show_session (id, movie_id, hall_id, start_time, end_time, price_fen) VALUES
('S001', 'M001', 'H01', '2026-09-10 14:30', '2026-09-10 16:28', 4600),
('S002', 'M001', 'H02', '2026-09-10 20:10', '2026-09-10 22:08', 4200),
('S003', 'M002', 'H03', '2026-09-10 15:00', '2026-09-10 17:06', 6200),
('S004', 'M003', 'H02', '2026-09-11 11:20', '2026-09-11 13:04', 3900),
('S005', 'M004', 'H01', '2026-09-11 18:45', '2026-09-11 20:37', 4500),
('S006', 'M004', 'H03', '2026-09-12 19:30', '2026-09-12 21:22', 5800),
('S007', 'M005', 'H02', '2026-09-12 16:00', '2026-09-12 17:35', 4100),
('S008', 'M005', 'H03', '2026-09-13 10:40', '2026-09-13 12:15', 5300),
('S009', 'M006', 'H01', '2026-09-14 14:00', '2026-09-14 15:28', 4800);

-- ----------------------------------------------------------------------------
-- 演示订单（让座位图、票房、订单查询具备真实数据）
-- 折扣：SILVER=950 基点，GOLD=880 基点，NORMAL=1000（无折扣）
-- ----------------------------------------------------------------------------

-- 订单 T000001：customer(银卡) 购「深海回声」S001 两个座位 A3、A4，已支付
INSERT INTO ticket_order
    (id, user_id, username, original_fen, discount_fen, payable_fen, membership_label, status, ticket_code, created_at)
VALUES
('T000001', 'U-001', 'customer', 9200, 460, 8740, '银卡会员', 'PAID', 'TKT-T000001-4821', '2026-09-09 10:12:00');

INSERT INTO ticket_order_item
    (id, order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_code)
VALUES
('OI001', 'T000001', 'S001', '深海回声', '1号杜比厅', '2026-09-10 14:30', '2026-09-10 16:28', 4600, 'A3'),
('OI002', 'T000001', 'S001', '深海回声', '1号杜比厅', '2026-09-10 14:30', '2026-09-10 16:28', 4600, 'A4');

-- 订单 T000002：chen(金卡) 购「星港24小时」S003 两个座位 B5、B6，已支付
INSERT INTO ticket_order
    (id, user_id, username, original_fen, discount_fen, payable_fen, membership_label, status, ticket_code, created_at)
VALUES
('T000002', 'U-002', 'chen', 12400, 1488, 10912, '金卡会员', 'PAID', 'TKT-T000002-9036', '2026-09-09 11:30:00');

INSERT INTO ticket_order_item
    (id, order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_code)
VALUES
('OI003', 'T000002', 'S003', '星港24小时', '3号IMAX厅', '2026-09-10 15:00', '2026-09-10 17:06', 6200, 'B5'),
('OI004', 'T000002', 'S003', '星港24小时', '3号IMAX厅', '2026-09-10 15:00', '2026-09-10 17:06', 6200, 'B6');

-- 订单 T000003：zhou(银卡) 购「长安小厨」S004 一个座位 C2，待支付
INSERT INTO ticket_order
    (id, user_id, username, original_fen, discount_fen, payable_fen, membership_label, status, ticket_code, created_at)
VALUES
('T000003', 'U-004', 'zhou', 3900, 195, 3705, '银卡会员', 'UNPAID', NULL, '2026-09-09 13:05:00');

INSERT INTO ticket_order_item
    (id, order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_code)
VALUES
('OI005', 'T000003', 'S004', '长安小厨', '2号激光厅', '2026-09-11 11:20', '2026-09-11 13:04', 3900, 'C2');

-- 订单 T000004：lin(普通) 购「旧城追光」S005 一个座位 D8，已支付
INSERT INTO ticket_order
    (id, user_id, username, original_fen, discount_fen, payable_fen, membership_label, status, ticket_code, created_at)
VALUES
('T000004', 'U-003', 'lin', 4500, 0, 4500, '普通会员', 'PAID', 'TKT-T000004-1174', '2026-09-09 15:42:00');

INSERT INTO ticket_order_item
    (id, order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_code)
VALUES
('OI006', 'T000004', 'S005', '旧城追光', '1号杜比厅', '2026-09-11 18:45', '2026-09-11 20:37', 4500, 'D8');
