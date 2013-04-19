package net.thucydides.core.reports.html;

import ch.lambdaj.Lambda;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import net.thucydides.core.issues.IssueTracking;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.lambdaj.Lambda.join;

/**
 * Format text for HTML reports.
 * In particular, this integrates JIRA links into the generated reports.
 */
public class Formatter {

    private final static String ISSUE_NUMBER_REGEXP = "#([A-Z][A-Z0-9-_]*)?-?\\d+";
    private final static Pattern shortIssueNumberPattern = Pattern.compile(ISSUE_NUMBER_REGEXP);
    private final static String FULL_ISSUE_NUMBER_REGEXP = "([A-Z][A-Z0-9-_]*)-\\d+";
    private final static Pattern fullIssueNumberPattern = Pattern.compile(FULL_ISSUE_NUMBER_REGEXP);
    private final static String ISSUE_LINK_FORMAT = "<a target=\"_blank\" href=\"{0}\">{1}</a>";
    private static final String ELIPSE = "&hellip;";

    private final IssueTracking issueTracking;

    @Inject
    public Formatter(IssueTracking issueTracking) {
        this.issueTracking = issueTracking;
    }


    static class IssueExtractor {
        private String workingCopy;

        IssueExtractor(String initialValue) {
            this.workingCopy = initialValue;
        }


        public List<String> getShortenedIssues() {
            Matcher matcher = shortIssueNumberPattern.matcher(workingCopy);

            ArrayList<String> issues = Lists.newArrayList();
            while (matcher.find()) {
                String issue = matcher.group();
                issues.add(issue);
                workingCopy = workingCopy.replaceFirst(issue,"");
            }

            return issues;
        }

        public List<String> getFullIssues() {
            Matcher unhashedMatcher = fullIssueNumberPattern.matcher(workingCopy);

            ArrayList<String> issues = Lists.newArrayList();
            while (unhashedMatcher.find()) {
                String issue = unhashedMatcher.group();
                issues.add(issue);
                workingCopy = workingCopy.replaceFirst(issue,"");
            }

            return issues;
        }

    }

    public static List<String> issuesIn(final String value) {

        IssueExtractor extractor = new IssueExtractor(value);

        List<String> issuesWithHash = extractor.getShortenedIssues();
        List<String> allIssues = extractor.getFullIssues();
        allIssues.addAll(issuesWithHash);

        return allIssues;
    }

    public String addLinks(final String value) {
        if (issueTracking == null) {
            return value;
        }
        String formattedValue = value;
        if (issueTracking.getIssueTrackerUrl() != null) {
            formattedValue = insertFullIssueTrackingUrls(value);
        }
        if (issueTracking.getShortenedIssueTrackerUrl() != null) {
            formattedValue = insertShortenedIssueTrackingUrls(formattedValue);
        }
        return formattedValue;
    }

    public String addLineBreaks(final String text) {
        return (text != null) ?
                text.replaceAll(IOUtils.LINE_SEPARATOR_WINDOWS,"<br>").replaceAll(IOUtils.LINE_SEPARATOR_UNIX, "<br>") : "";
    }

    private final CharSequenceTranslator ESCAPE_SPECIAL_CHARS = new AggregateTranslator(
            new LookupTranslator(EntityArrays.ISO8859_1_ESCAPE()),
            new LookupTranslator(EntityArrays.HTML40_EXTENDED_ESCAPE())
    );

    public String htmlCompatible(Object fieldValue) {
        return addLineBreaks(ESCAPE_SPECIAL_CHARS.translate(fieldValue != null? stringFormOf(fieldValue) : ""));
    }

    private String stringFormOf(Object fieldValue) {
        if (Iterable.class.isAssignableFrom(fieldValue.getClass())) {
            return "[" + join(fieldValue) + "]";
        } else {
            return fieldValue.toString();
        }
    }

    public String truncatedHtmlCompatible(String text, int length) {
        return addLineBreaks(ESCAPE_SPECIAL_CHARS.translate(truncate(text,length)));
    }

    private String truncate(String text, int length) {
        if (text.length() > length) {
            return text.substring(0,length).trim() + ELIPSE;
        } else {
            return text;
        }
    }

    private String insertShortenedIssueTrackingUrls(String value) {
        String formattedValue = value;
        String issueUrlFormat = issueTracking.getShortenedIssueTrackerUrl();
        List<String> issues = shortenedIssuesIn(value);
        for(String issue : issues) {
            String issueUrl = MessageFormat.format(issueUrlFormat, stripLeadingHashFrom(issue));
            String issueLink = MessageFormat.format(ISSUE_LINK_FORMAT, issueUrl, issue);
            formattedValue = formattedValue.replaceAll(issue, issueLink);
        }
        return formattedValue;
    }

    public static List<String> shortenedIssuesIn(String value) {
        IssueExtractor extractor = new IssueExtractor(value);
        return extractor.getShortenedIssues();
    }

    public static List<String> fullIssuesIn(String value) {
        IssueExtractor extractor = new IssueExtractor(value);
        return extractor.getFullIssues();
    }

    private String insertFullIssueTrackingUrls(String value) {
        String formattedValue = value;
        String issueUrlFormat = issueTracking.getIssueTrackerUrl();
        List<String> issues = fullIssuesIn(value);
        for(String issue : issues) {
            String issueUrl = MessageFormat.format(issueUrlFormat, issue);
            String issueLink = MessageFormat.format(ISSUE_LINK_FORMAT, issueUrl, issue);
            formattedValue = formattedValue.replaceAll(issue, issueLink);
        }
        return formattedValue;
    }

    public String formatWithFields(String textToFormat, List<String> fields) {
        String textWithEscapedFields = textToFormat;
        for(String field : fields) {
            textWithEscapedFields = textWithEscapedFields.replaceAll("<" + field + ">", "&lt;" + field + "&gt;");
        }
        return addLineBreaks(textWithEscapedFields);
    }

    private String stripLeadingHashFrom(final String issue) {
        return issue.substring(1);
    }
}
