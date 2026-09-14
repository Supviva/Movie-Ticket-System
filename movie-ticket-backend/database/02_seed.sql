-- ============================================================================
-- 光影票务系统 · 初始化数据（DML）
--
-- 内容：会员等级目录、演示账号、影片、影厅及其座位、场次
-- 说明：与后端 Java 演示数据保持一致，重新执行本脚本即可恢复初始状态。
-- 执行：mysql -uroot -p movie_ticket < 02_seed.sql
-- ============================================================================

USE movie_ticket;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE ticket_order_item;
TRUNCATE TABLE ticket_order;
TRUNCATE TABLE cart;
TRUNCATE TABLE show_session;
TRUNCATE TABLE seat;
TRUNCATE TABLE cinema_hall;
TRUNCATE TABLE movie;
TRUNCATE TABLE account;
TRUNCATE TABLE membership_level;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. 会员等级目录（sort_order 即升级顺序，与 Java 枚举声明顺序一致）
-- ----------------------------------------------------------------------------
INSERT INTO membership_level (level_code, level_label, sort_order, discount_bp, discount_text, threshold_fen) VALUES
('NORMAL',   '普通会员', 0, 1000, '无折扣',      0),
('SILVER',   '银卡会员', 1,  950, '95折',    20000),
('GOLD',     '金卡会员', 2,  880, '88折',    80000),
('PLATINUM', '铂金会员', 3,  840, '84折',   200000),
('DIAMOND',  '钻石会员', 4,  800, '8折',     500000);

