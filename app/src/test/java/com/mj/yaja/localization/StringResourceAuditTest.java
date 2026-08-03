package com.mj.yaja.localization;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StringResourceAuditTest {
    private static final String BASELINE_RELATIVE_PATH =
            "src/test/resources/localization/string-audit-baseline.properties";
    private static final Pattern FORMAT_PATTERN =
            Pattern.compile("%(?:(\\d+)\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?([a-zA-Z%])");
    private static final Pattern TEXT_LITERAL_PATTERN =
            Pattern.compile("\\bText\\s*\\(\\s*\"([^\"]{2,})\"");
    private static final Pattern TOAST_LITERAL_PATTERN =
            Pattern.compile("\\bToast\\.makeText\\s*\\([^,]+,\\s*\"([^\"]{2,})\"");
    private static final Pattern CONTENT_DESCRIPTION_LITERAL_PATTERN =
            Pattern.compile("\\bcontentDescription\\s*=\\s*\"([^\"]{2,})\"");

    @Test
    public void stringResourcesDoNotRegressAgainstBaseline() throws Exception {
        Path appDir = findAppDir();
        AuditResult result = audit(appDir);
        writeMarkdownReport(appDir, result);

        Path baselinePath = appDir.resolve(BASELINE_RELATIVE_PATH);
        if (!Files.exists(baselinePath)) {
            Path proposedBaseline = appDir.resolve("build/reports/localization/string-audit-baseline.properties");
            writeBaselineProperties(proposedBaseline, result);
            throw new AssertionError(
                    "Missing localization baseline. Proposed baseline written to " + proposedBaseline);
        }

        Properties baseline = new Properties();
        try (InputStream inputStream = Files.newInputStream(baselinePath)) {
            baseline.load(inputStream);
        }

        List<String> failures = compareAgainstBaseline(result, baseline);
        assertTrue(String.join(System.lineSeparator(), failures), failures.isEmpty());
    }

    private static AuditResult audit(Path appDir) throws Exception {
        Path resDir = appDir.resolve("src/main/res");
        ResourceFile base = parseResourceFile(resDir.resolve("values/strings.xml"));

        AuditResult result = new AuditResult();
        result.baseStringCount = base.strings.size();
        result.basePluralCount = base.plurals.size();
        result.hardcodedUiStringFindings = scanHardcodedUiStrings(appDir);

        try (Stream<Path> paths = Files.list(resDir)) {
            List<Path> localeStringFiles = paths
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("values-"))
                    .map(path -> path.resolve("strings.xml"))
                    .filter(Files::exists)
                    .sorted(Comparator.comparing(path -> path.getParent().getFileName().toString()))
                    .toList();

            for (Path localePath : localeStringFiles) {
                String locale = localePath.getParent().getFileName().toString();
                ResourceFile translated = parseResourceFile(localePath);
                result.locales.put(locale, compareLocale(locale, base, translated));
            }
        }

        return result;
    }

    private static LocaleAudit compareLocale(
            String locale,
            ResourceFile base,
            ResourceFile translated
    ) {
        LocaleAudit audit = new LocaleAudit(locale);
        audit.duplicateResources.addAll(translated.duplicateResources);

        audit.missingStrings.addAll(missingKeys(base.strings.keySet(), translated.strings.keySet()));
        audit.extraStrings.addAll(missingKeys(translated.strings.keySet(), base.strings.keySet()));
        audit.missingPlurals.addAll(missingKeys(base.plurals.keySet(), translated.plurals.keySet()));
        audit.extraPlurals.addAll(missingKeys(translated.plurals.keySet(), base.plurals.keySet()));

        for (String name : base.strings.keySet()) {
            StringValue baseValue = base.strings.get(name);
            StringValue translatedValue = translated.strings.get(name);
            if (translatedValue == null) {
                continue;
            }
            if (baseValue.formatted && translatedValue.formatted) {
                List<String> basePlaceholders = placeholders(baseValue.text);
                List<String> translatedPlaceholders = placeholders(translatedValue.text);
                if (!basePlaceholders.equals(translatedPlaceholders)) {
                    audit.placeholderMismatches.add(name);
                }
            }
            if (hasLetters(baseValue.text)
                    && normalizeText(baseValue.text).equals(normalizeText(translatedValue.text))) {
                audit.identicalStrings.add(name);
            }
        }

        for (String name : base.plurals.keySet()) {
            Map<String, StringValue> baseItems = base.plurals.get(name);
            Map<String, StringValue> translatedItems = translated.plurals.get(name);
            if (translatedItems == null) {
                continue;
            }
            Set<String> missingQuantities = missingKeys(baseItems.keySet(), translatedItems.keySet());
            if (!missingQuantities.isEmpty()) {
                audit.missingPluralQuantities.put(name, missingQuantities);
            }
            for (String quantity : baseItems.keySet()) {
                StringValue baseValue = baseItems.get(quantity);
                StringValue translatedValue = translatedItems.get(quantity);
                if (translatedValue == null || !baseValue.formatted || !translatedValue.formatted) {
                    continue;
                }
                if (!placeholders(baseValue.text).equals(placeholders(translatedValue.text))) {
                    audit.placeholderMismatches.add(name + "[" + quantity + "]");
                }
            }
        }

        return audit;
    }

    private static ResourceFile parseResourceFile(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(path.toFile());
        ResourceFile resourceFile = new ResourceFile();
        NodeList children = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            String tagName = element.getTagName();
            String name = element.getAttribute("name");
            if (name.isBlank() || "false".equals(element.getAttribute("translatable"))) {
                continue;
            }

            if ("string".equals(tagName)) {
                addResource(resourceFile.strings, resourceFile.duplicateResources, name,
                        new StringValue(element.getTextContent(), isFormatted(element)));
            } else if ("plurals".equals(tagName)) {
                Map<String, StringValue> items = new TreeMap<>();
                NodeList pluralItems = element.getChildNodes();
                for (int itemIndex = 0; itemIndex < pluralItems.getLength(); itemIndex++) {
                    Node itemNode = pluralItems.item(itemIndex);
                    if (itemNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element item = (Element) itemNode;
                    if (!"item".equals(item.getTagName())) {
                        continue;
                    }
                    items.put(item.getAttribute("quantity"),
                            new StringValue(item.getTextContent(), isFormatted(element)));
                }
                addResource(resourceFile.plurals, resourceFile.duplicateResources, name, items);
            }
        }
        return resourceFile;
    }

    private static <T> void addResource(
            Map<String, T> resources,
            List<String> duplicateResources,
            String name,
            T value
    ) {
        if (resources.containsKey(name)) {
            duplicateResources.add(name);
        }
        resources.put(name, value);
    }

    private static boolean isFormatted(Element element) {
        return !"false".equals(element.getAttribute("formatted"));
    }

    private static List<String> placeholders(String value) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = FORMAT_PATTERN.matcher(value);
        int fallbackIndex = 1;
        while (matcher.find()) {
            String type = matcher.group(2);
            if ("%".equals(type) || "n".equals(type)) {
                continue;
            }
            String position = matcher.group(1);
            String normalizedPosition = position == null ? String.valueOf(fallbackIndex++) : position;
            placeholders.add(normalizedPosition + ":" + normalizePlaceholderType(type));
        }
        return placeholders;
    }

    private static String normalizePlaceholderType(String type) {
        String lowerType = type.toLowerCase(Locale.US);
        if ("dox".contains(lowerType)) {
            return "integer";
        }
        if ("aefg".contains(lowerType)) {
            return "float";
        }
        if ("s".equals(lowerType)) {
            return "string";
        }
        if ("c".equals(lowerType)) {
            return "char";
        }
        return lowerType;
    }

    private static List<HardcodedUiString> scanHardcodedUiStrings(Path appDir) throws IOException {
        Path sourceDir = appDir.resolve("src/main/java");
        List<HardcodedUiString> findings = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceDir)) {
            for (Path path : files.filter(path -> path.toString().endsWith(".kt")).toList()) {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    addMatches(findings, appDir, path, i + 1, line, TEXT_LITERAL_PATTERN, "Text");
                    addMatches(findings, appDir, path, i + 1, line, TOAST_LITERAL_PATTERN, "Toast");
                    addMatches(findings, appDir, path, i + 1, line,
                            CONTENT_DESCRIPTION_LITERAL_PATTERN, "contentDescription");
                }
            }
        }
        findings.sort(Comparator.comparing((HardcodedUiString finding) -> finding.path)
                .thenComparingInt(finding -> finding.line));
        return findings;
    }

    private static void addMatches(
            List<HardcodedUiString> findings,
            Path appDir,
            Path path,
            int line,
            String source,
            Pattern pattern,
            String type
    ) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            String text = matcher.group(1);
            if (shouldIgnoreLiteral(text)) {
                continue;
            }
            findings.add(new HardcodedUiString(appDir.relativize(path).toString(), line, type, text));
        }
    }

    private static boolean shouldIgnoreLiteral(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty()
                || trimmed.startsWith("#")
                || trimmed.startsWith("%")
                || trimmed.startsWith("http")
                || trimmed.matches("[A-Z0-9_./:-]+")
                || !hasLetters(trimmed);
    }

    private static void writeMarkdownReport(Path appDir, AuditResult result) throws IOException {
        Path reportPath = appDir.resolve("build/reports/localization/string-audit.md");
        Files.createDirectories(reportPath.getParent());
        Files.write(reportPath, markdownReport(result).getBytes(StandardCharsets.UTF_8));
    }

    private static String markdownReport(AuditResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# String Resource Audit\n\n");
        builder.append("- Base strings: ").append(result.baseStringCount).append('\n');
        builder.append("- Base plurals: ").append(result.basePluralCount).append('\n');
        builder.append("- Translated locales: ").append(result.locales.size()).append('\n');
        builder.append("- Hardcoded UI string candidates: ")
                .append(result.hardcodedUiStringFindings.size()).append("\n\n");

        builder.append("## Locale Summary\n\n");
        builder.append("| Locale | Missing strings | Extra strings | Missing plurals | Extra plurals | Placeholder mismatches | Duplicate keys | Identical to English |\n");
        builder.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (LocaleAudit audit : result.locales.values()) {
            builder.append("| ").append(audit.locale)
                    .append(" | ").append(audit.missingStrings.size())
                    .append(" | ").append(audit.extraStrings.size())
                    .append(" | ").append(audit.missingPlurals.size())
                    .append(" | ").append(audit.extraPlurals.size())
                    .append(" | ").append(audit.placeholderMismatches.size())
                    .append(" | ").append(audit.duplicateResources.size())
                    .append(" | ").append(audit.identicalStrings.size())
                    .append(" |\n");
        }

        builder.append("\n## Missing Keys By Locale\n\n");
        for (LocaleAudit audit : result.locales.values()) {
            if (audit.missingStrings.isEmpty() && audit.missingPlurals.isEmpty()
                    && audit.missingPluralQuantities.isEmpty()) {
                continue;
            }
            builder.append("### ").append(audit.locale).append("\n\n");
            appendList(builder, "Missing strings", audit.missingStrings);
            appendList(builder, "Missing plurals", audit.missingPlurals);
            if (!audit.missingPluralQuantities.isEmpty()) {
                builder.append("- Missing plural quantities:\n");
                for (Map.Entry<String, Set<String>> entry : audit.missingPluralQuantities.entrySet()) {
                    builder.append("  - ").append(entry.getKey()).append(": ")
                            .append(String.join(", ", entry.getValue())).append('\n');
                }
            }
            builder.append('\n');
        }

        builder.append("## Structural Issues\n\n");
        for (LocaleAudit audit : result.locales.values()) {
            if (audit.extraStrings.isEmpty() && audit.extraPlurals.isEmpty()
                    && audit.placeholderMismatches.isEmpty() && audit.duplicateResources.isEmpty()) {
                continue;
            }
            builder.append("### ").append(audit.locale).append("\n\n");
            appendList(builder, "Extra strings", audit.extraStrings);
            appendList(builder, "Extra plurals", audit.extraPlurals);
            appendList(builder, "Placeholder mismatches", audit.placeholderMismatches);
            appendList(builder, "Duplicate keys", audit.duplicateResources);
            builder.append('\n');
        }

        builder.append("## Hardcoded UI String Candidates\n\n");
        for (HardcodedUiString finding : result.hardcodedUiStringFindings) {
            builder.append("- `").append(finding.path).append(':').append(finding.line).append("` ")
                    .append(finding.type).append(": ")
                    .append(finding.text.replace("\n", "\\n")).append('\n');
        }

        return builder.toString();
    }

    private static void appendList(StringBuilder builder, String label, Collection<String> values) {
        if (values.isEmpty()) {
            return;
        }
        builder.append("- ").append(label).append(": ").append(String.join(", ", values)).append('\n');
    }

    private static void writeBaselineProperties(Path path, AuditResult result) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, baselineProperties(result).getBytes(StandardCharsets.UTF_8));
    }

    private static String baselineProperties(AuditResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Generated by StringResourceAuditTest. Update only after reviewing localization changes.\n");
        builder.append("base.strings=").append(result.baseStringCount).append('\n');
        builder.append("base.plurals=").append(result.basePluralCount).append('\n');
        builder.append("locale.count=").append(result.locales.size()).append('\n');
        builder.append("hardcodedUiStrings=").append(result.hardcodedUiStringFindings.size()).append('\n');
        for (LocaleAudit audit : result.locales.values()) {
            String prefix = "locale." + audit.locale + ".";
            builder.append(prefix).append("missingStrings=").append(audit.missingStrings.size()).append('\n');
            builder.append(prefix).append("extraStrings=").append(audit.extraStrings.size()).append('\n');
            builder.append(prefix).append("missingPlurals=").append(audit.missingPlurals.size()).append('\n');
            builder.append(prefix).append("extraPlurals=").append(audit.extraPlurals.size()).append('\n');
            builder.append(prefix).append("missingPluralQuantities=")
                    .append(audit.missingPluralQuantities.size()).append('\n');
            builder.append(prefix).append("placeholderMismatches=")
                    .append(audit.placeholderMismatches.size()).append('\n');
            builder.append(prefix).append("duplicateResources=")
                    .append(audit.duplicateResources.size()).append('\n');
        }
        return builder.toString();
    }

    private static List<String> compareAgainstBaseline(AuditResult result, Properties baseline) {
        List<String> failures = new ArrayList<>();
        int baselineLocaleCount = intProperty(baseline, "locale.count");
        if (result.locales.size() < baselineLocaleCount) {
            failures.add("Translated locale count dropped from " + baselineLocaleCount
                    + " to " + result.locales.size());
        }

        compareCount(failures, "hardcodedUiStrings",
                result.hardcodedUiStringFindings.size(), intProperty(baseline, "hardcodedUiStrings"));

        for (LocaleAudit audit : result.locales.values()) {
            String prefix = "locale." + audit.locale + ".";
            compareCount(failures, prefix + "missingStrings",
                    audit.missingStrings.size(), intProperty(baseline, prefix + "missingStrings"));
            compareCount(failures, prefix + "extraStrings",
                    audit.extraStrings.size(), intProperty(baseline, prefix + "extraStrings"));
            compareCount(failures, prefix + "missingPlurals",
                    audit.missingPlurals.size(), intProperty(baseline, prefix + "missingPlurals"));
            compareCount(failures, prefix + "extraPlurals",
                    audit.extraPlurals.size(), intProperty(baseline, prefix + "extraPlurals"));
            compareCount(failures, prefix + "missingPluralQuantities",
                    audit.missingPluralQuantities.size(),
                    intProperty(baseline, prefix + "missingPluralQuantities"));
            compareCount(failures, prefix + "placeholderMismatches",
                    audit.placeholderMismatches.size(),
                    intProperty(baseline, prefix + "placeholderMismatches"));
            compareCount(failures, prefix + "duplicateResources",
                    audit.duplicateResources.size(), intProperty(baseline, prefix + "duplicateResources"));
        }

        return failures;
    }

    private static void compareCount(List<String> failures, String label, int actual, int baseline) {
        if (actual > baseline) {
            failures.add(label + " increased from " + baseline + " to " + actual);
        }
    }

    private static int intProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing localization baseline key: " + key);
        }
        return Integer.parseInt(value);
    }

    private static Set<String> missingKeys(Set<String> expected, Set<String> actual) {
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private static String normalizeText(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static boolean hasLetters(String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }

    private static Path findAppDir() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path appCandidate = current.resolve("app/src/main/res/values/strings.xml");
            if (Files.exists(appCandidate)) {
                return current.resolve("app");
            }
            Path moduleCandidate = current.resolve("src/main/res/values/strings.xml");
            if (Files.exists(moduleCandidate)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate app module from working directory");
    }

    private record StringValue(String text, boolean formatted) {
    }

    private record HardcodedUiString(String path, int line, String type, String text) {
    }

    private static final class ResourceFile {
        private final Map<String, StringValue> strings = new TreeMap<>();
        private final Map<String, Map<String, StringValue>> plurals = new TreeMap<>();
        private final List<String> duplicateResources = new ArrayList<>();
    }

    private static final class LocaleAudit {
        private final String locale;
        private final Set<String> missingStrings = new TreeSet<>();
        private final Set<String> extraStrings = new TreeSet<>();
        private final Set<String> missingPlurals = new TreeSet<>();
        private final Set<String> extraPlurals = new TreeSet<>();
        private final Map<String, Set<String>> missingPluralQuantities = new LinkedHashMap<>();
        private final Set<String> placeholderMismatches = new TreeSet<>();
        private final List<String> duplicateResources = new ArrayList<>();
        private final Set<String> identicalStrings = new TreeSet<>();

        private LocaleAudit(String locale) {
            this.locale = locale;
        }
    }

    private static final class AuditResult {
        private int baseStringCount;
        private int basePluralCount;
        private final Map<String, LocaleAudit> locales = new LinkedHashMap<>();
        private List<HardcodedUiString> hardcodedUiStringFindings = List.of();
    }
}
