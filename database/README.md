# 光影票务系统 · 数据库设计文档

本目录为「光影票务 Swing UI Demo」配套的 MySQL 数据库，覆盖系统的全部业务功能。
与原 Demo 的内存 Mock 数据一一对应，并做了规范化、约束、索引与统计视图的完善补充。

## 一、基本信息

| 项 | 说明 |
|---|---|
| 数据库名 | `movie_ticket` |
| 引擎 | MySQL 8.0（InnoDB） |
| 字符集 | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| 脚本 | `01_schema.sql`（结构）、`02_seed.sql`（种子数据） |
| 海报图片 | `src/main/resources/posters/M001.jpg` ~ `M006.jpg` |

## 二、表结构总览（9 表 + 4 视图）

```
membership_level ──┐
                   │ (membership_code)
account ───────────┘
   │
   ├─(user_id)─ cart ─(session_id)─ show_session ─(movie_id)─ movie
   │                                  │      │
   │                                  │      └(hall_id)─ cinema_hall ─(hall_id)─ seat
   └─(user_id)─ ticket_order ─(order_id)─ ticket_order_item ─(session_id)─ show_session
```

### 1. `membership_level` — 会员等级目录
| 字段 | 类型 | 说明 |
|---|---|---|
| code | VARCHAR(20) PK | NORMAL / SILVER / GOLD |
| label | VARCHAR(20) | 普通会员 / 银卡会员 / 金卡会员 |
| discount_basis_points | INT | 折扣基点：1000 无折扣、950 九折、880 八八折 |
| sort_order | INT | 展示排序 |

### 2. `account` — 账号（管理员 + 顾客）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(20) PK | `A-<用户名>` / `U-<序号>` |
| username | VARCHAR(20) UNIQUE | 登录名 |
| password_hash | CHAR(64) | 密码 SHA-256 摘要 |
| display_name | VARCHAR(20) | 姓名 |
| phone | VARCHAR(11) | 手机号 |
| role | ENUM(ADMIN,CUSTOMER) | 角色 |
| enabled | TINYINT(1) | 是否启用 |
| membership_code | VARCHAR(20) FK | 会员等级 |

### 3. `movie` — 电影
| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(20) PK | M001… |
| title / genre / director / starring | VARCHAR | 片名/类型/导演/主演 |
| duration_minutes | INT | 片长（分钟） |
| format | VARCHAR(20) | 2D / 3D / IMAX |
| release_date | DATE | 上映日期 |
| synopsis | TEXT | 简介 |
| rating | DECIMAL(2,1) | 评分 0.0~10.0 |
| poster_path | VARCHAR(255) | 海报相对路径，如 `posters/M001.jpg` |
| poster_palette | INT | 无图时配色兜底索引 |

### 4. `cinema_hall` — 影厅
| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(20) PK | H01… |
| name / specs | VARCHAR | 名称 / 规格 |
| row_count / col_count | INT | 行数（A 起）/ 列数 |

### 5. `seat` — 座位（显式化，支持扩展座位类型）
| 字段 | 类型 | 说明 |
|---|---|---|
| hall_id | VARCHAR(20) PK·FK | 所属影厅 |
| seat_row | CHAR(1) PK | 行号 A~Z |
| seat_col | INT PK | 列号 1~N |
| seat_code | VARCHAR(10) UNIQUE | 座位编码，如 `A1` |
| seat_type | VARCHAR(20) | NORMAL / LOVE / DISABLED |

### 6. `show_session` — 放映场次
| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(20) PK | S001… |
| movie_id / hall_id | VARCHAR(20) FK | 影片 / 影厅 |
| start_time / end_time | DATETIME | 起止时间 |
| price_fen | INT | 票价（分） |

### 7. `cart` — 购物车（用户 × 场次 × 座位 粒度）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO PK | 自增 |
| user_id / session_id | VARCHAR(20) FK | 用户 / 场次 |
| seat_code | VARCHAR(10) | 座位编码 |
| created_at | DATETIME | 加入时间 |
| — | UNIQUE(user_id, session_id, seat_code) | 防重复加座 |

### 8. `ticket_order` — 订单
| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(20) PK | T000001… |
| user_id | VARCHAR(20) FK | 下单用户 |
| username / membership_label | VARCHAR | 下单快照 |
| original_fen / discount_fen / payable_fen | INT | 原价 / 优惠 / 应付（分） |
| status | ENUM(UNPAID,PAID,CANCELED) | 状态 |
| ticket_code | VARCHAR(50) | 取票码（支付后生成） |
| created_at | DATETIME | 下单时间 |

