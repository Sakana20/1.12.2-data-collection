package com.sakana.nebulaestats.exit;

public final class ExitRegionIds {
    public static final String ID_PATTERN = "[\\p{L}\\p{N}_\\-:.]+";
    public static final String ID_RULE_MESSAGE =
            "\u51fa\u53e3 ID \u53ea\u80fd\u5305\u542b Unicode \u5b57\u6bcd\u3001\u6570\u5b57\u4ee5\u53ca _\u3001-\u3001:\u3001.";

    private ExitRegionIds() {
    }

    public static boolean isValid(String id) {
        return id != null && id.matches(ID_PATTERN);
    }
}