-- ----------------------------------------------------------------------------
-- 2. 账号
--    password_hash 为 SHA-256 摘要，原文均为 123456：
--      SHA-256('123456') = 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
-- ----------------------------------------------------------------------------
INSERT INTO account (id, username, display_name, phone, password_hash, role, enabled, membership_code) VALUES
('A-admin', 'admin',    '管理员',  '13800000001', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ADMIN',    1, 'NORMAL'),
('U-001',   'customer', '演示用户', '13800138000', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'CUSTOMER', 1, 'SILVER'),
('U-002',   'chen',     '陈一诺',  '13911110001', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'CUSTOMER', 1, 'GOLD'),
('U-003',   'lin',      '林远',    '13911110002', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'CUSTOMER', 1, 'NORMAL'),
('U-004',   'zhou',     '周晴',    '13911110003', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'CUSTOMER', 1, 'SILVER'),
('U-005',   'xu',       '徐舟',    '13911110004', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'CUSTOMER', 0, 'NORMAL');

-- ----------------------------------------------------------------------------
-- 3. 影片
-- ----------------------------------------------------------------------------
INSERT INTO movie (id, title, genre, director, starring, duration_min, format, release_date, synopsis, rating, poster_palette, poster_path) VALUES
('M001', '泰坦尼克号', '爱情', '詹姆斯·卡梅隆', '莱昂纳多·迪卡普里奥、凯特·温斯莱特', 194, '2D', '1997-12-19', '1912年，穷画家杰克与富家少女罗丝在泰坦尼克号上坠入爱河，巨轮撞上冰山沉没，留下了一段刻骨铭心的爱情绝唱。', 9.5, 0, 'posters/M001.jpg'),
('M002', '阿凡达', '科幻', '詹姆斯·卡梅隆', '萨姆·沃辛顿、佐伊·索尔达娜', 162, 'IMAX', '2009-12-18', '前海军陆战队员杰克前往潘多拉星球，操控“阿凡达”分身融入纳美族，在人类与纳美人的冲突中重新认识生命的意义。', 8.8, 1, 'posters/M002.jpg'),
('M003', '盗梦空间', '悬疑', '克里斯托弗·诺兰', '莱昂纳多·迪卡普里奥、玛丽昂·歌迪亚', 148, '2D', '2010-07-16', '造梦师柯布带领团队潜入他人梦境窃取机密，层层嵌套的梦境让现实与虚幻的边界逐渐模糊。', 9.4, 2, 'posters/M003.jpg'),
('M004', '肖申克的救赎', '剧情', '弗兰克·德拉邦特', '蒂姆·罗宾斯、摩根·弗里曼', 142, '2D', '1994-09-23', '银行家安迪被冤判入狱，在肖申克监狱中用希望与智慧救赎自己，也照亮了狱中好友瑞德的人生。', 9.7, 3, 'posters/M004.jpg'),
('M005', '千与千寻', '动画', '宫崎骏', '配音：柊瑠美、入野自由', 125, '2D', '2001-07-20', '少女千寻随父母误入神灵世界，父母因贪吃变成猪，她在汤屋打工并帮助白龙找回名字，最终救回父母回到人间。', 9.4, 4, 'posters/M005.jpg'),
('M006', '大话西游之大圣娶亲', '喜剧', '刘镇伟', '周星驰、朱茵', 95, '2D', '1995-02-04', '至尊宝为救白晶晶穿越时空，却与紫霞仙子相遇相爱，“爱你一万年”的告白成为一代人的经典记忆。', 9.2, 5, 'posters/M006.jpg'),
('M009', '夏日蝉鸣', '青春', '林夏', '唐果、苏屿', 101, '2D', '2026-09-24', '毕业前的最后一个夏天，五个少年用一场公路旅行告别青春，也重新找到各自的方向。', 8.4, 2, 'posters/M009.png'),
('M019', '复仇者联盟', '动作', '乔斯·韦登', '小罗伯特·唐尼、克里斯·埃文斯', 143, 'IMAX', '2012-05-05', '邪神洛基携外星大军入侵地球，六位超级英雄首次集结，为守护人类而并肩作战。', 9.3, 0, 'posters/M019.png'),
('M020', '复仇者联盟2：奥创纪元', '动作', '乔斯·韦登', '小罗伯特·唐尼、克里斯·海姆斯沃斯', 141, 'IMAX', '2015-05-12', '托尼·斯塔克启动的维和计划失控，诞生了人工智能奥创，复仇者联盟必须再度集结阻止人类灭绝。', 8.9, 1, 'posters/M020.png'),
('M021', '复仇者联盟3：无限战争', '动作', '安东尼·罗素', '小罗伯特·唐尼、克里斯·海姆斯沃斯', 149, 'IMAX', '2018-05-11', '灭霸为集齐六颗无限宝石席卷宇宙，复仇者联盟倾尽全力仍未阻止那一声响指。', 9.1, 2, 'posters/M021.png'),
('M022', '复仇者联盟4：终局之战', '动作', '安东尼·罗素', '小罗伯特·唐尼、克里斯·埃文斯', 181, 'IMAX', '2019-04-24', '幸存的复仇者穿越时间线夺回无限宝石，为逝去的战友与半个宇宙发起最后一战。', 9.4, 3, 'posters/M022.png'),
('M027', '星际穿越', '科幻', '克里斯托弗·诺兰', '马修·麦康纳、安妮·海瑟薇', 169, 'IMAX', '2014-11-12', '地球濒临枯竭，宇航员穿越虫洞为人类寻找新家园，而时间在相对论中悄然流逝。', 9.2, 2, 'posters/M027.png'),
('M029', '复仇者联盟5：康之王朝', '动作', '德斯汀·克里顿', '小罗伯特·唐尼、安东尼·麦凯', 156, 'IMAX', '2027-05-07', '漫威电影宇宙新纪元开启，多元宇宙的裂缝彻底撕裂，复仇者联盟必须面对来自时间尽头的征服者康。', 9.6, 4, 'posters/M029.png'),
('M030', '复仇者联盟6：秘密战争', '动作', '安东尼·罗素', '克里斯·海姆斯沃斯、布丽·拉尔森', 172, 'IMAX', '2027-11-12', '平行宇宙正面相撞，所有已知的复仇者被迫在同一战场上集结，为整个多元宇宙的存续而战。', 9.5, 5, 'posters/M030.png'),
('M031', '阿凡达3：火与烬', '科幻', '詹姆斯·卡梅隆', '萨姆·沃辛顿、佐伊·索尔达娜', 195, '3D', '2027-12-17', '潘多拉的海洋深处沉睡着一支灰烬部族，杰克一家必须在火与水的对峙中守护纳美人的未来。', 9.4, 0, 'posters/M031.png'),
('M034', '碟中谍8：最终清算', '动作', '克里斯托弗·麦奎里', '汤姆·克鲁斯、海莉·阿特维尔', 164, 'IMAX', '2027-05-21', '智体即将全面接管全球系统，伊森·亨特必须在最后一役中做出比生命更重的抉择。', 9.3, 3, 'posters/M034.png');

-- ----------------------------------------------------------------------------
-- 4. 影厅
-- ----------------------------------------------------------------------------
INSERT INTO cinema_hall (id, name, specs, row_count, col_count) VALUES
('H01', '1号杜比厅', '杜比全景声', 8, 12),
('H02', '2号激光厅', '激光放映',   6, 10),
('H03', '3号IMAX厅', 'IMAX',       9, 14);

-- ----------------------------------------------------------------------------
-- 5. 座位：按影厅行列自动生成
--    MySQL 8 可用递归 CTE 展开；此处直接列出，保证在任意客户端都能执行。
--    H01: 8行12列 (A-H)，H02: 6行10列 (A-F)，H03: 9行14列 (A-I)
-- ----------------------------------------------------------------------------
INSERT INTO seat (hall_id, seat_key, row_index, col_index, seat_type)
WITH RECURSIVE
nums AS (SELECT 0 AS n UNION ALL SELECT n + 1 FROM nums WHERE n < 13),
letters AS (SELECT 0 AS r, 'A' AS ch UNION ALL SELECT r + 1, CHAR(65 + r + 1) FROM letters WHERE r < 8)
SELECT h.id,
       CONCAT(l.ch, n.n + 1),
       l.r,
       n.n,
       CASE WHEN h.id = 'H03' AND n.n IN (2, 11) THEN 'VIP' ELSE 'NORMAL' END
FROM cinema_hall h
         JOIN letters l ON l.r < h.row_count
         JOIN nums n    ON n.n < h.col_count;

-- ----------------------------------------------------------------------------
-- 6. 场次：为每部影片生成未来几天的排片（明天起 5 天，每天 3 个黄金场）
--    使用递归 CTE 展开日期与时段，避免手工写几十条 INSERT
-- ----------------------------------------------------------------------------
INSERT INTO show_session (id, movie_id, hall_id, start_time, end_time, price_fen)
WITH RECURSIVE
days AS (SELECT 1 AS d UNION ALL SELECT d + 1 FROM days WHERE d < 5),
slots AS (
    SELECT 0 AS s, '14:00:00' AS t, 'H01' AS h
    UNION ALL SELECT 1, '16:30:00', 'H02'
    UNION ALL SELECT 2, '19:30:00', 'H03'
),
movies AS (
    SELECT id, duration_min, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM movie
),
priced AS (
    SELECT m.id, m.duration_min, m.rn,
           CASE m.id
               WHEN 'M001' THEN 4600 WHEN 'M002' THEN 6200 WHEN 'M003' THEN 3900
               WHEN 'M004' THEN 4500 WHEN 'M005' THEN 4100 WHEN 'M006' THEN 4800
               WHEN 'M009' THEN 3800 WHEN 'M019' THEN 5600 WHEN 'M020' THEN 5800
               WHEN 'M021' THEN 6000 WHEN 'M022' THEN 6200 WHEN 'M027' THEN 6800
               WHEN 'M029' THEN 7800 WHEN 'M030' THEN 8200 WHEN 'M031' THEN 8900
               WHEN 'M034' THEN 7500 ELSE 5000
           END AS price_fen
    FROM movies m
),
total AS (SELECT COUNT(*) AS cnt FROM priced)
SELECT CONCAT('S', LPAD(ROW_NUMBER() OVER (ORDER BY d, s), 3, '0')),
       p.id,
       sl.h,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d DAY), sl.t),
       DATE_ADD(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL d DAY), sl.t), INTERVAL p.duration_min MINUTE),
       p.price_fen
