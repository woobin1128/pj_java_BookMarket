package com.market.config;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Properties;

/**
 * 설정 로더. 우선순위: 환경변수 > JVM 시스템 프로퍼티 > WEB-INF/config.properties > ~/.bookmarket_web_data/config.properties
 * 키 예) supabase.url / supabase.key / kakao.rest.key / kakao.client.secret / kakao.redirect.uri
 * 환경변수는 점을 밑줄로, 대문자로 바꾼 이름(SUPABASE_URL ...)을 사용합니다.
 */
public final class AppConfig {
	private static final Properties props = new Properties();
	private static boolean loaded = false;

	private AppConfig() {}

	public static synchronized void load(String webappRealPath) {
		if (loaded) return;
		loaded = true;
		Path home = Paths.get(System.getProperty("user.home"), ".bookmarket_web_data", "config.properties");
		readInto(home);
		if (webappRealPath != null && !webappRealPath.isEmpty()) {
			readInto(Paths.get(webappRealPath, "WEB-INF", "config.properties"));
		}
	}

	private static void readInto(Path p) {
		if (!Files.isRegularFile(p)) return;
		try (InputStream in = Files.newInputStream(p)) {
			Properties tmp = new Properties();
			tmp.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
			for (String k : tmp.stringPropertyNames()) {
				String v = tmp.getProperty(k).trim();
				if (!v.isEmpty()) props.setProperty(k, v);   // 빈 값은 덮어쓰지 않음
			}
		} catch (Exception e) {
			System.err.println("[config] 설정 파일을 읽지 못했습니다: " + p + " (" + e.getMessage() + ")");
		}
	}

	public static String get(String key) {
		return get(key, "");
	}

	public static String get(String key, String def) {
		String env = System.getenv(key.replace('.', '_').toUpperCase());
		if (env != null && !env.trim().isEmpty()) return env.trim();
		String sys = System.getProperty(key);
		if (sys != null && !sys.trim().isEmpty()) return sys.trim();
		String v = props.getProperty(key);
		return (v != null && !v.isEmpty()) ? v : def;
	}

	public static boolean has(String key) {
		return !get(key).isEmpty();
	}
}
