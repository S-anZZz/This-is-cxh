package com.cxh.blog.web.dto;

import com.cxh.blog.post.dto.PostSummary;
import java.util.List;

/**
 * 归档分组读模型。分组逻辑属于「页面装配」职责，
 * 因此放在 web 层，而不是污染文章域或塞进模板表达式。
 */
public record ArchiveGroup(String year, List<PostSummary> posts) {
}
