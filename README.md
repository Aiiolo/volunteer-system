# 志愿服务系统后端

本目录是“三江学院志愿者服务系统”的 Spring Boot 后端，基于 JDK 17、Spring Boot 3、Spring Security、JWT、Spring Data JPA、MySQL 8 和 Redis 7 构建。

## 1. 技术栈

- JDK 17
- Spring Boot 3.3.5
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation
- MySQL 8
- Redis 7
- JWT
- Maven

## 2. 目录结构

```text
backend
├── pom.xml
└── src/main
    ├── java/com/sanjuan/volunteer
    │   ├── VolunteerSystemApplication.java
    │   ├── common              # 统一响应
    │   ├── config              # 安全配置
    │   ├── controller          # 三端 REST API
    │   ├── dto                 # 请求/响应 DTO
    │   ├── entity              # JPA 实体
    │   ├── exception           # 全局异常处理
    │   ├── repository          # 数据访问层
    │   ├── security            # JWT 和当前用户
    │   └── service             # 业务逻辑
    └── resources
        ├── application.yml
        └── db
            ├── schema.sql      # 建表脚本
            └── data.sql        # 初始化演示数据
```

## 3. 数据库初始化

先在 MySQL 中执行：

```sql
source src/main/resources/db/schema.sql;
source src/main/resources/db/data.sql;
```

或手动打开两个 SQL 文件依次执行。

默认库名：

```text
volunteer_system
```

## 4. 修改配置

打开 `src/main/resources/application.yml`，按本机环境修改 MySQL 和 Redis 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/volunteer_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
```

开发环境默认 JWT 密钥可直接使用；生产环境应通过环境变量 `JWT_SECRET` 覆盖。

## 5. 启动项目

在 `backend` 目录执行：

```bash
mvn spring-boot:run
```

服务默认运行在：

```text
http://localhost:8080
```

## 6. 初始化账号

`data.sql` 中内置了 3 个演示账号，密码均为：

```text
123456
```

| 账号 | 角色 | 说明 |
|---|---|---|
| admin | ADMIN | 管理员 |
| organizer | ORGANIZER | 活动负责人 |
| volunteer | VOLUNTEER | 志愿者 |

## 7. 登录接口

### 志愿者登录

```http
POST /api/v1/volunteer/auth/login
Content-Type: application/json
```

请求体：

```json
{
  "username": "volunteer",
  "password": "123456"
}
```

响应中会返回 JWT：

```json
{
  "success": true,
  "message": "success",
  "data": {
    "token": "xxx",
    "userId": 3,
    "username": "volunteer",
    "role": "VOLUNTEER"
  }
}
```

后续受保护接口需要携带：

```http
Authorization: Bearer <token>
```

## 8. 核心接口

### 志愿者端

| 方法 | 路径 | 功能 |
|---|---|---|
| POST | `/api/v1/volunteer/auth/register` | 志愿者注册 |
| POST | `/api/v1/volunteer/auth/login` | 登录 |
| GET | `/api/v1/volunteer/activities` | 查看可报名活动 |
| POST | `/api/v1/volunteer/applications` | 活动报名 |
| GET | `/api/v1/volunteer/applications/status` | 查看报名状态 |
| GET | `/api/v1/volunteer/activities/my` | 我的活动 |
| POST | `/api/v1/volunteer/attendance/check-in` | 签到 |
| POST | `/api/v1/volunteer/attendance/check-out` | 签退 |
| GET | `/api/v1/volunteer/profile` | 个人中心 |
| PUT | `/api/v1/volunteer/profile` | 修改个人资料 |

### 管理员端

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/api/v1/admin/volunteers` | 志愿者列表 |
| POST | `/api/v1/admin/volunteers/audit` | 志愿者注册审核 |
| PUT | `/api/v1/admin/volunteers/{id}` | 修改志愿者信息 |
| POST | `/api/v1/admin/activities` | 发布/创建活动 |
| PUT | `/api/v1/admin/activities/{id}` | 修改活动 |
| DELETE | `/api/v1/admin/activities/{id}` | 删除活动 |
| GET | `/api/v1/admin/activities/{id}/applications` | 查看活动报名 |
| POST | `/api/v1/admin/activities/applications/audit` | 审核报名 |
| GET | `/api/v1/admin/dashboard/stats` | 数据看板 |

### 活动负责人端

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/api/v1/organizer/my-activities` | 查看自己负责的活动 |
| POST | `/api/v1/organizer/attendance/assist-check` | 协助签到/签退 |
| PUT | `/api/v1/organizer/attendance/{id}/evaluate` | 活动后评价志愿者 |
| POST | `/api/v1/organizer/activities/{id}/summary` | 提交活动总结 |

## 9. 编译检查

```bash
mvn -q -DskipTests compile
```

当前工程已通过编译。
