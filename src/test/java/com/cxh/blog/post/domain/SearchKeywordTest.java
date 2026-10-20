package com.cxh.blog.post.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 搜索关键字值对象测试：重点是 LIKE 通配符转义，
 * 否则用户输入一个 {@code %} 就会命中全部文章（既是性能问题也是语义错误）。
 */
class SearchKeywordTest {

    @Test
    @DisplayName("普通关键词被包裹为 contains 模式并转小写")
    void wrapsKeyword() {
        SearchKeyword keyword = SearchKeyword.of("Spring Boot");

        assertThat(keyword.value()).isEqualTo("Spring Boot");
        assertThat(keyword.pattern()).isEqualTo("%spring boot%");
        assertThat(keyword.isMeaningful()).isTrue();
    }

    @Test
    @DisplayName("LIKE 通配符与转义符本身都会被转义")
    void escapesWildcards() {
        assertThat(SearchKeyword.of("100%").pattern()).isEqualTo("%100!%%");
        assertThat(SearchKeyword.of("a_b").pattern()).isEqualTo("%a!_b%");
        assertThat(SearchKeyword.of("a!b").pattern()).isEqualTo("%a!!b%");
    }

    @Test
    @DisplayName("空白与超长输入被规范化")
    void normalizesInput() {
        assertThat(SearchKeyword.of("  cxh   blog  ").value()).isEqualTo("cxh blog");
        assertThat(SearchKeyword.of("x".repeat(200)).value()).hasSize(SearchKeyword.MAX_LENGTH);
    }

    @Test
    @DisplayName("空关键词与纯符号关键词都不会触发查询")
    void detectsEmptyKeyword() {
        assertThat(SearchKeyword.of(null).isEmpty()).isTrue();
        assertThat(SearchKeyword.of("   ").isEmpty()).isTrue();
        assertThat(SearchKeyword.of("%%%").isMeaningful()).isFalse();
        assertThat(SearchKeyword.of("中文").isMeaningful()).isTrue();
    }
}
