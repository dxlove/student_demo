create table t_course
(
    id          bigint auto_increment comment '自增主键ID'
        primary key,
    course_code char(6)                             not null comment '课程代码',
    course_name varchar(100)                        not null comment '课程名称',
    credit      tinyint unsigned                    not null comment '学分(1-6)',
    course_type varchar(10)                         not null comment '课程类型',
    dept_name   varchar(30)                         not null comment '开课院系名称',
    course_desc text                                null comment '课程描述',
    created_at  timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at  timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后更新时间',
    created_by  varchar(36)                         not null comment '创建人',
    updated_by  varchar(36)                         not null comment '最后更新人',
    constraint course_code
        unique (course_code)
)
    comment '课程信息';

create index idx_dept
    on t_course (dept_name);

create table t_grade
(
    id         bigint auto_increment comment '自增主键ID'
        primary key,
    student_id char(12)                            not null comment '学号',
    course_id  bigint                              not null comment '课程表主键',
    semester   char(5)                             not null comment '学期(如2023S)',
    score      decimal(4, 1) unsigned              null comment '百分制成绩',
    created_at timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后更新时间',
    created_by varchar(36)                         not null comment '创建人',
    updated_by varchar(36)                         not null comment '最后更新人'
)
    comment '成绩记录';

create index idx_course_semester
    on t_grade (course_id, semester);

create index idx_student_semester
    on t_grade (student_id, semester);

create table t_student
(
    id              bigint auto_increment comment '自增主键ID'
        primary key,
    student_id      char(12)                            not null comment '学号',
    id_card         char(18)                            not null comment '身份证号',
    name            varchar(30)                         not null comment '姓名',
    dept_name       varchar(30)                         not null comment '所属院系名称',
    enrollment_year year                                not null comment '入学年份',
    created_at      timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at      timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后更新时间',
    created_by      varchar(36)                         not null comment '创建人',
    updated_by      varchar(36)                         not null comment '最后更新人',
    constraint id_card
        unique (id_card),
    constraint student_id
        unique (student_id)
)
    comment '学生基本信息';

create table t_user_auth
(
    id             bigint auto_increment comment '自增主键ID'
        primary key,
    student_id     char(12)                                            not null comment '关联学号',
    username       varchar(20)                                         not null comment '登录账号',
    password_hash  char(60)                                            not null comment 'BCrypt加密密码',
    account_status enum ('ACTIVE', 'LOCKED') default 'ACTIVE'          null comment '账号状态',
    last_login     datetime                                            null comment '最后登录时间',
    created_at     timestamp                 default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at     timestamp                 default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '最后更新时间',
    created_by     varchar(36)                                         not null comment '创建人',
    updated_by     varchar(36)                                         not null comment '最后更新人',
    constraint student_id
        unique (student_id),
    constraint username
        unique (username)
)
    comment '用户登录认证';

create index idx_student_id
    on t_user_auth (student_id);

create index idx_username
    on t_user_auth (username);


