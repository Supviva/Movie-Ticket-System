-- ============================================================================
-- 光影票务系统 · 数据库结构（DDL + 视图）
-- 数据库：movie_ticket
-- 引擎：MySQL 8.0  /  字符集：utf8mb4
-- 说明：本脚本对应 Swing Demo 的全部业务实体，含外键、索引、约束与统计视图。
-- 金额统一以「分」为单位存储（与 Java 端 priceFen / Money 类一致）。
-- ============================================================================

DROP DATABASE IF EXISTS movie_ticket;
CREATE DATABASE movie_ticket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE movie_ticket;

-- ----------------------------------------------------------------------------
-- 1. 会员等级目录表
--    对应枚举 MembershipLevel：NORMAL / SILVER / GOLD / PLATINUM / DIAMOND
--    sort_order 必须与枚举声明顺序（即升级顺序）一致
--    threshold_fen 为「累计消费晋升门槛」：顾客已支付订单金额合计达到该值时自动升档
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS membership_level;
CREATE TABLE membership_level (
    code                  VARCHAR(20)  NOT NULL COMMENT '等级编码：NORMAL/SILVER/GOLD/PLATINUM/DIAMOND',
    label                 VARCHAR(20)  NOT NULL COMMENT '等级名称：普通/银卡/金卡/铂金/钻石会员',
    discount_basis_points INT          NOT NULL COMMENT '折扣基点，1000=无折扣，950=95折，880=88折，840=84折，800=8折',
    threshold_fen         INT          NOT NULL DEFAULT 0 COMMENT '累计消费晋升门槛（分），0 表示注册即得',
    sort_order            INT          NOT NULL DEFAULT 0 COMMENT '展示排序，与枚举声明顺序一致',
    PRIMARY KEY (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '会员等级目录';

-- ----------------------------------------------------------------------------
-- 2. 账号表
--    对应模型 Account + Role；同时承担登录、顾客管理、会员等级关联
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS account;
CREATE TABLE account (
    id              VARCHAR(20)            NOT NULL COMMENT '账号ID：A-<用户名>(管理员) / U-<序号>(顾客)',
    username        VARCHAR(20)            NOT NULL COMMENT '登录用户名，唯一',
    password_hash   CHAR(64)               NOT NULL COMMENT '密码 SHA-256 摘要（十六进制）',
    display_name    VARCHAR(20)            NOT NULL COMMENT '显示姓名',
    phone           VARCHAR(11)            NOT NULL COMMENT '11位手机号',
    role            ENUM ('ADMIN','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER' COMMENT '角色',
    enabled         TINYINT(1)             NOT NULL DEFAULT 1 COMMENT '是否启用（1启用 0禁用）',
    membership_code VARCHAR(20)            NOT NULL DEFAULT 'NORMAL' COMMENT '会员等级编码',
    created_at      DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_username (username),
    KEY idx_account_phone (phone),
    CONSTRAINT fk_account_membership
        FOREIGN KEY (membership_code) REFERENCES membership_level (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '账号（含管理员与顾客）';

-- ----------------------------------------------------------------------------
-- 3. 电影表
--    对应模型 Movie；poster_path 指向海报图片，poster_palette 为配色兜底
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS movie;
CREATE TABLE movie (
    id              VARCHAR(20)  NOT NULL COMMENT '影片编号，如 M001',
    title           VARCHAR(100) NOT NULL COMMENT '片名',
    genre           VARCHAR(50)  NOT NULL COMMENT '类型：剧情/科幻/喜剧/悬疑/动画/纪录',
    director        VARCHAR(50)  NOT NULL COMMENT '导演',
    starring        VARCHAR(200) NOT NULL COMMENT '主演/配音',
    duration_minutes INT         NOT NULL COMMENT '时长（分钟）',
    format          VARCHAR(20)  NOT NULL COMMENT '放映格式：2D/3D/IMAX',
    release_date    DATE         NOT NULL COMMENT '上映日期',
    synopsis        TEXT         NULL COMMENT '剧情简介',
    rating          DECIMAL(2,1) NOT NULL DEFAULT 0.0 COMMENT '评分 0.0~10.0',
    poster_path     VARCHAR(255) NULL COMMENT '海报图片相对路径，如 posters/M001.jpg',
    poster_palette  INT          NOT NULL DEFAULT 0 COMMENT '海报配色索引（无图时兜底）',
    PRIMARY KEY (id),
    KEY idx_movie_title (title)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '电影';

-- ----------------------------------------------------------------------------
-- 4. 影厅表
--    对应模型 CinemaHall
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS cinema_hall;
CREATE TABLE cinema_hall (
    id        VARCHAR(20) NOT NULL COMMENT '影厅编号，如 H01',
    name      VARCHAR(50) NOT NULL COMMENT '影厅名称',
    specs     VARCHAR(50) NULL COMMENT '规格说明：杜比全景声/激光放映/IMAX',
    row_count INT         NOT NULL COMMENT '座位行数（A 起）',
    col_count INT         NOT NULL COMMENT '每行座位列数',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '影厅';

-- ----------------------------------------------------------------------------
-- 5. 座位表
--    将原「行列网格计算」显式化为独立实体，便于支持座位类型、维护等扩展
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS seat;
CREATE TABLE seat (
    hall_id   VARCHAR(20) NOT NULL COMMENT '所属影厅',
    seat_row  CHAR(1)     NOT NULL COMMENT '行号 A~Z',
    seat_col  INT         NOT NULL COMMENT '列号 1~N',
    seat_code VARCHAR(10) NOT NULL COMMENT '座位编码，如 A1',
    seat_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '座位类型：NORMAL/LOVE/DISABLED',
    PRIMARY KEY (hall_id, seat_row, seat_col),
    UNIQUE KEY uk_seat_code (hall_id, seat_code),
    CONSTRAINT fk_seat_hall
        FOREIGN KEY (hall_id) REFERENCES cinema_hall (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '座位';

-- ----------------------------------------------------------------------------
-- 6. 场次表
--    对应模型 ShowSession；时间用 DATETIME，票价用分
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS show_session;
CREATE TABLE show_session (
    id         VARCHAR(20) NOT NULL COMMENT '场次编号，如 S001',
    movie_id   VARCHAR(20) NOT NULL COMMENT '影片编号',
    hall_id    VARCHAR(20) NOT NULL COMMENT '影厅编号',
    start_time DATETIME    NOT NULL COMMENT '开始时间',
    end_time   DATETIME    NOT NULL COMMENT '结束时间（开始+片长）',
    price_fen  INT         NOT NULL COMMENT '票价（分）',
    PRIMARY KEY (id),
    KEY idx_session_movie (movie_id),
    KEY idx_session_hall (hall_id),
    KEY idx_session_start (start_time),
    CONSTRAINT fk_session_movie
        FOREIGN KEY (movie_id) REFERENCES movie (id) ON DELETE RESTRICT,
    CONSTRAINT fk_session_hall
        FOREIGN KEY (hall_id) REFERENCES cinema_hall (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '放映场次';

-- ----------------------------------------------------------------------------
-- 7. 购物车表
--    对应模型 CartItem（按「用户×场次×座位」粒度一行）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS cart;
CREATE TABLE cart (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    user_id    VARCHAR(20) NOT NULL COMMENT '用户账号ID',
    session_id VARCHAR(20) NOT NULL COMMENT '场次编号',
    seat_code  VARCHAR(10) NOT NULL COMMENT '座位编码',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_seat (user_id, session_id, seat_code),
    KEY idx_cart_user (user_id),
    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_session
        FOREIGN KEY (session_id) REFERENCES show_session (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '购物车';

-- ----------------------------------------------------------------------------
-- 8. 订单表
--    对应模型 TicketOrder；冗余用户名与会员标签作为下单快照
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS ticket_order;
CREATE TABLE ticket_order (
    id              VARCHAR(20)                     NOT NULL COMMENT '订单号，如 T000001',
    user_id         VARCHAR(20)                     NOT NULL COMMENT '下单用户账号ID',
    username        VARCHAR(20)                     NOT NULL COMMENT '用户名快照',
    original_fen    INT                             NOT NULL COMMENT '原价合计（分）',
    discount_fen    INT                             NOT NULL DEFAULT 0 COMMENT '优惠金额（分）',
    payable_fen     INT                             NOT NULL COMMENT '应付金额（分）',
    membership_label VARCHAR(20)                    NULL COMMENT '会员等级名称快照',
    status          ENUM ('UNPAID','PAID','CANCELED') NOT NULL DEFAULT 'UNPAID' COMMENT '订单状态',
    ticket_code     VARCHAR(50)                     NULL COMMENT '取票码（支付后生成）',
    created_at      DATETIME                        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (id),
    KEY idx_order_user (user_id),
    KEY idx_order_status (status),
    KEY idx_order_created (created_at),
    CONSTRAINT fk_order_user
        FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '订单';

-- ----------------------------------------------------------------------------
-- 9. 订单明细表（座位级快照）
--    对应模型 TicketOrderItem；一行 = 订单中一个场次的一个座位
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS ticket_order_item;
CREATE TABLE ticket_order_item (
    id             VARCHAR(20) NOT NULL COMMENT '明细ID，如 OI001',
    order_id       VARCHAR(20) NOT NULL COMMENT '所属订单号',
    session_id     VARCHAR(20) NOT NULL COMMENT '场次编号',
    movie_title    VARCHAR(100) NOT NULL COMMENT '片名快照',
    hall_name      VARCHAR(50)  NOT NULL COMMENT '影厅名称快照',
    start_time     DATETIME     NOT NULL COMMENT '开始时间快照',
    end_time       DATETIME     NOT NULL COMMENT '结束时间快照',
    unit_price_fen INT          NOT NULL COMMENT '单价快照（分）',
    seat_code      VARCHAR(10)  NOT NULL COMMENT '座位编码',
    PRIMARY KEY (id),
    KEY idx_item_order (order_id),
    KEY idx_item_session (session_id),
    CONSTRAINT fk_item_order
        FOREIGN KEY (order_id) REFERENCES ticket_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_session
        FOREIGN KEY (session_id) REFERENCES show_session (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '订单明细（座位级快照）';

-- ============================================================================
-- 视图
-- ============================================================================

-- 场次详情：联表电影 + 影厅
DROP VIEW IF EXISTS v_session_detail;
CREATE VIEW v_session_detail AS
SELECT s.id              AS session_id,
       s.movie_id,
       m.title           AS movie_title,
       m.genre,
       m.duration_minutes,
       m.format,
       s.hall_id,
       h.name            AS hall_name,
       h.specs,
       h.row_count,
       h.col_count,
       s.start_time,
       s.end_time,
       s.price_fen
FROM show_session s
         JOIN movie m ON m.id = s.movie_id
         JOIN cinema_hall h ON h.id = s.hall_id;

-- 已占用座位：未支付 + 已支付订单占用的座位（下单即占座）
DROP VIEW IF EXISTS v_sold_seats;
CREATE VIEW v_sold_seats AS
SELECT i.session_id,
       i.seat_code
FROM ticket_order_item i
         JOIN ticket_order o ON o.id = i.order_id
WHERE o.status IN ('UNPAID', 'PAID');

-- 票房统计：按影片汇总「已支付」订单的售票张数与票房（分）
DROP VIEW IF EXISTS v_movie_boxoffice;
CREATE VIEW v_movie_boxoffice AS
SELECT m.id                             AS movie_id,
       m.title,
       COALESCE(t.tickets_sold, 0)      AS tickets_sold,
       COALESCE(t.boxoffice_fen, 0)     AS boxoffice_fen
FROM movie m
         LEFT JOIN (
    SELECT s.movie_id,
           COUNT(i.id)           AS tickets_sold,
           SUM(i.unit_price_fen) AS boxoffice_fen
    FROM show_session s
             JOIN ticket_order_item i ON i.session_id = s.id
             JOIN ticket_order o ON o.id = i.order_id
    WHERE o.status = 'PAID'
    GROUP BY s.movie_id
) t ON t.movie_id = m.id;

-- 订单汇总：每个订单的明细条数、座位数与金额
DROP VIEW IF EXISTS v_order_summary;
CREATE VIEW v_order_summary AS
SELECT o.id              AS order_id,
       o.user_id,
       o.username,
       o.status,
       o.ticket_code,
       o.created_at,
       COUNT(i.id)       AS item_count,
       o.original_fen,
       o.discount_fen,
       o.payable_fen
FROM ticket_order o
         LEFT JOIN ticket_order_item i ON i.order_id = o.id
GROUP BY o.id, o.user_id, o.username, o.status, o.ticket_code, o.created_at,
         o.original_fen, o.discount_fen, o.payable_fen;
