package com.market.util;

import java.util.*;

/** 외부 라이브러리 없이 쓰는 최소 JSON 파서/직렬화기 (Map / List / String / Number / Boolean / null). */
public final class Json {
	private final String s;
	private int i = 0;

	private Json(String s) { this.s = s; }

	public static Object parse(String text) {
		if (text == null || text.trim().isEmpty()) return null;
		Json p = new Json(text);
		p.ws();
		Object v = p.value();
		return v;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseObject(String text) {
		Object o = parse(text);
		return (o instanceof Map) ? (Map<String, Object>) o : new LinkedHashMap<>();
	}

	@SuppressWarnings("unchecked")
	public static List<Object> parseArray(String text) {
		Object o = parse(text);
		return (o instanceof List) ? (List<Object>) o : new ArrayList<>();
	}

	// ---- 편의 접근자 ----
	@SuppressWarnings("unchecked")
	public static Map<String, Object> obj(Object o, String key) {
		if (o instanceof Map) {
			Object v = ((Map<String, Object>) o).get(key);
			if (v instanceof Map) return (Map<String, Object>) v;
		}
		return new LinkedHashMap<>();
	}

	public static String str(Object o, String key) {
		if (o instanceof Map) {
			Object v = ((Map<?, ?>) o).get(key);
			return v == null ? null : String.valueOf(v);
		}
		return null;
	}

	public static int num(Object o, String key, int def) {
		if (o instanceof Map) {
			Object v = ((Map<?, ?>) o).get(key);
			if (v instanceof Number) return ((Number) v).intValue();
			if (v != null) try { return (int) Double.parseDouble(String.valueOf(v)); } catch (Exception ignore) {}
		}
		return def;
	}

	public static boolean bool(Object o, String key) {
		if (o instanceof Map) {
			Object v = ((Map<?, ?>) o).get(key);
			if (v instanceof Boolean) return (Boolean) v;
			return v != null && "true".equalsIgnoreCase(String.valueOf(v));
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> list(Object o, String key) {
		if (o instanceof Map) {
			Object v = ((Map<String, Object>) o).get(key);
			if (v instanceof List) return (List<Object>) v;
		}
		return new ArrayList<>();
	}

	// ---- 파서 ----
	private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

	private Object value() {
		if (i >= s.length()) throw new IllegalArgumentException("JSON 끝");
		char c = s.charAt(i);
		switch (c) {
		case '{': return object();
		case '[': return array();
		case '"': return string();
		case 't': i += 4; return Boolean.TRUE;
		case 'f': i += 5; return Boolean.FALSE;
		case 'n': i += 4; return null;
		default: return number();
		}
	}

	private Map<String, Object> object() {
		Map<String, Object> m = new LinkedHashMap<>();
		i++; ws();
		if (s.charAt(i) == '}') { i++; return m; }
		while (true) {
			ws();
			String k = string();
			ws(); i++; // ':'
			ws();
			m.put(k, value());
			ws();
			char c = s.charAt(i++);
			if (c == '}') break;
		}
		return m;
	}

	private List<Object> array() {
		List<Object> l = new ArrayList<>();
		i++; ws();
		if (s.charAt(i) == ']') { i++; return l; }
		while (true) {
			ws();
			l.add(value());
			ws();
			char c = s.charAt(i++);
			if (c == ']') break;
		}
		return l;
	}

	private String string() {
		StringBuilder sb = new StringBuilder();
		i++; // opening quote
		while (true) {
			char c = s.charAt(i++);
			if (c == '"') break;
			if (c == '\\') {
				char n = s.charAt(i++);
				switch (n) {
				case 'n': sb.append('\n'); break;
				case 't': sb.append('\t'); break;
				case 'r': sb.append('\r'); break;
				case 'b': sb.append('\b'); break;
				case 'f': sb.append('\f'); break;
				case 'u': sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
				default: sb.append(n);
				}
			} else sb.append(c);
		}
		return sb.toString();
	}

	private Object number() {
		int st = i;
		while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
		String n = s.substring(st, i);
		if (n.isEmpty()) throw new IllegalArgumentException("JSON 파싱 오류 위치 " + st);
		if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
		long l = Long.parseLong(n);
		return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (Object) (int) l : (Object) l;
	}

	// ---- 직렬화 ----
	public static String write(Object o) {
		StringBuilder sb = new StringBuilder();
		write(sb, o);
		return sb.toString();
	}

	private static void write(StringBuilder sb, Object o) {
		if (o == null) { sb.append("null"); return; }
		if (o instanceof Number || o instanceof Boolean) { sb.append(o); return; }
		if (o instanceof Map) {
			sb.append('{');
			boolean first = true;
			for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
				if (!first) sb.append(',');
				first = false;
				quote(sb, String.valueOf(e.getKey()));
				sb.append(':');
				write(sb, e.getValue());
			}
			sb.append('}');
			return;
		}
		if (o instanceof Collection) {
			sb.append('[');
			boolean first = true;
			for (Object x : (Collection<?>) o) {
				if (!first) sb.append(',');
				first = false;
				write(sb, x);
			}
			sb.append(']');
			return;
		}
		quote(sb, String.valueOf(o));
	}

	private static void quote(StringBuilder sb, String s) {
		sb.append('"');
		for (int k = 0; k < s.length(); k++) {
			char c = s.charAt(k);
			switch (c) {
			case '"': sb.append("\\\""); break;
			case '\\': sb.append("\\\\"); break;
			case '\n': sb.append("\\n"); break;
			case '\r': sb.append("\\r"); break;
			case '\t': sb.append("\\t"); break;
			default:
				if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
			}
		}
		sb.append('"');
	}
}
