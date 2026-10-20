package com.cxh.blog.common.api;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页结果。与具体持久化技术无关，屏蔽 Spring Data 的 Page 类型，
 * 使控制器与模板不被持久化框架污染。
 *
 * @param items      当前页数据
 * @param page       页码，从 0 开始
 * @param size       每页条数
 * @param total      总条数
 * @param totalPages 总页数
 */
public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(List.copyOf(items), page, size, total, totalPages);
    }

    public static <S, T> PageResult<T> of(List<S> source, int page, int size, long total, Function<S, T> mapper) {
        return of(source.stream().map(mapper).toList(), page, size, total);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
