-- 与 h2 版本等价，仅类型写法遵循 MySQL 习惯。
create table app_user (
    id            bigint auto_increment primary key,
    username      varchar(40)  not null unique,
    email         varchar(160) not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(60),
    role          varchar(20)  not null check (role in ('USER', 'ADMIN')),
    enabled       boolean      not null default true,
    created_at    datetime(6),
    updated_at    datetime(6)
) engine = InnoDB default charset = utf8mb4;

alter table post add column author_id bigint;

create index idx_post_author on post (author_id);

create table post_like (
    id         bigint auto_increment primary key,
    post_id    bigint not null,
    user_id    bigint not null,
    created_at datetime(6),
    constraint uk_post_like unique (post_id, user_id),
    constraint fk_post_like_post foreign key (post_id) references post (id) on delete cascade,
    constraint fk_post_like_user foreign key (user_id) references app_user (id) on delete cascade
) engine = InnoDB default charset = utf8mb4;
