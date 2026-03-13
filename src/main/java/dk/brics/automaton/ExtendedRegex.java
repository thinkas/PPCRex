package dk.brics.automaton;

// extend regex with some extra metachar
//\w 	Find a word character => ADDED
//\d 	Find a digit  => ADDED
//\s 	Find a whitespace character

/**
 * This class allows to build a regex from a string with more metachar than
 * those supported by RegExp dk.brics.automaton library
 *
 */
public class ExtendedRegex {

	public static RegExp getSimplifiedRegexp(String s) {
		String eString = extendRegex(s);
		return new RegExp(eString);
	}

	public static String extendRegex(String s) {
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < s.length()) {
			char c = s.charAt(i);

			if (c == '[') {
				// 处理字符类
				int j = i + 1;
				boolean neg = false;
				if (j < s.length() && s.charAt(j) == '^') {
					neg = true;
					j++;
				}

				StringBuilder cls = new StringBuilder();
				while (j < s.length() && s.charAt(j) != ']') {
					cls.append(s.charAt(j));
					j++;
				}

				// 如果没有找到关闭 ], 原样输出
				if (j >= s.length()) {
					out.append(c);
					i++;
					continue;
				}

				// 处理字符类内容
				String expanded = expandCharClass(cls.toString());

				// 重建字符类
				// 如果 expandCharClass 返回以 '(' 开头，就直接追加，不加 []
				if (expanded.startsWith("(")) {
					out.append(expanded);
				} else {
					out.append("[");
					if (neg) out.append("^");
					out.append(expanded);
					out.append("]");
				}


				i = j + 1;
			} else {
				// 字符类外部，继续处理单独的 \d, \w, \s
				if (c == '\\' && i + 1 < s.length()) {
					char n = s.charAt(i + 1);
					switch (n) {
						case 'd':
							out.append("[0-9]");
							break;
						case 'D':
							out.append("[^0-9]");  // 添加这一行
							break;
						case 'w':
							out.append("[a-zA-Z0-9_]");
							break;
						case 's':
							out.append("[ ]");
							break;
						case 'S':
							out.append("[^ ]");
							break;
						default:
							out.append(c).append(n);
					}
					i += 2;
				} else {
					out.append(c);
					i++;
				}
			}
		}
		return out.toString();
	}

	private static String expandCharClass(String cls) {
		// 统计字符类中可扩展转义字符数量
		int specialCount = 0;
		for (int i = 0; i < cls.length(); i++) {
			char c = cls.charAt(i);
			if (c == '\\' && i + 1 < cls.length()) {
				char n = cls.charAt(i + 1);
				if ("dDwWsS".indexOf(n) >= 0) {
					specialCount++;
				}
				i++; // 跳过下一个字符
			}
		}

		// 如果出现多个特殊转义字符，拆成 ()
		if (specialCount > 1) {
			StringBuilder out = new StringBuilder();
			out.append("(");
			boolean first = true;
			for (int i = 0; i < cls.length(); i++) {
				char c = cls.charAt(i);
				String piece = null;
				if (c == '\\' && i + 1 < cls.length()) {
					char n = cls.charAt(i + 1);
					switch (n) {
						case 'd':
							piece = "[0-9]";
							break;
						case 'D':
							piece = "[^0-9]";
							break;
						case 'w':
							piece = "[a-zA-Z0-9_]";
							break;
						case 'W':
							piece = "[^a-zA-Z0-9_]";
							break;
						case 's':
							piece = "[ ]";
							break;
						case 'S':
							piece = "[^ ]";
							break;
						default:
							piece = "\\" + n;
							break;
					}
					i++;
				} else {
					piece = String.valueOf(c);
				}
				if (!first) {
					out.append("|");
				}
				out.append(piece);
				first = false;
			}
			out.append(")");
			return out.toString();
		}

			// 否则，按原逻辑展开
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cls.length(); i++) {
			char c = cls.charAt(i);
			if (c == '\\' && i + 1 < cls.length()) {
				char n = cls.charAt(i + 1);
				switch (n) {
					case 'd':
						sb.append("0-9");
						break;
					case 'D':
						sb.append("^0-9");
						break;
					case 'w':
						sb.append("a-zA-Z0-9_");
						break;
					case 's':
						sb.append(" ");
						break;
					case 'S':
						sb.append("^ ");
						break;
					default:
						sb.append("\\").append(n);
				}
				i++;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}