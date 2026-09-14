# 光影票务系统 · 后端（数据库持久化层）

这是「电影票系统」小组项目的**后端部分**。

前三位同学做的是界面与业务演示，数据存在内存里，程序一关就没了。
这一层把它们接到 **MySQL 真实数据库**上：账号、影片、影厅、场次、座位、购物车、订单、
会员等级全部落库，并保证多个人同时抢座位时不会卖出同一张票。

---

## 一、需要什么才能跑起来

| 需要的东西 | 你机器上的情况 | 说明 |
|---|---|---|
| JDK | 已装（17，在 `D:\`） | 代码按 JDK 17 写，兼容 |
| Maven | 已装（`C:\maven`） | 用来编译打包 |
| MySQL | 已装且已在运行（端口 3306） | 需要你提供 root 密码 |

**只需要做一件事：把 MySQL 密码填到配置文件里。**

---

## 二、三步跑起来

### 第 1 步：填密码

打开这个文件：

```
src/main/resources/db.properties
```

把 `db.password=` 后面改成你本机 MySQL 的 root 密码，保存：

```properties
db.password=你的密码
db.user=root
```

### 第 2 步：建库并导入数据

在项目根目录（`movie-ticket-backend`）打开命令行，依次执行：

```bash
mysql -uroot -p < database/01_schema.sql
mysql -uroot -p < database/02_seed.sql
```

执行第一条会提示输入密码，输完回车即可。

> 如果提示 `mysql` 不是内部或外部命令，用完整路径：
> `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p < database/01_schema.sql`

### 第 3 步：编译并运行验证程序

```bash
mvn clean package
java -jar target/movie-ticket-backend-1.0.0.jar
```

程序会自动跑 8 组验证并打印详细结果，最后给出「通过 N 项，失败 N 项」的结论。

> Maven 如果报 `JAVA_HOME` 相关错误，先执行：
> `set JAVA_HOME=D:\` 再运行（你机器 JDK 装在 D 盘根目录）。

---

## 三、验证程序会给你看什么

| 步骤 | 验证内容 |
|---|---|
| 1 | 数据库连通性，打印 MySQL 版本 |
| 2 | 读取影片 / 影厅 / 场次 / 顾客，确认 `SELECT` 正常 |
| 3 | 登录校验：正确口令通过、错误口令拒绝、禁用账号拦截、注册新用户 |
| 4 | 座位图与已占座位查询（走数据库视图 `v_sold_seats`） |
| 5 | 加入购物车、下单、**下单即占座**、他人抢同座位被拦截 |
| 6 | 支付出票、按累计消费**自动升级会员**、会员数据统计 |
| 7 | 取消订单后**座位自动释放** |
| 8 | 11 项业务规则拦截（空座位、超 6 座、越界、重复支付等） |

全部通过时输出：

```
══════════════════════════════════════════════════════════════════
  验证结果：通过 N 项，失败 0 项
  结论：后端持久化层工作正常，所有业务规则均按预期生效。
══════════════════════════════════════════════════════════════════
```

---

## 四、代码结构

```
movie-ticket-backend/
├── database/
│   ├── 01_schema.sql          建库建表：9 张表 + 4 个视图
│   └── 02_seed.sql            初始化数据：会员档位、账号、影片、影厅、场次
├── src/main/java/com/movieticket/backend/
│   ├── BackendDemo.java       验证程序入口（java -jar 跑的就是它）
│   ├── db/
│   │   ├── Db.java            连接管理与事务模板
│   │   ├── DbConfig.java      连接配置（环境变量 > 配置文件 > 默认值）
│   │   └── ServiceException.java  业务异常
│   ├── model/                 领域模型（8 个 record + 2 个枚举）
│   ├── dao/                   数据访问层（7 个 DAO，全部 PreparedStatement）
│   │   ├── AccountDao        账号
│   │   ├── MovieDao          影片
│   │   ├── CinemaHallDao     影厅（含座位批量生成）
│   │   ├── ShowSessionDao    场次
│   │   ├── SeatDao           座位与占座查询
│   │   ├── CartDao           购物车
│   │   └── OrderDao          订单主表 + 明细表
│   └── service/
│       ├── MovieTicketService.java  业务服务（全部业务规则）
│       └── PasswordHasher.java      SHA-256 口令摘要
└── src/main/resources/
    └── db.properties          数据库连接配置
```

---

## 五、数据库表设计

| 表名 | 作用 | 关键点 |
|---|---|---|
| `membership_level` | 会员等级目录 | `sort_order` 与 Java 枚举顺序一致 |
| `account` | 账号 | 存 SHA-256 摘要，不存明文口令 |
| `movie` | 影片 | |
| `cinema_hall` | 影厅 | 存行列数 |
| `seat` | 座位 | 把「行列网格」显式化为实体，便于扩展座位类型 |
| `show_session` | 场次 | 票价用「分」存 INT |
| `cart` | 购物车 | 一行 = 一个座位 |
| `ticket_order` | 订单主表 | 状态：UNPAID / PAID / CANCELED |
| `ticket_order_item` | 订单明细 | 一行 = 一个座位，冗余片名影厅作快照 |

**4 个视图**：

- `v_session_detail` —— 场次联表电影与影厅
- `v_sold_seats` —— **已占座位**（未支付 + 已支付都占座，取消后释放）
- `v_movie_boxoffice` —— 按影片统计票房
- `v_order_summary` —— 订单金额与座位数汇总

---

## 六、三个关键设计决定

### 1. 金额一律用「分」存整数

不用 `FLOAT`/`DOUBLE`，避免 `46.00` 变成 `45.99999`。Java 端对应 `int priceFen`，
折扣计算用 `BigDecimal` 四舍五入到分。

### 2. 占座口径由数据库视图统一定义

「哪些座位被占了」这件事只写在 `v_sold_seats` 视图里：

```sql
WHERE o.status IN ('UNPAID', 'PAID')
```

好处是**取消订单时不用手工删占用记录**——只需把状态改成 `CANCELED`，座位自然释放。
Java 端和数据库端永远不会有第二套判断逻辑，杜绝两处打架。

### 3. 抢座用 `SELECT ... FOR UPDATE` 加锁

下单前会锁定相关座位行，两个用户同时点同一个座位时，只有一个能成功，
**从数据库层面杜绝超卖**。整个「检查座位 + 建订单 + 写明细」在一个事务里，
任一步失败全部回滚。

---

## 七、常见问题

**Q：提示 `Access denied for user 'root'`**
密码不对。改 `src/main/resources/db.properties` 里的 `db.password`。

**Q：提示 `Unknown database 'movie_ticket'`**
还没建库。执行第 2 步的两条 SQL。

**Q：提示 `Communications link failure`**
MySQL 没启动。以管理员身份打开命令行执行 `net start MySQL80`。

**Q：`mvn` 报 `JAVA_HOME` 找不到**
你机器 JDK 装在 `D:\`。执行 `set JAVA_HOME=D:\` 后重试。

**Q：想恢复初始数据**
重新执行 `database/02_seed.sql` 即可，脚本会重建全部演示数据。

---

## 八、演示账号

| 角色 | 用户名 | 密码 | 会员等级 |
|---|---|---|---|
| 管理员 | `admin` | `123456` | — |
| 顾客 | `customer` | `123456` | 银卡 |
| 顾客 | `chen` | `123456` | 金卡 |
| 顾客 | `lin` | `123456` | 普通 |
| 顾客 | `zhou` | `123456` | 银卡 |
| 顾客 | `xu` | `123456` | 已禁用（用于测试拦截）|
