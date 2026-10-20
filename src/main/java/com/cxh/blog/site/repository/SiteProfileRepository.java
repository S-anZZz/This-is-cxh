package com.cxh.blog.site.repository;

import com.cxh.blog.site.domain.SiteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 站点档案仓储。上层只依赖 Spring Data 的抽象，
 * 因此 H2 / MySQL 甚至换成其他存储都不影响服务层代码。
 */
public interface SiteProfileRepository extends JpaRepository<SiteProfile, Long> {
}
