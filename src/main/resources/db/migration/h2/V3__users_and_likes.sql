-- 用户体系与点赞。
-- 设计取舍：
--   * 角色用 varchar + check（与项目既有枚举风格一致），只区分 USER / ADMIN；
--   * 一对一唯一约束放在数据库层（用户名、邮箱各自唯一），应用层只做友好提示；
--   * 点赞用 (post_id, user_id) 唯一约束保证「一人一赞」，而不是靠代码判重。
create table app_user (
    id            bigint auto_increment primary key,
    username      varchar(40)  not null unique,
    email         varchar(160) not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(60),
    role          varchar(20)  not null check (role in ('USER', 'ADMIN')),
    enabled       boolean      not null default true,
    created_at    timestamp,
    updated_at    timestamp
);

-- 作者列可空：升级前已有的文章没有作者，保持可空避免回填脚本
alter table post add column author_id bigint;

create index idx_post_author on post (author_id);

create table post_like (
    id         bigint auto_increment primary key,
    post_id    bigint not null,
    user_id    bigint not null,
    created_at timestamp,
    constraint uk_post_like unique (post_id, user_id),
    constraint fk_post_like_post foreign key (post_id) references post (id) on delete cascade,
    constraint fk_post_like_user foreign key (user_id) references app_user (id) on delete cascade
);
