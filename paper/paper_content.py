# -*- coding: utf-8 -*-
"""
《基于 Spring Boot 与 Vue 的多合一环境监测保护系统的设计与实现》
论文内容数据（结构化元素列表）。
元素类型：
  ("h1", 文本)                  一级标题（第X章）
  ("h2", 文本)                  二级标题（X.Y）
  ("h3", 文本)                  三级标题（X.Y.Z）
  ("body", 文本)                正文段落
  ("caption", 文本)             表格/图题
  ("table", {title, header, rows})  表格（title 可选）
  ("code", 文本)                代码块
  ("fig", 文本)                 图占位说明
  ("ref", 文本)                 参考文献条目
  ("abstract_cn", 文本)         中文摘要段落
  ("abstract_en", 文本)         英文摘要段落
"""

PAPER_TITLE = "基于 Spring Boot 与 Vue 的多合一环境监测保护系统的设计与实现"
PAPER_TITLE_EN = "Design and Implementation of an All-in-One Environmental Monitoring and Protection System Based on Spring Boot and Vue"

# 封面占位（用户自行填写）
COVER = {
    "school": "××大学",
    "doc_type": "课程设计论文",
    "course": "（课程名称）",
    "student": "（学生姓名）",
    "student_id": "（学 号）",
    "major_class": "（专业/班级）",
    "teacher": "（指导教师）",
    "date": "2026 年 8 月",
}

ABSTRACT_CN_KEYWORDS = "关键词：环境监测；Spring Boot；Vue 3；WebSocket；阈值告警；前后端分离"
ABSTRACT_EN_KEYWORDS = "Keywords: environmental monitoring; Spring Boot; Vue 3; WebSocket; threshold alerting; front-end and back-end separation"

ABSTRACT_CN = [
    "随着工业化与城市化进程的加快，空气污染、水质恶化、噪声扰民等环境问题日益突出，传统的人工巡检与专业仪器监测方式存在成本高、时效差、覆盖范围有限等不足，难以满足环境管理对“自动采集、实时展示、阈值告警”闭环管理的要求。针对上述问题，本文设计并实现了一套多合一环境监测保护系统。",
    "系统采用前后端分离架构：后端基于 Spring Boot 2.6.13 与 MyBatis-Plus 构建 RESTful API，通过指标字典驱动的方式覆盖空气质量、水质与噪声三类共八项监测指标，并设计了基于 WARN/ALARM 两级阈值与 30 分钟防重复机制的告警引擎，监测数据写入、设备状态更新与告警生成在同一事务内完成，保证数据一致性；前端采用 Vue 3、Element Plus 与 ECharts 构建 ENVISION 深色驾驶舱风格的数据大屏，借助 WebSocket 实现监测数据与告警信息的秒级实时推送。",
    "测试结果表明：接口冒烟测试 7 项全部通过；端到端测试覆盖认证、设备、数据、告警、阈值、统计、用户、指标字典、WebSocket 与前端链路 10 个模块共 36 项断言全部通过；阈值告警、事务回滚、实时推送等关键功能均符合预期。系统实现了环境数据从采集、存储、展示到告警处置的完整闭环，具有良好的可扩展性，可为校园、园区等场景的低成本环境监测提供可复用的参考方案。",
]

ABSTRACT_EN = [
    "With the acceleration of industrialization and urbanization, environmental problems such as air pollution, water quality deterioration and noise disturbance have become increasingly prominent. Traditional monitoring methods based on manual inspection and professional instruments suffer from high cost, poor timeliness and limited coverage, which can hardly satisfy the closed-loop management requirement of automatic collection, real-time display and threshold alerting. To address these issues, this paper designs and implements an all-in-one environmental monitoring and protection system.",
    "The system adopts a front-end and back-end separation architecture. The back-end is built on Spring Boot 2.6.13 and MyBatis-Plus to provide RESTful APIs, covering eight monitoring indicators of air quality, water quality and noise driven by a sensor dictionary. An alerting engine based on two-level thresholds (WARN/ALARM) and a 30-minute de-duplication mechanism is designed, and data writing, device status updating and alert generation are performed within the same transaction to guarantee data consistency. The front-end uses Vue 3, Element Plus and ECharts to build an ENVISION dark cockpit-style dashboard, and WebSocket is adopted to push monitoring data and alert messages in real time at second level.",
    "Test results show that all the 7 interface smoke tests passed, and all the 36 assertions of the end-to-end tests covering 10 modules (authentication, devices, data, alerts, thresholds, statistics, users, sensor dictionary, WebSocket and front-end chain) passed. Key functions such as threshold alerting, transaction rollback and real-time push all met expectations. The system realizes a complete closed loop from data collection, storage and display to alert handling, has good scalability, and provides a reusable low-cost reference solution for environmental monitoring in campuses and parks.",
]

