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

import com.murasame.domain.vo.BlogBriefVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	// 扩展的 Safelist，允许更多标签（流程图、时序图、数学公式的容器元素）
	private static final Safelist SAFE = Safelist.relaxed()
			.addTags("input", "del", "ins", "mark", "kbd", "sub", "sup")
			.addAttributes("input", "type", "checked", "disabled")
			.addAttributes("table", "class")
			.addAttributes("td", "align", "colspan", "rowspan")
			.addAttributes("th", "align", "colspan", "rowspan")
			.addAttributes("img", "width", "height", "style")
			.addAttributes("code", "class")
			.addAttributes("pre", "class")
			.addAttributes("div", "class")
			.addAttributes("span", "class");

	// markdown to clean html（保护数学块 → CommonMark → Jsoup 清洗 → 还原数学块）
	public static String toHtml(String markdown){
		if (markdown == null || markdown.isEmpty()) {
			return "";
		}
		Map<String, String> mathBlocks = new LinkedHashMap<>();
		String protectedMd = protectMathBlocks(markdown, mathBlocks);
		Node document = PARSER.parse(protectedMd);
		String html = RENDERER.render(document);
		String cleaned = Jsoup.clean(html, SAFE);
		return restoreMathBlocks(cleaned, mathBlocks);
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

	// Markdown 图片语法正则：![alt](url)
	private static final Pattern MARKDOWN_IMG_PATTERN =
			Pattern.compile("!\\[.*?\\]\\((.*?)\\)");

	// 数学块保护：先匹配 $$...$$（块级），再匹配 $...$（行内）
	private static final Pattern MATH_DISPLAY = Pattern.compile("\\$\\$(.+?)\\$\\$", Pattern.DOTALL);
	private static final Pattern MATH_INLINE = Pattern.compile("(?<!\\$)\\$(?!\\$)(.+?)(?<!\\$)\\$(?!\\$)");

	// 占位符前缀/后缀，用于在 Markdown 处理前替换数学块
	private static final String PH_DISPLAY = "D"; // display math placeholder prefix
	private static final String PH_INLINE  = "I"; // inline math placeholder prefix
	private static final String PH_SUFFIX  = "";   // placeholder suffix

	// 从 Markdown 内容中提取第一张图片 URL，作为封面自动降级方案
	public static String extractFirstImage(String markdown) {
		if (markdown == null || markdown.isEmpty()) {
			return null;
		}
		Matcher m = MARKDOWN_IMG_PATTERN.matcher(markdown);
		return m.find() ? m.group(1) : null;
	}

	// 将数学块（$$...$$ 和 $...$）替换为占位符，避免 CommonMark 将其中的 _ * \ 等字符误解析
	// 返回替换后的文本，并将原始数学文本存入 mathBlocks（key=占位符，value=原始文本含 $ 分隔符）
	private static String protectMathBlocks(String markdown, Map<String, String> mathBlocks) {
		if (markdown == null || markdown.isEmpty()) {
			return markdown;
		}
		// 先保护块级数学
		Matcher m = MATH_DISPLAY.matcher(markdown);
		StringBuffer sb = new StringBuffer();
		int idx = 0;
		while (m.find()) {
			String ph = PH_DISPLAY + idx + PH_SUFFIX;
			mathBlocks.put(ph, "$$" + m.group(1) + "$$");
			m.appendReplacement(sb, Matcher.quoteReplacement(ph));
			idx++;
		}
		m.appendTail(sb);
		markdown = sb.toString();

		// 再保护行内数学
		m = MATH_INLINE.matcher(markdown);
		sb = new StringBuffer();
		while (m.find()) {
			String ph = PH_INLINE + idx + PH_SUFFIX;
			mathBlocks.put(ph, "$" + m.group(1) + "$");
			m.appendReplacement(sb, Matcher.quoteReplacement(ph));
			idx++;
		}
		m.appendTail(sb);

		return sb.toString();
	}

	// 将占位符还原为原始数学文本，用于 KaTeX 客户端渲染
	private static String restoreMathBlocks(String html, Map<String, String> mathBlocks) {
		if (html == null || mathBlocks.isEmpty()) {
			return html;
		}
		String result = html;
		for (Map.Entry<String, String> entry : mathBlocks.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}

	// 批量将 BlogBriefVO 中的 brief 从 Markdown 片段转为纯文本
	public static void processBriefs(List<BlogBriefVO> list) {
		if (list == null) return;
		for (BlogBriefVO vo : list) {
			if (vo.getBrief() != null) {
				vo.setBrief(extractBrief(vo.getBrief()));
			}
		}
	}
}
