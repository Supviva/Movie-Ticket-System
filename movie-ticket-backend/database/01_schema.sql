-- ============================================================================
-- 光影票务系统 · 数据库结构（DDL + 视图）
-- 数据库：movie_ticket        引擎：MySQL 8.0    字符集：utf8mb4
--
-- 设计要点
--   1. 本脚本对应后端全部业务实体，含外键、索引、约束与统计视图。
--   2. 金额统一以「分」为单位存储（INT），与 Java 端 priceFen / Money 类一致，
--      全程不使用浮点数，避免精度误差。
--   3. 会员等级的 sort_order 必须与 Java 枚举 MembershipLevel 的声明顺序一致，
--      该顺序即升级顺序。
--   4. 时间统一 DATETIME，业务代码按 'yyyy-MM-dd HH:mm' / 'yyyy-MM-dd HH:mm:ss' 格式化。
--
-- 执行方式：mysql -uroot -p < 01_schema.sql
-- ============================================================================

DROP DATABASE IF EXISTS movie_ticket;
CREATE DATABASE movie_ticket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE movie_ticket;

-- ----------------------------------------------------------------------------
-- 1. 会员等级目录表
--    对应枚举 MembershipLevel：NORMAL / SILVER / GOLD / PLATINUM / DIAMOND
--    threshold_fen 为「累计消费晋升门槛」：顾客已支付订单金额合计达到该值时自动升档
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS membership_level;
CREATE TABLE membership_level (
    level_code      VARCHAR(16)  NOT NULL                COMMENT '等级编码，与 Java 枚举名一致',
    level_label     VARCHAR(32)  NOT NULL                COMMENT '中文名称，如「银卡会员」',
    sort_order      INT          NOT NULL                COMMENT '排序，同时是升级顺序，从 0 起',
    discount_bp     INT          NOT NULL                COMMENT '折扣基点，1000=原价，950=95折',
    discount_text   VARCHAR(16)  NOT NULL                COMMENT '折扣文案，如「95折」',
    threshold_fen   INT          NOT NULL                COMMENT '晋升门槛（分），累消达标即升档',
    PRIMARY KEY (level_code),
    UNIQUE KEY uk_membership_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级目录';

-- ----------------------------------------------------------------------------
-- 2. 账号表
--    对应模型 Account + Role；同时承担登录、顾客管理、会员等级关联
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS account;
CREATE TABLE account (
    id              VARCHAR(16)  NOT NULL                COMMENT '账号主键，如 A-admin / U-001',
    username        VARCHAR(20)  NOT NULL                COMMENT '登录名，唯一',
    display_name    VARCHAR(20)  NOT NULL                COMMENT '姓名/昵称',
    phone           VARCHAR(16)  NOT NULL                COMMENT '手机号',
    password_hash   VARCHAR(64)  NOT NULL                COMMENT '口令摘要（SHA-256，不存明文）',
    role            VARCHAR(16)  NOT NULL                COMMENT '角色：CUSTOMER / ADMIN',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用，0=禁用',
    membership_code VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '会员等级编码',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_username (username),
    KEY idx_account_role (role),
    CONSTRAINT fk_account_membership FOREIGN KEY (membership_code)
        REFERENCES membership_level (level_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号';

-- ----------------------------------------------------------------------------
-- 3. 电影表
--    对应模型 Movie；poster_path 指向海报图片，poster_palette 为配色兜底
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS movie;
CREATE TABLE movie (
    id              VARCHAR(16)  NOT NULL                COMMENT '影片编号，如 M001',
    title           VARCHAR(64)  NOT NULL                COMMENT '片名',
    genre           VARCHAR(32)  NOT NULL                COMMENT '类型，如 科幻',
    director        VARCHAR(64)  NOT NULL                COMMENT '导演',
    starring        VARCHAR(128) NOT NULL                COMMENT '主演',
    duration_min    INT          NOT NULL                COMMENT '时长（分钟）',
    format          VARCHAR(16)  NOT NULL                COMMENT '制式：2D / 3D / IMAX',
    release_date    DATE         NULL                    COMMENT '上映日期',
    synopsis        VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '剧情简介',
    rating          DECIMAL(3,1) NOT NULL DEFAULT 0.0    COMMENT '评分 0.0 - 10.0',
    poster_palette  INT          NOT NULL DEFAULT 0      COMMENT '海报配色索引（0-5）',
    poster_path     VARCHAR(255) NULL                    COMMENT '海报文件路径',
    PRIMARY KEY (id),
    KEY idx_movie_title (title),
    KEY idx_movie_genre (genre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电影';

-- ----------------------------------------------------------------------------
-- 4. 影厅表
--    对应模型 CinemaHall
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS cinema_hall;
CREATE TABLE cinema_hall (
    id              VARCHAR(16)  NOT NULL                COMMENT '影厅编号，如 H01',
    name            VARCHAR(32)  NOT NULL                COMMENT '影厅名称',
    specs           VARCHAR(64)  NOT NULL                COMMENT '规格，如 杜比全景声',
    row_count       INT          NOT NULL                COMMENT '座位行数',
    col_count       INT          NOT NULL                COMMENT '座位列数',
    PRIMARY KEY (id),
    CONSTRAINT ck_hall_rows CHECK (row_count > 0),
    CONSTRAINT ck_hall_cols CHECK (col_count > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影厅';

-- ----------------------------------------------------------------------------
-- 5. 座位表
--    把「行列网格计算」显式化为独立实体，便于支持座位类型、维护等扩展
--    seat_key 形如 A1 / B12，与业务层字符串座位号一致
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS seat;
CREATE TABLE seat (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    hall_id         VARCHAR(16)  NOT NULL                COMMENT '所属影厅',
    seat_key        VARCHAR(8)   NOT NULL                COMMENT '座位号，如 A1',
    row_index       INT          NOT NULL                COMMENT '行下标，从 0 起',
    col_index       INT          NOT NULL                COMMENT '列下标，从 0 起',
    seat_type       VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '座位类型：NORMAL/VIP/COUPLE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seat_hall_key (hall_id, seat_key),
    CONSTRAINT fk_seat_hall FOREIGN KEY (hall_id)
        REFERENCES cinema_hall (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位';

-- ----------------------------------------------------------------------------
-- 6. 场次表
--    对应模型 ShowSession；时间用 DATETIME，票价用分
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS show_session;
CREATE TABLE show_session (
    id              VARCHAR(16)  NOT NULL                COMMENT '场次编号，如 S001',
    movie_id        VARCHAR(16)  NOT NULL                COMMENT '影片编号',
    hall_id         VARCHAR(16)  NOT NULL                COMMENT '影厅编号',
    start_time      DATETIME     NOT NULL                COMMENT '开映时间',
    end_time        DATETIME     NOT NULL                COMMENT '散场时间',
    price_fen       INT          NOT NULL                COMMENT '票价（分）',
    PRIMARY KEY (id),
    KEY idx_session_movie (movie_id),
    KEY idx_session_hall (hall_id),
    KEY idx_session_start (start_time),
    CONSTRAINT fk_session_movie FOREIGN KEY (movie_id) REFERENCES movie (id),
    CONSTRAINT fk_session_hall  FOREIGN KEY (hall_id)  REFERENCES cinema_hall (id),
    CONSTRAINT ck_session_price CHECK (price_fen >= 0),
    CONSTRAINT ck_session_time  CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次';

-- ----------------------------------------------------------------------------
-- 7. 购物车表
--    对应模型 CartItem，按「用户 × 场次 × 座位」粒度一行
--    同一用户同一场次的多个座位即多行，聚合后即为一个 CartItem
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS cart;
CREATE TABLE cart (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    account_id      VARCHAR(16)  NOT NULL                COMMENT '所属账号',
    session_id      VARCHAR(16)  NOT NULL                COMMENT '场次编号',
    seat_key        VARCHAR(8)   NOT NULL                COMMENT '座位号',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_session_seat (account_id, session_id, seat_key),
    KEY idx_cart_account (account_id),
    CONSTRAINT fk_cart_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_session FOREIGN KEY (session_id) REFERENCES show_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- ----------------------------------------------------------------------------
-- 8. 订单表
--    对应模型 TicketOrder；冗余用户名与会员标签作为下单快照
--    座位占用规则：UNPAID 与 PAID 均占座，CANCELED 释放座位
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS ticket_order;
CREATE TABLE ticket_order (
    id               VARCHAR(16)  NOT NULL               COMMENT '订单号，如 T001001',
    account_id       VARCHAR(16)  NOT NULL               COMMENT '下单账号',
    username         VARCHAR(20)  NOT NULL               COMMENT '用户名快照',
    original_fen     INT          NOT NULL               COMMENT '原价合计（分）',
    discount_fen     INT          NOT NULL               COMMENT '优惠金额（分）',
    payable_fen      INT          NOT NULL               COMMENT '应付金额（分）',
    membership_label VARCHAR(32)  NOT NULL               COMMENT '下单时会员标签快照',
    status           VARCHAR(16)  NOT NULL               COMMENT 'UNPAID / PAID / CANCELED',
    ticket_code      VARCHAR(32)  NULL                   COMMENT '出票码，支付后生成',
    created_at       DATETIME     NOT NULL               COMMENT '下单时间',
    paid_at          DATETIME     NULL                   COMMENT '支付时间',
    PRIMARY KEY (id),
    KEY idx_order_account (account_id),
    KEY idx_order_status (status),
    KEY idx_order_created (created_at),
    CONSTRAINT fk_order_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT ck_order_status CHECK (status IN ('UNPAID','PAID','CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- ----------------------------------------------------------------------------
-- 9. 订单明细表（座位级快照）
--    对应模型 TicketOrderItem；一行 = 订单中一个场次的一个座位
--    冗余片名/影厅/时间是「下单快照」，影片改名不影响历史订单
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS ticket_order_item;
CREATE TABLE ticket_order_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id        VARCHAR(16)  NOT NULL                COMMENT '所属订单',
    session_id      VARCHAR(16)  NOT NULL                COMMENT '场次编号',
    movie_title     VARCHAR(64)  NOT NULL                COMMENT '片名快照',
    hall_name       VARCHAR(32)  NOT NULL                COMMENT '影厅名快照',
    start_time      DATETIME     NOT NULL                COMMENT '开映时间快照',
    end_time        DATETIME     NOT NULL                COMMENT '散场时间快照',
    unit_price_fen  INT          NOT NULL                COMMENT '单价（分）',
    seat_key        VARCHAR(8)   NOT NULL                COMMENT '座位号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_order_seat (order_id, session_id, seat_key),
    KEY idx_item_order (order_id),
    KEY idx_item_session (session_id),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES ticket_order (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细（座位级）';

-- ============================================================================
-- 视图
-- ============================================================================

-- 场次详情：联表电影 + 影厅，供后台排片列表直接查询
DROP VIEW IF EXISTS v_session_detail;
CREATE VIEW v_session_detail AS
SELECT s.id            AS session_id,
       s.start_time,
       s.end_time,
       s.price_fen,
       m.id            AS movie_id,
       m.title         AS movie_title,
       m.genre,
       m.duration_min,
       m.format,
       h.id            AS hall_id,
       h.name          AS hall_name,
       h.row_count,
       h.col_count
FROM show_session s
         JOIN movie m      ON m.id = s.movie_id
         JOIN cinema_hall h ON h.id = s.hall_id;

-- 已占用座位：未支付 + 已支付订单占用的座位（下单即占座，取消后释放）
DROP VIEW IF EXISTS v_sold_seats;
CREATE VIEW v_sold_seats AS
SELECT i.session_id,
       i.seat_key,
       o.id     AS order_id,
       o.status
FROM ticket_order_item i
         JOIN ticket_order o ON o.id = i.order_id
WHERE o.status IN ('UNPAID', 'PAID');

-- 票房统计：按影片汇总「已支付」订单的售票张数与票房（分）
DROP VIEW IF EXISTS v_movie_boxoffice;
CREATE VIEW v_movie_boxoffice AS
SELECT m.id                                AS movie_id,
       m.title                             AS movie_title,
       COUNT(i.id)                         AS ticket_count,
       COALESCE(SUM(i.unit_price_fen), 0)  AS gross_fen
FROM movie m
         LEFT JOIN show_session s       ON s.movie_id = m.id
         LEFT JOIN ticket_order_item i  ON i.session_id = s.id
         LEFT JOIN ticket_order o       ON o.id = i.order_id AND o.status = 'PAID'
WHERE o.id IS NOT NULL
GROUP BY m.id, m.title;

-- 订单汇总：每个订单的明细条数、座位数与金额
DROP VIEW IF EXISTS v_order_summary;
CREATE VIEW v_order_summary AS
SELECT o.id,
       o.username,
       o.status,
       o.original_fen,
       o.discount_fen,
       o.payable_fen,
       COUNT(i.id) AS seat_count,
       o.created_at
FROM ticket_order o
         LEFT JOIN ticket_order_item i ON i.order_id = o.id
GROUP BY o.id, o.username, o.status, o.original_fen, o.discount_fen, o.payable_fen, o.created_at;
