package com.murasame.util;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.Arrays;
import java.util.List;

public class BlogHtmlUtil {

	// 配置 CommonMark 扩展
	private static final List<Extension> EXTENSIONS = Arrays.asList(
			TablesExtension.create(),
			StrikethroughExtension.create(),
			TaskListItemsExtension.create(),
			AutolinkExtension.create()
	);

	// 配置 Parser 和 Renderer
	private static final Parser PARSER = Parser.builder()
			.extensions(EXTENSIONS)
			.build();

	private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
			.extensions(EXTENSIONS)
			.build();

	// 扩展的 Safelist，允许更多标签
	private static final Safelist SAFE = Safelist.relaxed()
			.addTags("input", "del", "ins", "mark", "kbd", "sub", "sup")
			.addAttributes("input", "type", "checked", "disabled")
			.addAttributes("table", "class")
			.addAttributes("td", "align", "colspan", "rowspan")
			.addAttributes("th", "align", "colspan", "rowspan")
			.addAttributes("img", "width", "height", "style")
			.addAttributes("code", "class")
			.addAttributes("pre", "class");

	// markdown to clean html
	public static String toHtml(String markdown){
		if (markdown == null || markdown.isEmpty()) {
			return "";
		}
		Node document = PARSER.parse(markdown);
		String html = RENDERER.render(document);
		return Jsoup.clean(html, SAFE);
	}

	// 提取博客摘要（先转换为HTML，再去除标签，截取前30个字符）
	public static String extractBrief(String markdown) {
		if (markdown == null || markdown.isEmpty()) {
			return "";
		}

		// 1. 将 Markdown 转换为 HTML
		Node document = PARSER.parse(markdown);
		String html = RENDERER.render(document);

		// 2. 去除所有 HTML 标签，得到纯文本
		String text = Jsoup.clean(html, Safelist.none());

		// 3. 去除多余的空白字符
		text = text.replaceAll("\\s+", " ").trim();

		// 4. 截取前 30 个字符（符合 BlogBriefVO 设计）
		if (text.length() > 30) {
			return text.substring(0, 30);
		}
		return text;
	}
}
