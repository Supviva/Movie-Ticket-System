# 项目长期记忆：影院管理系统（movies）

## 项目概况
- 技术栈：Java Swing + JDBC + SQL Server（库名 movie）+ JFreeChart + JTattoo 皮肤；Eclipse 工程，`.classpath` 里含绝对路径依赖（D:/Zlinjie/...），换机需改。
- 入口：`Main.Login.main` → 登录 → `use_level==1` 进管理员 `Main.Main`，否则进顾客端 `Main.TicketMain`。
- 分层：UI 包（Main/Movie/Hall/Play/Sale/Customer/Vip/Statistics）→ UserDao 包 → User 实体包 → connect.Db。
- DB 连接：`connect.Db`，sa/123456，jdbc:sqlserver://localhost:1433;DatabaseName=movie。
- 数据库对象：表 movie/hall/play/ticket/customer/vip/users，视图 v_play、v_vip，存储过程 pr_tickets。
- VIP 折扣用策略模式：Vip.Discount 抽象类 + VIPA(9折)/VIPB/VIPC/VIPD(无折扣)。
- 会话态：User.LoginID 单例保存 CusId（字符串，未登录为 "000"）。

## 已知问题（未修，用户未要求）
- `type=="customer"` 用 == 比较字符串（CustomerDao）。
- DAO 与 Chat 包大量字符串拼接 SQL，存在注入风险；Chat 包重复硬编码连接串。
- 多数 DAO 未关闭 ResultSet/PreparedStatement，部分 finally 里 return 吞掉异常。
- 中文乱码：源码含 GBK 字面量（如 jFrame.setTitle("ӰƬ")），需按 GBK 编译。
- Seat 图标用相对路径 "src/img/1.png"，工作目录不对就加载失败。