FROM days d
         CROSS JOIN slots sl
         JOIN total t
         JOIN priced p ON p.rn = MOD((d.d - 1) + sl.s, t.cnt);

-- ----------------------------------------------------------------------------
-- 7. 历史订单样例：用于演示「会员按累计消费自动升级」与票房视图
--    这两单均为已支付状态，使 customer(U-001) 的累计消费达到银卡门槛
-- ----------------------------------------------------------------------------
INSERT INTO ticket_order (id, account_id, username, original_fen, discount_fen, payable_fen,
                          membership_label, status, ticket_code, created_at, paid_at) VALUES
('T001001', 'U-001', 'customer', 12400, 620, 11780, '银卡会员', 'PAID', 'TKT-T001001-8842', '2026-09-10 19:05:00', '2026-09-10 19:06:12'),
('T001002', 'U-002', 'chen',     9200,  1104, 8096, '金卡会员', 'PAID', 'TKT-T001002-3317', '2026-09-11 20:31:00', '2026-09-11 20:32:40');

-- 明细：T001001 两张泰坦尼克号
INSERT INTO ticket_order_item (order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_key)
SELECT 'T001001', s.id, m.title, h.name, s.start_time, s.end_time, s.price_fen, 'B5'
FROM show_session s JOIN movie m ON m.id = s.movie_id JOIN cinema_hall h ON h.id = s.hall_id
WHERE m.id = 'M001' ORDER BY s.start_time LIMIT 1;

INSERT INTO ticket_order_item (order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_key)
SELECT 'T001001', s.id, m.title, h.name, s.start_time, s.end_time, s.price_fen, 'B6'
FROM show_session s JOIN movie m ON m.id = s.movie_id JOIN cinema_hall h ON h.id = s.hall_id
WHERE m.id = 'M001' ORDER BY s.start_time LIMIT 1;

-- 明细：T001002 两张阿凡达
INSERT INTO ticket_order_item (order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_key)
SELECT 'T001002', s.id, m.title, h.name, s.start_time, s.end_time, s.price_fen, 'C7'
FROM show_session s JOIN movie m ON m.id = s.movie_id JOIN cinema_hall h ON h.id = s.hall_id
WHERE m.id = 'M002' ORDER BY s.start_time LIMIT 1;

INSERT INTO ticket_order_item (order_id, session_id, movie_title, hall_name, start_time, end_time, unit_price_fen, seat_key)
SELECT 'T001002', s.id, m.title, h.name, s.start_time, s.end_time, s.price_fen, 'C8'
FROM show_session s JOIN movie m ON m.id = s.movie_id JOIN cinema_hall h ON h.id = s.hall_id
WHERE m.id = 'M002' ORDER BY s.start_time LIMIT 1;
