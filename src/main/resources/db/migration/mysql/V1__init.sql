-- =====================================================================
-- cxh 博客：MySQL 初始化脚本
--
-- 与 h2/V1__init.sql 结构一一对应，差异只在数据库方言：
--   * 主键自增：auto_increment
--   * 时间类型：datetime(6)
--   * 大文本：text
--   * 布尔：bit
--   * 显式 utf8mb4，避免中文与 emoji 出现乱码
--
-- 注意：列类型必须与 Hibernate 期望一致，否则 ddl-auto=validate 会启动失败。
-- =====================================================================

create table site_profile
(
    id                bigint      not null,
    site_title        varchar(80) not null,
    site_subtitle     varchar(160),
    author_name       varchar(60) not null,
    author_avatar_url varchar(400),
    location          varchar(80),
    github_url        varchar(400),
    email             varchar(160),
    footer_text       varchar(240),
    icp_number        varchar(80),
    intro_markdown    text,
    custom_css        text,
    theme_preset      varchar(40) check (theme_preset in
                                         ('ELEGANT_LIGHT', 'MIDNIGHT_DARK', 'WARM_PAPER', 'OCEAN_BLUE',
                                          'FOREST_GREEN', 'MONO_PRINT')),
    color_primary     varchar(9),
    color_accent      varchar(9),
    color_background  varchar(9),
    color_surface     varchar(9),
    color_text        varchar(9),
    color_muted       varchar(9),
    font_heading      varchar(40) check (font_heading in
                                         ('SYSTEM_SANS', 'HUMANIST_SANS', 'MODERN_SERIF', 'CLASSIC_SERIF', 'MONO')),
    font_body         varchar(40) check (font_body in
                                         ('SYSTEM_SANS', 'HUMANIST_SANS', 'MODERN_SERIF', 'CLASSIC_SERIF', 'MONO')),
    base_font_size    integer,
    content_width     integer,
    corner_radius     integer,
    sticky_header     bit,
    show_hero         bit,
    created_at        datetime(6),
    updated_at        datetime(6),
    constraint pk_site_profile primary key (id)
) engine = InnoDB
  default charset = utf8mb4;

create table post
(
    id               bigint       not null auto_increment,
    title            varchar(200) not null,
    slug             varchar(220) not null,
    summary          varchar(500),
    content_markdown text,
    cover_image_url  varchar(400),
    status           varchar(20)  not null check (status in ('DRAFT', 'PUBLISHED')),
    published_at     datetime(6),
    views            bigint       not null,
    created_at       datetime(6),
    updated_at       datetime(6),
    constraint pk_post primary key (id),
    constraint uk_post_slug unique (slug)
) engine = InnoDB
  default charset = utf8mb4;

create table post_tag
(
    post_id bigint      not null,
    tag     varchar(30) not null,
    constraint pk_post_tag primary key (post_id, tag),
    constraint fk_post_tag_post foreign key (post_id) references post (id) on delete cascade
) engine = InnoDB
  default charset = utf8mb4;
