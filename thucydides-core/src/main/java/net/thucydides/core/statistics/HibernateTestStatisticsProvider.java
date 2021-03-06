package net.thucydides.core.statistics;

import com.google.inject.Inject;
import net.thucydides.core.Thucydides;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.statistics.dao.TestOutcomeHistoryDAO;
import net.thucydides.core.statistics.model.TestRun;
import net.thucydides.core.statistics.model.TestRunTag;
import net.thucydides.core.statistics.model.TestStatistics;

import java.util.List;

public class HibernateTestStatisticsProvider implements TestStatisticsProvider {

    private final TestOutcomeHistoryDAO testOutcomeHistoryDAO;

    @Inject
    public HibernateTestStatisticsProvider(TestOutcomeHistoryDAO testOutcomeHistoryDAO) {
        this.testOutcomeHistoryDAO = testOutcomeHistoryDAO;
    }

    @Override
    public List<TestRun> testRunsForTest(With withCondition) {
        if (withCondition instanceof WithTitle) {
            return testOutcomeHistoryDAO.findTestRunsByTitle(((WithTitle) withCondition).getTitle());
        }
        return null;
    }

    @Override
    public List<TestRun> getAllTestHistories() {
        return testOutcomeHistoryDAO.findAll();
    }


    @Override
    public TestStatistics statisticsForTests(With withCondition) {
        if (withCondition instanceof WithTitle) {
            return testStatisticsForTitle((WithTitle) withCondition);
        } else if (withCondition instanceof WithTagNamed) {
            return testStatisticsForTestsWithTag((WithTagNamed) withCondition);
        } else if (withCondition instanceof WithTagTypeNamed) {
            return testStatisticsForTestsWithTagType((WithTagTypeNamed) withCondition);
        }
        return null;
    }

    private TestStatistics testStatisticsForTestsWithTag(WithTagNamed withCondition) {
        String tag = withCondition.getTag();
        Long totalTests = testOutcomeHistoryDAO.countTestRunsByTag(tag);
        Long passingTests = testOutcomeHistoryDAO.countTestRunsByTagAndResult(tag, TestResult.SUCCESS);
        Long failingTests = testOutcomeHistoryDAO.countTestRunsByTagAndResult(tag, TestResult.FAILURE);
        List<TestResult> results = testOutcomeHistoryDAO.getResultsForTestsWithTag(tag);
        List<TestRunTag> latestTags = testOutcomeHistoryDAO.getLatestTagsForTestsWithTag(tag);
        return new TestStatistics(totalTests, passingTests, failingTests, results, latestTags);
    }

    private TestStatistics testStatisticsForTestsWithTagType(WithTagTypeNamed withCondition) {
        String tagType = withCondition.getTagType();
        Long totalTests = testOutcomeHistoryDAO.countTestRunsByTagType(tagType);
        Long passingTests = testOutcomeHistoryDAO.countTestRunsByTagTypeAndResult(tagType, TestResult.SUCCESS);
        Long failingTests = testOutcomeHistoryDAO.countTestRunsByTagTypeAndResult(tagType, TestResult.FAILURE);
        List<TestResult> results = testOutcomeHistoryDAO.getResultsForTestsWithTagType(tagType);
        List<TestRunTag> latestTags = testOutcomeHistoryDAO.getLatestTagsForTestsWithTagType(tagType);
        return new TestStatistics(totalTests, passingTests, failingTests, results, latestTags);
    }

    private TestStatistics testStatisticsForTitle(WithTitle withCondition) {
        Long totalTests = testOutcomeHistoryDAO.countTestRunsByTitle(withCondition.getTitle());
        Long passingTests = testOutcomeHistoryDAO.countTestRunsByTitleAndResult(withCondition.getTitle(), TestResult.SUCCESS);
        Long failingTests = testOutcomeHistoryDAO.countTestRunsByTitleAndResult(withCondition.getTitle(), TestResult.FAILURE);
        List<TestResult> results = testOutcomeHistoryDAO.getResultsTestWithTitle(withCondition.getTitle());
        List<TestRunTag> latestTags = testOutcomeHistoryDAO.getLatestTagsForTestWithTitleByTitle(withCondition.getTitle());
        return new TestStatistics(totalTests, passingTests, failingTests, results, latestTags);
    }

    @Override
    public List<TestRunTag> findAllTags() {
        return testOutcomeHistoryDAO.findAllTags();
    }

    @Override
    public List<String> findAllTagTypes() {
        return testOutcomeHistoryDAO.findAllTagTypes();
    }
}
