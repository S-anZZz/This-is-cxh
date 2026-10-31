package com.cxh.blog.user.repository;

import com.cxh.blog.user.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 账号仓储。用户名与邮箱都按「忽略大小写」查询：
 * 用户登录时不该因为大小写不同而被判成另一个账号。
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