CONTENT = [
    # ===================== 第 1 章 绪论 =====================
    ("h1", "第 1 章　绪论"),
    ("h2", "1.1　研究背景与意义"),
    ("body", "随着工业化与城市化进程的持续加快，空气污染、水质恶化、噪声扰民等环境问题日益突出，已经成为影响公众健康和生活质量的重要社会问题。环境监测作为掌握环境质量状况、识别污染来源、评估治理效果的基础性工作，是环境保护与污染防治的重要前提。近年来，我国不断加大生态环境监测网络建设力度，推动监测体系向“天空地海”一体化、数智化方向转型，对环境数据的自动化采集、实时传输、智能分析与及时预警提出了更高要求。"),
    ("body", "然而，传统的环境监测多依赖人工巡检与专业仪器，普遍存在建设成本高、监测时效差、覆盖范围有限、数据不透明等不足。对于校园、园区等中小尺度场景，建设大型专业监测站既不经济也不灵活。随着物联网、云计算与 Web 应用技术的成熟，利用前后端分离架构、实时通信与数据可视化技术，以较低成本搭建一套“自动采集—实时展示—阈值告警”闭环的小型环境监测系统，具有重要的现实意义与应用价值。"),
    ("body", "本文以多合一环境监测保护系统的设计与实现为研究对象，综合运用 Spring Boot、Vue 3、WebSocket、MySQL 与 ECharts 等主流技术，构建一套覆盖空气质量、水质与噪声三类指标的监测平台。该系统一方面通过指标字典驱动的方式实现监测类型的灵活扩展，另一方面通过两级阈值告警与实时推送形成完整的管理闭环，为校园、园区等场景提供一套低成本、可扩展、易维护的环境监测解决方案，也进一步验证了前后端分离架构与实时推送技术在该类业务场景中的可行性与有效性。"),
    ("h2", "1.2　国内外研究现状"),
    ("body", "从行业现状看，环境监测正加速向自动化、数字化与智能化方向发展。国外对物联网环境监测的研究起步较早，围绕空气质量、水环境、精准农业等应用领域开展了大量研究，重点关注异构设备的接入、数据安全、系统扩展性与实时性等问题，形成了较为成熟的技术体系。国内方面，我国已建成全球规模最大的生态环境物联网监测网络，环境监测站点覆盖大气、水、噪声等主要环境要素，并依托人工智能、物联网、区块链等新技术推动监测网络智能化改造与数智化升级，实现监测数据采集、传输、处理、分析及应用支撑的全流程智能化。"),
    ("body", "在技术层面，随着前后端分离架构成为 Web 应用开发的主流模式，Spring Boot 凭借“约定优于配置”的理念与自动化配置能力，成为 Java 生态中构建后端服务的主流框架；Vue 3 以其渐进式、组件化与响应式的特性，广泛应用于前端界面开发；ECharts 提供了丰富的图表类型与高性能渲染能力，成为数据可视化大屏建设的常用选择；WebSocket 协议在单个 TCP 连接上提供全双工通信，有效解决了传统 HTTP 轮询在实时性上的不足，广泛用于实时监控、即时消息等场景。"),
    ("body", "综合来看，现有研究多聚焦于大规模监测网络与专业监测系统，面向校园、园区等中小尺度场景、强调低成本与快速落地的多指标一体化监测系统仍具有较大的探索空间。本文结合上述技术，以实际可运行系统为载体，重点研究多指标统一接入、两级阈值告警与实时推送一体化实现方案，具有一定的工程参考价值。"),
    ("h2", "1.3　主要研究内容与章节安排"),
    ("body", "本文围绕多合一环境监测保护系统的设计与实现展开，主要研究内容包括：（1）基于指标字典的数据模型设计，实现空气、水质、噪声多类指标的统一接入与灵活扩展；（2）基于事务机制的数据上报流程设计，保证监测数据写入、设备状态更新与告警生成的原子性；（3）基于两级阈值与防重复机制的告警引擎设计；（4）基于 WebSocket 的实时数据与告警推送机制设计；（5）基于 Vue 3 与 ECharts 的实时监测大屏与业务管理界面实现；（6）系统接口与端到端测试。"),
    ("body", "全文共分七章，各章内容安排如下：第 1 章为绪论，介绍研究背景、意义、国内外研究现状与主要研究内容；第 2 章介绍系统开发所采用的关键技术；第 3 章进行系统需求分析；第 4 章完成系统总体设计，包括架构设计、功能模块设计与数据库设计；第 5 章阐述系统核心功能的具体实现；第 6 章对系统进行测试与结果分析；第 7 章对全文进行总结并对后续工作进行展望。"),
    # ===================== 第 2 章 相关技术介绍 =====================
    ("h1", "第 2 章　相关技术介绍"),
    ("h2", "2.1　Spring Boot 框架"),
    ("body", "Spring Boot 是 Spring 生态中用于快速构建独立、生产级应用的框架，其核心理念是“约定优于配置”，通过自动化配置与起步依赖（Starter）机制，大幅简化了 Spring 应用的搭建与部署过程。Spring Boot 内置 Tomcat 等嵌入式服务器，开发者无需部署外部 Web 容器即可直接运行应用；同时提供 Actuator 监控、统一异常处理、参数校验等开箱即用能力，显著提升了后端开发效率。本文后端采用 Spring Boot 2.6.13，基于 spring-boot-starter-web 构建 RESTful API，基于 spring-boot-starter-validation 完成参数校验，基于 spring-boot-starter-websocket 实现实时消息推送。"),
    ("h2", "2.2　MyBatis-Plus 持久层框架"),
    ("body", "MyBatis-Plus 是 MyBatis 的增强工具，在保留 MyBatis 原有能力的基础上“只做增强不做改变”，提供通用 Mapper、条件构造器、物理分页、自动填充等丰富功能。借助 BaseMapper 接口，开发者无需编写基础的单表 CRUD SQL，即可完成常见的数据访问操作；LambdaQueryWrapper 等条件构造器以类型安全的方式拼接查询条件，简化了动态 SQL 的编写。本文使用 MyBatis-Plus 3.4.1 配合分页插件完成监测数据、设备、告警等数据的分页查询与条件过滤。"),
    ("h2", "2.3　Vue 3 前端框架"),
    ("body", "Vue 3 是一套用于构建用户界面的渐进式 JavaScript 框架，采用基于 Proxy 的响应式系统与组合式 API，代码组织更加灵活，性能与 TypeScript 友好度较 Vue 2 均有显著提升。Vue Router 负责前端路由管理，Pinia 负责全局状态管理，二者与 Vue 3 共同构成成熟的前端工程化方案。本文前端采用 Vue 3.5 结合 Vite 构建工具、Element Plus 组件库与 ECharts 图表库，实现监测大屏与各类业务管理页面。"),
    ("h2", "2.4　ECharts 数据可视化"),
    ("body", "ECharts 是一个基于 JavaScript 的开源可视化图表库，提供折线图、柱状图、仪表盘、散点图等二十余种图表类型与丰富的组件，支持 Canvas、SVG 双渲染引擎与流式加载，可在千万级数据规模下保持流畅交互。其声明式的配置方式使得开发者能够以简洁的配置对象快速构建交互式可视化图表。本文使用 ECharts 5.5 实现仪表盘（Gauge）、实时曲线、空间分布等监测大屏图表。"),
    ("h2", "2.5　WebSocket 实时通信技术"),
    ("body", "WebSocket 是一种在单个 TCP 连接上进行全双工通信的网络协议，由 IETF 于 2011 年标准化为 RFC 6455。与传统的 HTTP 请求—响应模式不同，WebSocket 连接建立后，客户端与服务端可以随时互相发送消息，避免了高频轮询带来的连接开销与传输延迟，非常适合实时监控、即时消息等场景。本文后端基于 spring-boot-starter-websocket 实现消息端点，前端通过 WebSocket API 建立连接，实现监测数据与告警信息的秒级实时推送，并辅以断线自动重连与定时轮询兜底机制保证可靠性。"),
    ("h2", "2.6　MySQL 数据库"),
    ("body", "MySQL 是一款开源的关系型数据库管理系统，具有性能稳定、使用简单、生态成熟等特点，广泛应用于各类 Web 应用的数据存储。本文采用 MySQL 8.0 作为数据存储引擎，使用 InnoDB 存储引擎保证事务的一致性与崩溃恢复能力，字符集采用 utf8mb4 以支持完整的中文字符集。系统中监测数据表与告警表均建立复合索引，以支撑时序数据的高效查询。"),
    # ===================== 第 3 章 系统需求分析 =====================
    ("h1", "第 3 章　系统需求分析"),
    ("h2", "3.1　可行性分析"),
    ("body", "技术可行性方面，系统所采用的 Spring Boot、MyBatis-Plus、Vue 3、MySQL、WebSocket 与 ECharts 均为成熟、开源且社区活跃的技术，开发资料丰富，技术风险低。经济可行性方面，系统全部基于开源技术栈实现，可运行于普通个人电脑，无需额外购置专业设备，数据采集环节采用软件模拟器完成演示，后期可平滑接入低成本传感器硬件，整体投入可控。操作可行性方面，系统以浏览器为访问入口，提供图形化界面与实时大屏，管理人员无需专业背景即可完成设备管理、阈值配置与告警处理等日常操作。综合来看，系统在技术、经济与操作三个层面均具备可行性。"),
    ("h2", "3.2　功能需求分析"),
    ("body", "结合环境监测的业务特点，系统功能需求划分为设备管理、数据采集、数据展示、告警管理与系统管理五大部分，具体功能需求如表 3-1 所示。"),
    ("table", {
        "title": "表 3-1　系统功能需求清单",
        "header": ["功能模块", "功能点", "功能说明"],
        "rows": [
            ["设备管理", "设备增删改查", "监测设备/站点的注册、修改、删除与分页查询，支持按类型、状态、关键字筛选"],
            ["设备管理", "设备状态管理", "维护设备在线/离线/停用状态，展示设备总数与在线数"],
            ["数据采集", "数据上报", "支持按设备编号上报一项或多项指标数据，并写入历史记录"],
            ["数据采集", "数据模拟器", "定时为每台设备生成符合量程的随机数据，支持概率峰值以演示告警"],
            ["数据展示", "实时监测大屏", "以仪表盘、实时曲线、空间分布等可视化方式展示最新监测数据"],
            ["数据展示", "历史数据查询", "支持按设备、指标、时间范围分页查询历史数据并绘制趋势曲线"],
            ["数据展示", "数据导出", "支持将监测数据按条件导出为 CSV 文件"],
            ["告警管理", "阈值配置", "按设备或全局配置各指标的预警/报警上下限，支持启停"],
            ["告警管理", "告警生成与推送", "超阈值自动生成 WARN/ALARM 告警并实时推送，30 分钟防重复"],
            ["告警管理", "告警处理", "查看未处理告警并进行处理登记，形成处理闭环"],
            ["系统管理", "登录认证", "管理员账号密码登录、登出，接口返回令牌"],
            ["系统管理", "用户管理", "普通用户的增删改查与启停管理"],
            ["系统管理", "指标字典", "维护监测指标编码、单位、量程与标准限值，驱动前端渲染"],
        ],
    }),
    ("h2", "3.3　非功能需求分析"),
    ("body", "（1）数据一致性：数据上报过程涉及监测数据写入、设备状态更新与告警生成多个写操作，必须保证三者同成功或同失败，避免产生脏数据。（2）实时性：监测数据与告警信息应实现秒级到达，前端大屏无需手动刷新即可看到最新数据。（3）可扩展性：通过指标字典与设备类型驱动，新增监测指标或设备类型无需改动核心代码。（4）界面友好性：大屏采用深色驾驶舱设计语言，信息层级清晰，操作页面符合常规管理习惯。（5）可维护性与可部署性：前后端分离、模块分层清晰，支持开发模式与生产模式两种部署方式。"),
    # ===================== 第 4 章 系统设计 =====================
    ("h1", "第 4 章　系统设计"),
    ("h2", "4.1　系统总体架构设计"),
    ("body", "系统采用前后端分离的三层总体架构，由数据源层、后端服务层与展示层组成，如图 4-1 所示。数据源层包括数据模拟器与预留的真实硬件接入通道（HTTP/MQTT），负责产生或采集环境监测数据；后端服务层基于 Spring Boot 构建，包含 RESTful API、阈值告警引擎、WebSocket 服务与 MySQL 数据存储，承担业务逻辑处理；展示层为基于 Vue 3 构建的 Web 前端，包含实时监测大屏与业务管理页面。前端通过 HTTP 请求调用后端接口，通过 WebSocket 接收实时数据与告警推送。"),
    ("fig", "图 4-1　系统总体架构图（此处插入系统三层架构示意图）"),
    ("body", "后端在工程结构上按分层思想组织：bean 包存放实体类，dao 包存放数据访问接口，service 包与 impl 子包存放业务逻辑，ctrl 包存放控制器，common 包存放统一响应与全局异常处理，config 包存放配置类，ws 包存放 WebSocket 处理器，service/simulator 子包存放数据模拟器。前端按视图组织：Dashboard 为实时监测大屏，Devices、Alerts、Thresholds、History、Users 等为业务管理页面。"),
    ("h2", "4.2　系统功能模块设计"),
    ("body", "系统功能模块划分为设备管理、监测数据、告警管理、阈值管理、统计分析与系统管理六大模块，各模块通过 RESTful API 对外提供服务。设备管理模块提供设备分页查询、新增、修改、删除与在线统计能力；监测数据模块提供数据上报、最新数据、历史数据、趋势聚合与 CSV 导出能力；告警管理模块提供告警分页、未处理统计与告警处理能力；阈值管理模块提供全局/设备级阈值的增删改查能力；统计分析模块提供系统总览、环境质量评分与设备上报排行能力；系统管理模块提供登录认证、用户管理与指标字典查询能力。各模块职责边界清晰、相互独立，便于后续扩展。"),
    ("h2", "4.3　数据库设计"),
    ("h3", "4.3.1　概念结构设计"),
    ("body", "根据需求分析，系统主要数据实体包括管理员（admins）、用户（users）、区域（regions）、监测设备（devices）、监测指标（sensors）、监测数据（monitor_data）、告警阈值（thresholds）与告警记录（alerts）。实体之间的主要关系为：区域与设备为一对多关系；设备与监测数据为一对多关系；设备与告警记录为一对多关系；设备与告警阈值为一对多关系，阈值表通过 device_id 为空（NULL）表示全局默认阈值；监测数据与告警记录均通过 sensor_code 与指标字典关联。admins 与 users 为独立权限表。"),
    ("h3", "4.3.2　逻辑结构设计"),
    ("body", "根据概念结构设计，数据库命名为 nep，采用 InnoDB 存储引擎与 utf8mb4 字符集，共设计 8 张数据表。监测数据表与告警记录表为核心表，采用 BIGINT 自增主键并建立复合索引以支撑时序查询。各表结构设计如下。"),
    ("table", {
        "title": "表 4-1　devices 监测设备表",
        "header": ["字段", "类型", "说明"],
        "rows": [
            ["id", "INT 自增 PK", "设备ID"],
            ["device_code", "VARCHAR(50) UNIQUE", "设备编号（上报用）"],
            ["device_name", "VARCHAR(100)", "设备名称"],
            ["type", "VARCHAR(20)", "AIR/WATER/NOISE"],
            ["region_id", "INT", "所属区域"],
            ["location", "VARCHAR(255)", "安装位置"],
            ["status", "TINYINT", "0离线 1在线 2停用"],
            ["last_report_time", "DATETIME", "最近上报时间"],
            ["create_time", "DATETIME", "创建时间"],
        ],
    }),
    ("table", {
        "title": "表 4-2　sensors 监测指标字典表",
        "header": ["字段", "类型", "说明"],
        "rows": [
            ["id", "INT 自增 PK", "指标ID"],
            ["sensor_code", "VARCHAR(30) UNIQUE", "TEMP/HUMI/PM25/CO2/PH/TURBIDITY/DO/NOISE"],
            ["sensor_name", "VARCHAR(50)", "指标名称"],
            ["unit", "VARCHAR(20)", "单位"],
            ["device_type", "VARCHAR(20)", "适用设备类型，NULL=通用"],
            ["min_range / max_range", "DECIMAL(10,2)", "量程上下限"],
            ["standard_max", "DECIMAL(10,2)", "标准限值（超标判定）"],
        ],
    }),
    ("table", {
        "title": "表 4-3　monitor_data 监测数据表（核心）",
        "header": ["字段", "类型", "说明"],
        "rows": [
            ["id", "BIGINT 自增 PK", "数据ID"],
            ["device_id", "INT", "设备ID（复合索引 idx_device_time）"],
            ["sensor_code", "VARCHAR(30)", "指标编码"],
            ["value", "DECIMAL(10,2)", "监测数值"],
            ["report_time", "DATETIME", "上报时间"],
            ["create_time", "DATETIME", "入库时间"],
        ],
    }),
    ("table", {
        "title": "表 4-4　thresholds 告警阈值表",
        "header": ["字段", "类型", "说明"],
        "rows": [
            ["id", "INT 自增 PK", "阈值ID"],
            ["device_id", "INT NULL", "设备ID，NULL=全局默认"],
            ["sensor_code", "VARCHAR(30)", "指标编码"],
            ["warn_min / warn_max", "DECIMAL(10,2)", "预警下限/上限"],
            ["alarm_min / alarm_max", "DECIMAL(10,2)", "报警下限/上限"],
            ["enabled", "TINYINT", "启用开关"],
            ["update_time", "DATETIME", "更新时间"],
        ],
    }),
    ("table", {
        "title": "表 4-5　alerts 告警记录表（核心）",
        "header": ["字段", "类型", "说明"],
        "rows": [
            ["id", "BIGINT 自增 PK", "告警ID"],
            ["device_id", "INT", "设备ID"],
            ["sensor_code", "VARCHAR(30)", "指标编码"],
            ["level", "VARCHAR(10)", "WARN预警/ALARM报警"],
            ["alert_value", "DECIMAL(10,2)", "触发告警的数值"],
            ["message", "VARCHAR(255)", "告警描述"],
            ["status", "TINYINT", "0未处理 1已处理"],
            ["handle_user / handle_time", "VARCHAR(50)/DATETIME", "处理人/处理时间"],
            ["create_time", "DATETIME", "告警时间"],
        ],
    }),
    ("body", "除上述核心表外，admins 管理员表记录管理员账号与密码，users 用户表记录普通用户信息（用户名、昵称、角色、状态），regions 区域表以 parent_id 支持层级区域划分。系统初始化时写入 3 台演示设备（DEV-AIR-001 教学楼A栋空气质量站、DEV-WTR-001 人工湖水质监测站、DEV-NSE-001 操场噪声监测站）与 8 项监测指标（TEMP、HUMI、PM25、CO2、PH、TURBIDITY、DO、NOISE），并预置 PM25（预警 75、报警 150）与 NOISE（预警 65、报警 75）两条全局阈值。"),
    ("h2", "4.4　关键流程设计"),
    ("h3", "4.4.1　数据上报流程"),
    ("body", "数据上报是系统的核心业务流程。设备或模拟器通过 POST /api/data/report 接口上报数据，后端在单个事务内依次完成：校验设备编号是否存在、逐项写入监测数据、更新设备在线状态与最近上报时间、对各指标进行阈值校验并生成告警。任一环节失败则整个事务回滚，确保不产生脏数据。整个上报流程如图 4-2 所示。"),
    ("fig", "图 4-2　数据上报核心事务流程图（此处插入数据上报流程示意图）"),
    ("h3", "4.4.2　阈值告警流程"),
    ("body", "阈值校验遵循“设备级阈值优先、全局阈值兜底”的原则：首先查询该设备对应指标的启用阈值，若不存在则回退到 device_id 为空的全局默认阈值。校验时依次判断是否超过报警上限、低于报警下限、超过预警上限、低于预警下限，命中则生成对应级别的告警记录。为避免告警风暴，同一设备同一指标同一级别在 30 分钟内不重复生成告警。生成的告警同时通过 WebSocket 广播至前端，提示管理人员及时处理。"),
    # ===================== 第 5 章 系统实现 =====================
    ("h1", "第 5 章　系统实现"),
    ("h2", "5.1　后端实现"),
    ("h3", "5.1.1　统一响应与全局异常处理"),
    ("body", "为规范前后端交互契约，后端定义统一的响应封装 Result<T>，包含 code、message、data 三个字段，code 为 200 表示成功，其他为失败。同时通过 @RestControllerAdvice 定义全局异常处理器，将业务异常（BizException）、参数类型不匹配、参数校验异常与兜底异常统一转换为 {code, message, data} 格式返回，避免异常堆栈直接暴露给前端，提升了系统的健壮性与可维护性。"),
    ("code", "// 统一响应封装（Result<T>）\npublic class Result<T> {\n    private Integer code;\n    private String message;\n    private T data;\n    public static <T> Result<T> ok(T data) { return new Result<>(200, \"success\", data); }\n    public static <T> Result<T> fail(Integer code, String message) { return new Result<>(code, message, null); }\n}"),
    ("h3", "5.1.2　数据上报核心事务"),
    ("body", "数据上报接口位于 DataController，业务实现在 MonitorDataServiceImpl 的 report 方法中。方法以 @Transactional(rollbackFor = Exception.class) 标注，保证整个上报过程的原子性。实现要点如下：（1）校验设备编号与指标数据非空，并按设备编号查询设备，设备不存在时抛出业务异常使事务回滚；（2）遍历指标项，逐条写入监测数据表，并即时通过 WebSocket 广播数据消息；（3）更新设备在线状态与最近上报时间；（4）逐项进行阈值校验并生成告警。核心代码如代码 5-1 所示。"),
    ("code", "// 代码 5-1　数据上报核心事务\n@Transactional(rollbackFor = Exception.class)\npublic void report(String deviceCode, List<Map<String, Object>> items, Date reportTime) {\n    Devices device = devicesDao.selectOne(new QueryWrapper<Devices>()\n            .eq(\"device_code\", deviceCode).last(\"limit 1\"));\n    if (device == null) { throw new BizException(\"设备不存在: \" + deviceCode); }\n    Date now = reportTime != null ? reportTime : new Date();\n    // 1) 写入监测数据并实时广播\n    for (Map<String, Object> item : items) {\n        MonitorData md = new MonitorData();\n        md.setDeviceId(device.getId());\n        md.setSensorCode(item.get(\"sensorCode\").toString());\n        md.setValue(new BigDecimal(item.get(\"value\").toString()));\n        md.setReportTime(now);\n        super.save(md);\n        notifyWebSocketHandler.broadcast(dataJson(device, item, now));\n    }\n    // 2) 更新设备在线状态与最近上报时间\n    Devices upd = new Devices();\n    upd.setId(device.getId()); upd.setStatus(1); upd.setLastReportTime(now);\n    devicesDao.updateById(upd);\n    // 3) 阈值校验并生成告警\n    for (Map<String, Object> item : items) {\n        checkThreshold(device, item.get(\"sensorCode\").toString(),\n                new BigDecimal(item.get(\"value\").toString()), now);\n    }\n}"),
    ("h3", "5.1.3　两级阈值告警引擎"),
    ("body", "阈值校验在 checkThreshold 方法中实现。方法先查设备级阈值，未命中再查全局默认阈值；然后按报警上限、报警下限、预警上限、预警下限的优先级进行判断，命中任一区间则确定告警级别与描述。生成告警前，通过查询最近 30 分钟内是否已存在同一设备、同一指标、同一级别的告警记录来实现防重复；确认无重复后插入告警记录并广播告警消息。核心代码如代码 5-2 所示。"),
    ("code", "// 代码 5-2　两级阈值告警引擎（节选）\nprivate void checkThreshold(Devices device, String sensorCode, BigDecimal value, Date now) {\n    Thresholds thr = thresholdsDao.selectOne(new QueryWrapper<Thresholds>()\n            .eq(\"device_id\", device.getId()).eq(\"sensor_code\", sensorCode)\n            .eq(\"enabled\", 1).last(\"limit 1\"));\n    if (thr == null) { // 回退全局默认阈值\n        thr = thresholdsDao.selectOne(new QueryWrapper<Thresholds>()\n                .isNull(\"device_id\").eq(\"sensor_code\", sensorCode)\n                .eq(\"enabled\", 1).last(\"limit 1\"));\n    }\n    if (thr == null) { return; }\n    String level = null, desc = null;\n    if (thr.getAlarmMax() != null && value.compareTo(thr.getAlarmMax()) > 0) {\n        level = \"ALARM\"; desc = sensorCode + \" 超过报警上限 \" + thr.getAlarmMax() + \"，当前值 \" + value;\n    } else if (thr.getWarnMax() != null && value.compareTo(thr.getWarnMax()) > 0) {\n        level = \"WARN\"; desc = sensorCode + \" 超过预警上限 \" + thr.getWarnMax() + \"，当前值 \" + value;\n    }\n    if (level == null) { return; }\n    // 30 分钟内同设备同指标同级别不重复\n    Calendar cal = Calendar.getInstance(); cal.setTime(now); cal.add(Calendar.MINUTE, -30);\n    Integer recent = alertsDao.selectCount(new QueryWrapper<Alerts>()\n            .eq(\"device_id\", device.getId()).eq(\"sensor_code\", sensorCode)\n            .eq(\"level\", level).gt(\"create_time\", cal.getTime()));\n    if (recent > 0) { return; }\n    Alerts alert = new Alerts();\n    alert.setDeviceId(device.getId()); alert.setSensorCode(sensorCode);\n    alert.setLevel(level); alert.setAlertValue(value);\n    alert.setMessage(desc); alert.setStatus(0); alert.setCreateTime(now);\n    alertsDao.insert(alert);\n    notifyWebSocketHandler.broadcast(alertJson(alert));\n}"),
    ("h3", "5.1.4　数据模拟器"),
    ("body", "为在无真实硬件的情况下演示系统完整流程，系统实现了数据模拟器 DataSimulatorService。模拟器由 @ConditionalOnProperty(prefix = \"simulator\", name = \"enabled\", havingValue = \"true\") 控制启用，可通过配置开关在接入真实硬件时关闭。模拟器使用 @Scheduled 定时任务，默认每 5 秒为一轮，遍历全部启用设备，按设备类型生成符合量程的随机数据并调用数据上报接口，从而自动触发阈值校验与告警演示。以空气质量设备为例，模拟器生成温度（18~35℃）、湿度（30~80%）、PM2.5（0~300，其中 5% 概率产生 150~300 的峰值以演示告警）与二氧化碳（350~1200 ppm）四类数据。"),
    ("code", "// 代码 5-3　数据模拟器定时上报（节选）\n@Scheduled(fixedDelayString = \"${simulator.interval-ms:5000}\")\npublic void simulate() {\n    if (!config.isEnabled()) { return; }\n    List<Devices> devices = devicesDao.selectList(\n            new QueryWrapper<Devices>().ne(\"status\", 2)); // 排除停用设备\n    for (Devices d : devices) {\n        List<Map<String, Object>> items = generateItems(d.getType());\n        monitorDataService.report(d.getDeviceCode(), items, new java.util.Date());\n    }\n}"),
    ("h3", "5.1.5　WebSocket 实时推送"),
    ("body", "系统通过 NotifyWebSocketHandler 管理所有前端 WebSocket 连接。处理器继承 TextWebSocketHandler，使用 ConcurrentHashMap 保存在线会话，连接建立时加入集合、断开时移除。broadcast 方法遍历所有在线会话并发送 JSON 消息，消息类型包括 data（监测数据）与 alert（告警信息）。数据上报与告警生成时均调用 broadcast 实现秒级推送。前端 ws.js 封装了连接管理与消息订阅，支持断线后每 3 秒自动重连，并保留定时轮询作为兜底，保证大屏数据持续刷新。"),
    ("h2", "5.2　前端实现"),
    ("h3", "5.2.1　实时监测大屏"),
    ("body", "实时监测大屏（Dashboard）采用 ENVISION 深色驾驶舱设计语言，以深色渐变背景配合漂浮导航（Floating HUD）构建沉浸式监测界面。页面自上而下依次为：Hero 首屏展示系统整体环境质量指数（AQI）主指标与系统在线状态；数据带展示设备总数、在线设备、今日上报、未处理告警等总览指标；核心区域使用 ECharts Gauge 仪表盘展示关键指标得分，使用平滑实时曲线展示指标变化趋势，使用空间分布图展示各监测点位的地理分布。大屏数据由 WebSocket 驱动秒级更新，同时设置 30 秒定时轮询作为兜底；点击首屏 AQI 指标可跳转历史数据页查看详情。"),
    ("h3", "5.2.2　业务管理模块"),
    ("body", "除监测大屏外，前端还实现设备管理（Devices）、告警管理（Alerts）、阈值管理（Thresholds）、历史数据（History）与用户管理（Users）等业务页面，以及登录页（Login）。各页面基于 Element Plus 组件库实现表格分页、表单弹窗与操作按钮，通过统一封装的 request 工具调用后端 API，接口返回的 {code, message, data} 结构由拦截器统一处理，异常时弹出提示。路由层面通过 Vue Router 配置页面跳转，登录状态由 Pinia 状态管理维护。"),
    ("h2", "5.3　系统界面展示"),
    ("body", "系统运行后，通过浏览器访问前端页面即可使用。登录页完成管理员认证；实时监测大屏展示环境质量总览、仪表盘与实时曲线；设备管理页支持设备的新增、修改、删除与状态查看；告警管理页展示未处理告警并支持处理登记；阈值管理页支持全局与设备级阈值配置；历史数据页支持按条件查询与趋势分析。系统主要界面截图可替换插入此处。"),
    ("fig", "图 5-1　系统登录界面（此处插入登录页截图）"),
    ("fig", "图 5-2　实时监测大屏界面（此处插入监测大屏截图）"),
    ("fig", "图 5-3　告警管理界面（此处插入告警管理页截图）"),
    # ===================== 第 6 章 系统测试 =====================
    ("h1", "第 6 章　系统测试"),
    ("h2", "6.1　测试环境"),
    ("body", "系统测试在 Windows 本机环境完成，后端以 java -jar 方式运行于 8080 端口，前端以 Vite 开发服务器运行于 5173 端口，数据库使用本机 MySQL 8.0。测试采用接口冒烟测试、端到端自动化测试与关键功能单元测试相结合的方式，覆盖接口功能、业务逻辑、实时推送与事务一致性等层面。测试环境配置如表 6-1 所示。"),
    ("table", {
        "title": "表 6-1　测试环境配置",
        "header": ["类别", "配置"],
        "rows": [
            ["操作系统", "Windows 10/11"],
            ["后端", "Spring Boot 2.6.13，Java 8，端口 8080"],
            ["前端", "Vue 3.5 + Vite 5，端口 5173"],
            ["数据库", "MySQL 8.0，库 nep"],
            ["测试工具", "curl、PowerShell 脚本、Node.js WebSocket 客户端、JUnit"],
        ],
    }),
    ("h2", "6.2　接口冒烟测试"),
    ("body", "编写 smoke-test.bat 冒烟测试脚本，在后端运行时依次验证登录、设备分页、指标字典、系统总览、未处理告警、环境质量评分与历史数据 7 个核心接口是否正常返回。测试结果如表 6-2 所示，7 项接口全部通过。"),
    ("table", {
        "title": "表 6-2　接口冒烟测试结果",
        "header": ["序号", "测试接口", "结果"],
        "rows": [
            ["1", "POST /api/auth/login 登录", "通过"],
            ["2", "GET /api/devices/page 设备分页", "通过"],
            ["3", "GET /api/sensors 指标字典", "通过"],
            ["4", "GET /api/stats/overview 系统总览", "通过"],
            ["5", "GET /api/alerts/unhandled 未处理告警", "通过"],
            ["6", "GET /api/stats/quality 环境质量评分", "通过"],
            ["7", "GET /api/data/history 历史数据", "通过"],
        ],
    }),
    ("h2", "6.3　端到端测试"),
    ("body", "编写 e2e-test.ps1 端到端测试脚本，覆盖认证、设备、数据、告警、阈值、统计、用户、指标字典、WebSocket 与前端链路共 10 个模块、36 项断言。脚本自动创建测试设备与数据，并在测试结束后自动清理，避免污染演示数据。主要测试项包括：错误密码登录被拒绝、重复设备编码被拒绝、未知设备上报触发事务回滚、超阈值上报生成 ALARM 告警、30 分钟防重复告警、告警处理状态更新、阈值增删改、WebSocket 实时广播到达、前端页面可访问与 API 代理正常等。测试结果全部通过，核心场景说明如下。"),
    ("table", {
        "title": "表 6-3　端到端测试核心场景结果",
        "header": ["测试模块", "关键断言", "结果"],
        "rows": [
            ["M1 认证", "登录返回 token、错误密码返回 401、登出成功", "通过"],
            ["M2 设备", "新增/修改设备、重复编码拒绝、在线统计", "通过"],
            ["M3 数据", "数据上报、未知设备回滚、最新/历史/趋势/CSV 导出", "通过"],
            ["M4 告警", "超阈值生成 ALARM、30 分钟防重复、处理告警、近 7 天统计", "通过"],
            ["M5 阈值", "全局阈值增删改", "通过"],
            ["M6 统计", "总览字段完整、质量评分、设备上报排行", "通过"],
            ["M7 用户", "新增/修改/禁用/删除用户、重复用户名拒绝", "通过"],
            ["M8 字典", "指标字典 8 项、按类型过滤", "通过"],
            ["M9 WebSocket", "data/alert 消息实时到达", "通过"],
            ["M10 前端链路", "首页可访问、API 代理正常、页面可加载", "通过"],
        ],
    }),
    ("h2", "6.4　关键功能测试"),
    ("body", "针对系统最核心的告警引擎与事务一致性，进行了专项单元测试：阈值告警测试通过上报不同量级的监测值，验证了 WARN 与 ALARM 两个告警级别的判定逻辑正确（单元测试用例 950 触发 WARN、1100 触发 ALARM）；事务回滚测试对不存在的设备进行上报，验证接口报错且监测数据表中不产生任何脏数据；实时推送测试使用 Node.js WebSocket 客户端订阅消息端点，实测数据上报与告警产生时 data/alert 广播均能及时到达。此外，mvn package 与 vite build 均构建成功。关键功能测试结果如表 6-4 所示。"),
    ("table", {
        "title": "表 6-4　关键功能测试结果",
        "header": ["测试项", "测试方法", "结果"],
        "rows": [
            ["接口功能", "curl 全链路冒烟（smoke-test.bat 7 项）", "7/7 通过"],
            ["端到端", "e2e-test.ps1（10 模块 36 项断言）", "全部通过"],
            ["阈值告警", "单元测试（WARN/ALARM 两档判定）", "通过"],
            ["事务回滚", "单元测试（设备不存在上报）", "无脏数据"],
            ["实时推送", "Node WebSocket 客户端实测", "data/alert 广播到达"],
            ["系统构建", "mvn package / vite build", "成功"],
        ],
    }),
    ("h2", "6.5　测试结论"),
    ("body", "综合各项测试结果，系统在接口功能、业务逻辑、实时性与数据一致性等方面均表现良好：冒烟测试与端到端测试全部通过，阈值告警判定准确，事务回滚可靠，WebSocket 实时推送稳定到达，前端各页面可正常加载与交互。测试结果表明系统设计合理、实现正确，满足了需求分析阶段提出的功能与非功能要求。"),
    # ===================== 第 7 章 总结与展望 =====================
    ("h1", "第 7 章　总结与展望"),
    ("h2", "7.1　工作总结"),
    ("body", "本文设计并实现了一套多合一环境监测保护系统，完成了从需求分析、系统设计、编码实现到测试验证的全流程开发工作。系统基于 Spring Boot 与 Vue 3 前后端分离架构，覆盖空气质量、水质与噪声三类共八项监测指标，实现了环境数据从自动采集、实时展示、阈值告警到告警处理的完整闭环。"),
    ("body", "系统的主要工作与成果包括：（1）设计了指标字典驱动的数据模型，通过 sensors 表登记指标编码、单位与标准限值，使新增监测类型无需修改核心代码即可扩展；（2）实现了基于事务机制的数据上报流程，保证监测数据、设备状态与告警生成的原子性与一致性；（3）设计了 WARN/ALARM 两级阈值告警引擎与 30 分钟防重复机制，既保证告警及时有效又避免告警风暴；（4）基于 WebSocket 实现数据与告警的秒级实时推送，前端免轮询即可实时刷新；（5）实现了 ENVISION 深色驾驶舱风格的实时监测大屏与完整的业务管理界面；（6）通过接口冒烟测试、端到端测试与关键功能测试验证了系统的正确性与可靠性。"),
    ("h2", "7.2　不足与展望"),
    ("body", "系统目前仍存在一些不足，主要包括：数据采集依赖软件模拟器，尚未接入真实传感器硬件；认证采用简单令牌机制，安全强度有待提升；监测数据量增大后，MySQL 关系型存储在高频写入与时序查询方面存在性能瓶颈；环境质量评分仅基于单指标与标准限值的简单比值，维度较为单一。"),
    ("body", "后续工作可从以下方向展开：（1）接入真实硬件，通过 MQTT 协议实现低功耗传感器数据上报，完善设备接入层；（2）引入 Spring Security 与 JWT 完善认证授权机制；（3）引入 InfluxDB、TDengine 等时序数据库或消息队列，支撑更大规模数据的采集与存储；（4）基于历史监测数据构建环境质量预测模型，实现从“事后告警”向“事前预警”的转变；（5）开发移动端应用，进一步提升监测的便捷性。"),
]