### 9. `ticket_order_item` — 订单明细（座位级快照）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(20) PK | OI001… |
| order_id | VARCHAR(20) FK | 所属订单 |
| session_id | VARCHAR(20) FK | 场次 |
| movie_title / hall_name / start_time / end_time / unit_price_fen | 快照 | 下单时冗余，查询无需多表联查 |
| seat_code | VARCHAR(10) | 座位编码 |

## 三、视图

| 视图 | 用途 |
|---|---|
| `v_session_detail` | 场次详情（联电影 + 影厅），对应售票列表 |
| `v_sold_seats` | 已占用座位（UNPAID + PAID 订单），对应选座图 |
| `v_movie_boxoffice` | 各电影票房（仅 PAID），对应票房统计 |
| `v_order_summary` | 订单汇总（明细条数 + 金额），对应订单查询 |

## 四、功能 → 表映射

| 功能 | 涉及表/视图 |
|---|---|
| 登录 / 注册 | `account`、`membership_level` |
| 顾客管理（列表/编辑/禁用/设会员） | `account`、`membership_level` |
| 影片管理（增删改查） | `movie` |
| 影厅管理（增删改查） | `cinema_hall`、`seat` |
| 场次管理（增删改查） | `show_session`、`movie`、`cinema_hall` |
| 选座（座位图） | `seat`、`v_sold_seats`、`v_session_detail` |
| 购物车 | `cart`、`show_session` |
| 购票 / 下单 | `ticket_order`、`ticket_order_item`、`show_session` |
| 支付 / 取消 | `ticket_order`（状态流转） |
| 我的订单 / 全部订单 | `ticket_order`、`ticket_order_item`、`v_order_summary` |
| 票房统计 | `v_movie_boxoffice` |

## 五、海报图片

6 部电影均为虚构，海报取自 Pixabay 免费图库（Pixabay Content License，可免费商用、无需署名），
统一裁剪为竖版（长边 1280px），存放于 `src/main/resources/posters/`：

| 影片 | 文件 | 主题 |
|---|---|---|
| 深海回声 | `M001.jpg` | 深海水母 |
| 星港24小时 | `M002.jpg` | 空间站 |
| 长安小厨 | `M003.jpg` | 中式饺子 |
| 旧城追光 | `M004.jpg` | 旧城街道 |
| 纸飞机航线 | `M005.jpg` | 手中纸飞机 |
| 冰川来信 | `M006.jpg` | 冰川雪原 |

数据库 `movie.poster_path` 字段已指向这些文件。

## 六、使用方法

```bash
# 按顺序执行（账号 root / 613617，可在脚本内改）
mysql -uroot -p613617 --default-character-set=utf8mb4 < database/01_schema.sql
mysql -uroot -p613617 --default-character-set=utf8mb4 < database/02_seed.sql
```

演示账号（密码均为 `123456`，库内以 SHA-256 存储）：

| 账号 | 角色 | 会员 |
|---|---|---|
| admin | 管理员 | 普通 |
| customer | 顾客 | 银卡 |
| chen | 顾客 | 金卡 |
| lin | 顾客 | 普通 |
| zhou | 顾客 | 银卡 |
| xu | 顾客（已禁用） | 普通 |

## 七、设计要点

1. **金额以「分」存储**（`*_fen`），与 Java 端 `priceFen` / `Money` 类一致，避免浮点误差。
2. **密码 SHA-256 摘要**，脚本内用 `SHA2('123456', 256)` 生成，杜绝明文入库。
3. **外键 + ON DELETE 规则**：电影/影厅/场次被引用时 `RESTRICT`，防止删除后产生孤儿订单；明细随订单 `CASCADE` 删除。
4. **座位显式化**：由「行列网格计算」升级为 `seat` 实体，可扩展座位类型、维护状态。
5. **订单快照**：`ticket_order_item` 冗余片名/影厅/时间，历史订单不随影片改名而变。
6. **视图承载统计**：票房、已售座位等派生数据用视图实时计算，避免冗余不一致。
7. **索引**：登录（username 唯一）、场次（movie/hall/start）、订单（user/status/created）等高频查询均建索引。
