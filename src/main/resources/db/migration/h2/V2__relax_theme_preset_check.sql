-- 新增主题预设 SAKURA_PINK 后，V1 里那条「匿名」CHECK 仍然只允许旧的 6 个值，
-- 保存新预设会被数据库直接拒绝（Check constraint violation）。
--
-- V1 里的约束是匿名的，H2 按建列顺序自动命名为 CONSTRAINT_B（theme_preset 是表中第一条 check）。
-- 这里把它替换成具名约束 chk_theme_preset：一是纳入新值，二是以后再改就有确定的名字可引用了。
ALTER TABLE site_profile DROP CONSTRAINT IF EXISTS CONSTRAINT_B;

ALTER TABLE site_profile ADD CONSTRAINT chk_theme_preset CHECK (theme_preset IN (
    'ELEGANT_LIGHT', 'MIDNIGHT_DARK', 'WARM_PAPER', 'OCEAN_BLUE',
    'FOREST_GREEN', 'MONO_PRINT', 'SAKURA_PINK'
));