# 参考文献（GB/T 7714-2015）
REFERENCES = [
    "GB/T 7714—2015 信息与文献　参考文献著录规则[S]. 北京: 中国标准出版社, 2015.",
    "中华人民共和国环境保护部. 环境空气质量标准: GB 3095—2012[S]. 北京: 中国环境科学出版社, 2012.",
    "中华人民共和国环境保护部. 声环境质量标准: GB 3096—2008[S]. 北京: 中国环境科学出版社, 2008.",
    "中华人民共和国环境保护部. 环境空气质量指数（AQI）技术规定（试行）: HJ 633—2012[S]. 北京: 中国环境科学出版社, 2012.",
    "中华人民共和国生态环境部. 地表水水质自动监测站（常规五参数、CODMn、NH3-N、TP、TN）运行维护技术规范: HJ 915.3—2024[S]. 北京: 中国环境出版集团, 2024.",
    "中华人民共和国生态环境部. 强化数智化理念，扎实推进国家生态环境监测网络转型任务落实[EB/OL]. (2025-03-28)[2026-08-31]. https://www.mee.gov.cn/zcwj/zcjd/202503/t20250328_1104874.shtml.",
    "Fette I, Melnikov A. The WebSocket Protocol: RFC 6455[S/OL]. (2011-12)[2026-08-31]. https://www.rfc-editor.org/rfc/rfc6455.",
    "Li D, Mei H, Shen Y, et al. ECharts: A declarative framework for rapid construction of web-based visualization[J]. Visual Informatics, 2018, 2(2): 136-146.",
    "王珊, 萨师煊. 数据库系统概论[M]. 5版. 北京: 高等教育出版社, 2014.",
    "周志明. 深入理解Java虚拟机: JVM高级特性与最佳实践[M]. 3版. 北京: 机械工业出版社, 2019.",
    "Spring Boot Reference Documentation[EB/OL]. [2026-08-31]. https://docs.spring.io/spring-boot/docs/current/reference/.",
    "MyBatis-Plus 官方文档[EB/OL]. [2026-08-31]. https://baomidou.com.",
    "Vue.js 官方文档[EB/OL]. [2026-08-31]. https://cn.vuejs.org.",
    "Apache ECharts 官方文档[EB/OL]. [2026-08-31]. https://echarts.apache.org.",
]
