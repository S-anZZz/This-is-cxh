package com.cxh.blog.bootstrap;

import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.service.PostService;
import com.cxh.blog.site.service.SiteProfileService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动期初始化。这是整个工程中<b>唯一</b>允许同时依赖多个业务域的组件，
 * 因为「同时准备站点档案与示例文章」本身就是一项跨域的应用级任务。
 *
 * <p>所有写入都走各域的服务接口，不直接接触仓储与实体。
 */
@Component
public class BlogSeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BlogSeedData.class);

    /** 相册图片目录（静态资源），与 src/main/resources/static/images/gallery 对应。 */
    private static final String GALLERY = "/images/gallery/";

    /** 缩略图目录，尺寸更小，用于列表展示。 */
    private static final String THUMBNAILS = GALLERY + "thumbs/";

    private final SiteProfileService siteProfileService;
    private final PostService postService;
    private final SeedProperties seedProperties;

    public BlogSeedData(SiteProfileService siteProfileService,
                        PostService postService,
                        SeedProperties seedProperties) {
        this.siteProfileService = siteProfileService;
        this.postService = postService;
        this.seedProperties = seedProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        siteProfileService.ensureInitialized();
        log.info("站点档案就绪，当前主题：{}", siteProfileService.current().theme().getPreset().getLabel());

        if (!seedProperties.isEnabled()) {
            return;
        }
        if (postService.stats().total() > 0) {
            return;
        }
        samplePosts().forEach(form -> {
            var saved = postService.save(form);
            log.info("已写入示例文章：{}", saved.title());
        });
    }

    private List<PostForm> samplePosts() {
        return List.of(
                post("欢迎来到我的博客",
                        "这台博客是怎么搭起来的，以及你可以在这里做什么。",
                        "Blog,Spring Boot",
                        """
                        # 欢迎 👋

                        这是一个基于 **Spring Boot 3** 的个人博客，风格与简介都可以在后台自由编辑。

                        ## 你能做什么

                        1. 进入 `/admin`，用配置里的账号登录；
                        2. 打开 **外观设置**，拖动取色器、切换预置主题，右侧预览会实时刷新；
                        3. 改完点保存，整站风格立刻生效 —— 不需要重新打包，也不需要重启。

                        ## 风格自由度

                        | 可编辑项 | 说明 |
                        | --- | --- |
                        | 预置主题 | 6 套配色，一键套用 |
                        | 6 个颜色令牌 | 主色、辅助色、背景、卡片、正文、次要文字 |
                        | 字体 | 标题与正文字体分别选择 |
                        | 排版 | 字号、内容宽度、圆角、吸顶导航 |
                        | 自定义 CSS | 想更深地改造时，直接写 CSS |
                        """),
                post("高内聚低耦合：这个小项目的落地清单",
                        "从分包、依赖方向、数据契约三个角度，说明本项目如何组织代码。",
                        "架构,Spring Boot,设计",
                        """
                        # 高内聚低耦合的落地清单

                        架构原则如果只停留在 PPT 上，代码很快就会失控。下面是我在这个项目里真正执行的规则。

                        ## 1. 按业务能力分包，而不是按技术分层

                        ```
                        site/   站点域：站点身份 + 简介 + 主题风格
                        post/   文章域：写作、发布、归档
                        common/ 通用能力：响应包装、异常、渲染、文本工具
                        web/    页面装配：首页、归档、后台概览
                        ```

                        每个业务域内部再分 `domain / repository / service / web`，
                        域内高内聚，域间零引用。

                        ## 2. 依赖方向单向向内

                        ```
                        web / bootstrap  →  业务域  →  common
                        ```

                        `post` 不认识 `site`，`site` 也不认识 `post`。
                        需要同时用到两者时，由 `web` 层装配 —— 依赖关系永远是可预测的。

                        ## 3. 用读模型隔离展示层

                        模板不接触实体。`SiteProfileView` / `PostSummary` / `PostDetail`
                        就是页面能看到的全部世界，字段随页面需求演进，实体则随业务不变量演进。

                        ## 4. 把变化点收敛成接口

                        - `MarkdownRenderer` —— 换渲染器不改业务代码；
                        - `ThemeStyleRenderer` —— 风格表达方式可整体替换；
                        - `SiteProfileService` / `PostService` —— 控制器只依赖接口。

                        > 判据很简单：**改一个需求，需要同时打开的文件越少，内聚越高。**
                        """),
                post("Markdown 写作速查",
                        "标题、列表、表格、引用与代码块的最小示例。",
                        "Markdown,写作",
                        """
                        # Markdown 速查

                        ## 文本

                        **加粗**、*斜体*、`行内代码`、[链接](https://spring.io)。

                        ## 列表

                        - 无序项
                        - 另一个无序项

                        1. 有序项
                        2. 另一个有序项

                        ## 引用与代码

                        > 引用块适合放结论。

                        ```java
                        @Service
                        public class DemoService {
                            public String hello() {
                                return "hello cxh";
                            }
                        }
                        ```

                        ## 表格

                        | 语法 | 效果 |
                        | --- | --- |
                        | `# 标题` | 一级标题 |
                        | `> 文本` | 引用块 |

                        正文中的原始 HTML 会被转义 —— 想改样式，请到「外观设置」里写 CSS。
                        """),
                post("相册 · 生活片段",
                        "随手拍的几个片段：校园的路、宿舍的床、工位前的显示器。",
                        "相册,生活",
                        """
                        都是随手拍的，没什么构图，胜在真实。点缩略图可以看原图。

                        ## 校园

                        """ + album(
                        "bc01.jpg", "校园路上的树影与自行车") + """
                        ## 宿舍与工位

                        """ + album(
                        "cxh.jpg", "宿舍里笑到模糊",
                        "cxh02.jpg", "被窝里刷手机",
                        "cxh03.jpg", "工位前的显示器",
                        "cxh04.jpg", "蓝色房门前的背影",
                        "cxh05.jpg", "深夜的一张糊自拍")),
                post("表情包与整活",
                        "收藏的几个表情包，外加一张 AI 生成的整活图（图片自带水印）。",
                        "表情包,相册",
                        """
                        写代码卡住的时候，看一眼这些能续命。

                        """ + album(
                        "cgx.jpg", "表情包：瞪眼的熊",
                        "cgx02.jpg", "表情包：咧嘴的熊",
                        "cgx03.jpg", "表情包：熊和松鼠",
                        "NL1.jpg", "蜜瓜头套的角色") + """
                        ## AI 整活

                        """ + album(
                        "ysgn.jpg", "AI 生成的「野生狗奶」牛奶盒（图片左上角带 AI 生成水印）")));
    }

    /**
     * 相册条目：Markdown 的「图片套链接」写法 {@code [![说明](缩略图)](原图)}，
     * 点击缩略图即可查看原图，不需要任何前端脚本。
     *
     * @param fileAndAlt 文件名与替代文本交替出现，顺序即展示顺序
     */
    private static String album(String... fileAndAlt) {
        StringBuilder markdown = new StringBuilder();
        for (int i = 0; i + 1 < fileAndAlt.length; i += 2) {
            String file = fileAndAlt[i];
            String alt = fileAndAlt[i + 1];
            markdown.append("[![").append(alt).append("](").append(THUMBNAILS).append(file).append(")](")
                    .append(GALLERY).append(file).append(")\n\n");
        }
        return markdown.toString();
    }

    private PostForm post(String title, String summary, String tags, String markdown) {
        PostForm form = new PostForm();
        form.setTitle(title);
        form.setSummary(summary);
        form.setTagsText(tags);
        form.setContentMarkdown(markdown);
        form.setStatus(PostStatus.PUBLISHED);
        return form;
    }
}
