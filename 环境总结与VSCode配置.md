# 本地环境总结（Handoff）& VSCode 配置指南

> 生成日期：2025-08-25 · 工作区：`C:\Users\LZ\Desktop\drenepsystem\`

---

## 一、工作区概况

| 路径 | 说明 |
|---|---|
| `nepsystem\` | Spring Boot 后端项目（Java + Maven） |
| `0824.docx` | 学习笔记：MyBatis-Plus 框架 + admins 表 ORM 映射 + CRUD 测试 |
| `0824环境搭建.docx` | 环境搭建笔记：MySQL + Navicat + JDK + IDEA + Maven 全流程 |

## 二、项目信息（nepsystem）

- **技术栈**：Spring Boot 2.6.13 + MyBatis-Plus 3.4.1 + MySQL 5.1.38 驱动（计划前端：Vue3 + Axios + Element-Plus）
- **JDK 目标**：Java 1.8（pom.xml 中 `source/target 1.8`）
- **包结构**：
  - `org.nep.nepsystem.NepsystemApplication` — 启动类（端口 8080）
  - `bean.Admins` — 实体类，映射 `admins` 表（admin_id 主键）
  - `dao.AdminsDao` — 继承 `BaseMapper<Admins>`，`@Mapper` 注解
  - `ctrl.AdminsCtrl` — /admins/insert、/admins/update、/admins/delete（写死主键 2 的测试接口）
  - `demos.web.BasicController` — 官方示例（/hello、/user 等）
- **配置**（`application.properties`）：`server.port=8080`，数据库 `jdbc:mysql://localhost:3306/nep`，账号 `root`，密码 `mysql` ⚠️

## 三、本机环境清单（已实测）

| 组件 | 版本/路径 | 状态 |
|---|---|---|
| JDK | **21.0.9 LTS** → `D:\Develop\JDK21`（`JAVA_HOME` 已设置） | ✅ 正常 |
| Maven | **3.9.9** → `D:\Develop\mvn\apache-maven-3.9.9` | ✅ 正常 |
| Maven 本地仓库 | **`D:\dev\java\maven\repository`**（全局 settings.xml 指定，非默认 ~/.m2） | ⚠️ 缺 2.6.13 依赖 |
| Maven 镜像 | nexus-aliyun（阿里云，已配好） | ✅ 可联网下载 |
| Node.js | v22.20.0 | ✅ 正常（Vue 前端用） |
| Git | 2.53.0.windows.2 | ✅ 正常 |
| VSCode | `D:\Tools\Microsoft VS Code\bin\code.cmd` | ✅ 已安装 |
| MySQL | **MySQL80 服务运行中**，端口 3306 可连接 | ✅ 正常 |
| Navicat | `C:\Program Files (x86)\PremiumSoft\Navicat Premium`（按文档） | 按文档已装 |
| MySQL Workbench | `D:\Develop\MySQL\MySQL workbench 8.0 CE` | ✅ 存在 |
| IDEA | 未安装（但项目里残留 `.idea` 配置，引用了 JDK 1.8） | ⚠️ 已不可用 |

## 四、数据库现状 ⚠️（已实测确认）

- MySQL 服务 **运行中**（3306 端口正常监听）。
- **root 密码已确认：`124102`**（2025-08-25 由用户提供，已实测可登录 ✅）
  → 请到 Navicat/Workbench 的已保存连接里确认真实密码，或重置 root 密码，再同步修改 `application.properties`。
- 数据库 `nep` 及 8 张表按文档应已建好（密码不对，暂无法直接验证表内容）。
- **影响**：已同步更新 `application.properties` 中 `spring.datasource.password=124102`，数据库接口可正常使用。

## 五、VSCode 连接与配置（已为你生成配置文件）

### 1. 打开项目
```
code C:\Users\LZ\Desktop\drenepsystem\nepsystem
```
（或在 VSCode 中 File → Open Folder 选择 `nepsystem` 文件夹，注意是内层文件夹）

### 2. 安装扩展（已写入 `.vscode/extensions.json`，打开时会提示安装）
- **Extension Pack for Java**（`vscjava.vscode-java-pack`）— 语言支持 + 调试器 + 测试
- **Maven for Java**（`vscjava.vscode-maven`）— Maven 项目管理
- **Spring Boot Tools**（`vmware.vscode-spring-boot`）+ **Spring Initializr** — Spring 支持

### 3. 已生成的配置
- `nepsystem\.vscode\settings.json` — JDK 21 运行时、Maven 路径（`mvn.cmd`）、UTF-8 编码
- `nepsystem\.vscode\launch.json` — 调试配置 `Spring Boot - NepsystemApplication`（F5 直接运行/断点调试）
- `nepsystem\.vscode\extensions.json` — 推荐扩展清单

> 首次打开后 VSCode 会自动导入 Maven 项目并下载缺失依赖（需联网，走阿里云镜像），右下角有进度。

### 4. 运行方式（重要 ⚠️）
- **方式 A（推荐）**：F5 或运行调试配置 `Spring Boot - NepsystemApplication`（已生成 launch.json，不依赖 Maven 插件，实测可用）
- **方式 B**：终端执行 `mvn spring-boot:run` — ⚠️ **当前 pom.xml 里 spring-boot-maven-plugin 配了 `<skip>true</skip>`，会静默跳过启动（BUILD SUCCESS 但应用不启动，实测踩坑）**。两种解法：
  1. 删掉 pom.xml 里 plugin 配置中的 `<skip>true</skip>` 一行（推荐，删后 `mvn package` 也能打出可运行 jar）；
  2. 或继续用方式 A 的 F5 调试（不受 skip 影响）。
- **方式 C**：Spring Boot Dashboard 面板点启动
- 验证接口：http://localhost:8080/hello?name=test （不依赖数据库，可先测这个）→ 实测返回 `Hello test`；http://localhost:8080/admins/insert 需先修复数据库密码

## 六、注意事项

1. **依赖已补齐 ✅**：本次已从阿里云镜像下载 `spring-boot-dependencies 2.6.13` 及全部依赖到全局仓库 `D:\dev\java\maven\repository`，`mvn compile` / `test-compile` 实测 **BUILD SUCCESS**（7 个源文件）。
2. **JDK 版本**：项目按 Java 1.8 编译（JDK 21 兼容，仅编译警告）；`.idea/misc.xml` 里引用的是 JDK 1.8（本机已无），VSCode 已改用 JDK 21。
3. **spring-boot-maven-plugin 设置了 `skip=true`**：`mvn package` 不生成可执行 fat jar，且 **`mvn spring-boot:run` 会被静默跳过**（详见"运行方式"），建议删掉该行；VSCode F5 调试不受影响（实测启动成功，/hello 返回正常）。
4. `.vscode/` 在 `.gitignore` 中（本目录也**不是** git 仓库，未初始化），配置仅本地生效。
5. 前端（Vue3）尚未创建，Node 22 环境已就绪，后续可用 Vite 脚手架。
6. **本机只剩 JDK 21**（IDEA 已卸载）：以后用 VSCode + 扩展包即可，无需再装 IDEA。

## 七、建议下一步（按优先级）

1. **修复数据库密码**：确认 MySQL root 真实密码并更新 `application.properties`（或重置为 `mysql`）→ 否则 /admins/* 全部 500
2. **删除 pom.xml 中的 `<skip>true</skip>`**：让 `mvn spring-boot:run` / `mvn package` 恢复正常
3. 用 VSCode 打开项目跑通 F5 调试（实测可行）
4. 数据库建全 8 张表（目前只有 admins 相关）
5. 初始化 git 仓库做版本管理