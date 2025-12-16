package com.murasame.util;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

// 后端传值将html博客文本与markdown之间互转保存或取值（Jsoup + commonmark）
public class BlogHtmlUtil {
	private static final Parser PARSER = Parser.builder().build();
	private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();
	private static final Safelist SAFE = Safelist.basic()
			.addTags("br","p","strong","em","h1","h2","h3","h4","h5","h6","a")
			.addAttributes("a","href","title");

	// markdown to clean html
	public static String toHtml(String markdown){
		if (markdown == null) return "";
		markdown = markdown.replace("\n","\n\n");
		String html = RENDERER.render(PARSER.parse(markdown));
		return Jsoup.clean(html, SAFE);
	}
}
