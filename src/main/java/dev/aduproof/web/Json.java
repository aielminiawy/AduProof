package dev.aduproof.web;

/** Small response-only JSON writer. Never used to parse untrusted JSON input. */
public final class Json {
    private Json() {}

    public static String quote(String s) {
        if (s == null) return "null";
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c == '\u2028' || c == '\u2029') {
                        out.append(String.format("\\u%04x", (int)c));
                    } else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    public static String field(String key, String value) { return quote(key) + ":" + quote(value); }
    public static String flag(String key, boolean value) { return quote(key) + ":" + value; }
}
