-- 同 h2 版本：把 V1 的匿名 CHECK 换成具名约束并纳入 SAKURA_PINK。
-- MySQL 8 的匿名 check 按 <表名>_chk_<序号> 命名，theme_preset 是第一条，即 site_profile_chk_1。
-- MySQL 不支持 DROP CHECK IF EXISTS：名字不符会直接报错，比「静默保留旧约束」更安全。
ALTER TABLE site_profile DROP CHECK site_profile_chk_1;

ALTER TABLE site_profile ADD CONSTRAINT chk_theme_preset CHECK (theme_preset IN (
    'ELEGANT_LIGHT', 'MIDNIGHT_DARK', 'WARM_PAPER', 'OCEAN_BLUE',
    'FOREST_GREEN', 'MONO_PRINT', 'SAKURA_PINK'
));
