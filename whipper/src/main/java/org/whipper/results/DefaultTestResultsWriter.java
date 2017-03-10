package org.whipper.results;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whipper.Query;
import org.whipper.Query.QueryResult;
import org.whipper.Scenario;
import org.whipper.Suite;
import org.whipper.WhipperProperties;

/**
 * Default test results writer.
 *
 * @author Juraj Duráni
 */
public class DefaultTestResultsWriter implements TestResultsWriter{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTestResultsWriter.class);
    private static final String LS = System.lineSeparator();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private static final DateFormat TS_FORMAT = new SimpleDateFormat("_yyyyMMdd_HHmmss");
    private static final int NAME_PAD = 50;
    private static final int RESULTS_PAD = 6;

    private File outputDir;
    private File errors;
    private File totals;

    @Override
    public boolean init(WhipperProperties props){
        outputDir = props.getOutputDir();
        if(outputDir == null){
            LOG.error("Cannot write results. Output directory is not set.");
            return false;
        }
        if(outputDir.exists() && !outputDir.isDirectory()){
            LOG.error("Cannot write results. Output directory is not a directory [{}].", outputDir.getAbsolutePath());
            return false;
        }
        if(!outputDir.exists() && !outputDir.mkdirs()){
            LOG.error("Cannot write results. Output directory cannot be created [{}].", outputDir.getAbsolutePath());
            return false;
        }
        errors = summaryErrors(outputDir);
        totals = summaryTotals(outputDir);
        return true;
    }

    @Override
    public void destroy(){
        outputDir = null;
        errors = null;
        totals = null;
    }

    @Override
    public void writeResultOfScenario(Scenario scen){
        writeSummary(scen);
        File outDir = scenarioDir(outputDir, scen.getId());
        if(!outDir.exists() && !outDir.mkdirs()){
            LOG.error("Cannot create output directory of the scenario {}", outDir.getAbsolutePath());
        } else if(outDir.exists() && !outDir.isDirectory()){
            LOG.error("Output directory of the scenario is not a directory {}", outDir.getAbsolutePath());
        } else {
            for(Suite s : scen.getSuites()){
                writeResultOfSuite(outDir, s);
            }
        }
    }

    /**
     * Writes result of the suite in files {@code <outDir>/<suite-name>.txt}
     * and {@code <outDir>/<suite-name>_<timestamp>.txt}
     *
     * @param outDir output directory
     * @param suite suite
     */
    private void writeResultOfSuite(File outDir, Suite suite){
        File out1 = suiteSummary(outDir, suite.getId(), false);
        File out2 = suiteSummary(outDir, suite.getId(), true);
        try{
            if(!out1.exists()){
                out1.createNewFile();
            }
            out2.createNewFile();
        } catch (IOException ex){
            LOG.error("Cannot create summary files for suite.", ex);
            return;
        }
        try(FileWriter fwAll = new FileWriter(out1, true);
                FileWriter fwThis = new FileWriter(out2, false)){
            writeSuiteSummaryHeader(fwAll, suite);
            writeSuiteSummaryHeader(fwThis, suite);
            writeSuiteResults(fwAll, suite);
            writeSuiteResults(fwThis, suite);
            appendLine(fwAll, LS); // two lines
        } catch (IOException ex) {
            LOG.error("Unable to write result of suite " + suite.getId(), ex);
        }
    }

    /**
     * Writes results of the suite to provided file writer.
     *
     * @param fw file writer
     * @param suite suite
     * @throws IOException if some error occurs
     */
    private void writeSuiteResults(FileWriter fw, Suite suite) throws IOException{
        if(suite.getNumberOfExecutedQueries() > 0){
            fw.append(LS);
        }
        for(Query q : suite.getExecutedQueries()){
            appendLine(fw, q.getResult().toString());
        }
    }

    /**
     * Writes header of the suite summary file to provided file writer
     *
     * @param fw file writer
     * @param suite suite
     * @throws IOException is some exception occurs
     */
    private void writeSuiteSummaryHeader(FileWriter fw, Suite suite) throws IOException{
        appendLine(fw, "Suite - " + suite.getId());
        appendLine(fw, "============================");
        appendLine(fw, "Start Time:                 " + formatDate(suite.getStartTime()));
        appendLine(fw, "End Time:                   " + formatDate(suite.getEndTime()));
        appendLine(fw, "Elapsed:                    " + timeToString(suite.getDuration()));
        int all = suite.getNumberOfAllQueries();
        int exec = suite.getNumberOfExecutedQueries();
        appendLine(fw, "Number of all queries:      " + all);
        appendLine(fw, "Number of skipped queries:  " + (all - exec));
        appendLine(fw, "Number of executed queries: " + exec);
        appendLine(fw, "Number of passed queries:   " + suite.getNumberOfPassedQueries());
        appendLine(fw, "Number of failed queries:   " + suite.getNumberOfFailedQueries());
        appendLine(fw, "============================");
    }

    /**
     * Writes summary of the scenario to global summary files (totals and errors)
     * and to file {@code Summary_<scenario>.txt}
     *
     * @param scen scenario
     */
    private void writeSummary(Scenario scen){
        try{
            File out = scenarioSummary(outputDir, scen.getId());
            if(!out.exists()){
                out.createNewFile();
            }
            boolean totalsExists = totals.exists();
            boolean errorsExists = errors.exists();
            if(!totalsExists){
                totals.createNewFile();
            }
            if(!errorsExists){
                errors.createNewFile();
            }
            try(FileWriter fwTotals = new FileWriter(totals, totalsExists);
                    FileWriter fwErrors = new FileWriter(errors, errorsExists);
                    FileWriter fw = new FileWriter(out, false)){
                if(!totalsExists){
                    appendLine(fwTotals, "==============");
                    appendLine(fwTotals, "Summary totals");
                    appendLine(fwTotals, "==============");
                    appendLine(fwTotals, pad("Scenario", NAME_PAD) + pad("Pass", RESULTS_PAD)
                            + pad("Fail", RESULTS_PAD) + pad("Skip", RESULTS_PAD) + pad("Total", RESULTS_PAD));
                }
                if(!errorsExists){
                    appendLine(fwErrors, "==============");
                    appendLine(fwErrors, "Summary errors");
                    appendLine(fwErrors, "==============");
                }
                writeScenarioSummaryHeader(fw, scen);
                writeScenarioResults(fw, scen);
                writeScenarioResults(fwErrors, scen);
                int fail = scen.getNumberOfFailedQueries();
                int pass = scen.getNumberOfPassedQueries();
                int all = scen.getNumberOfAllQueries();
                appendLine(fwTotals, pad(scen.getId(), NAME_PAD) + pad(Integer.toString(pass), RESULTS_PAD)
                        + pad(Integer.toString(fail), RESULTS_PAD) + pad(Integer.toString(all - fail - pass), RESULTS_PAD)
                        + pad(Integer.toString(all), RESULTS_PAD));
            }
        } catch (IOException ex) {
            LOG.error("Unable to write result of scenario " + scen.getId(), ex);
        }
    }

    /**
     * Writes failed queries to provided file writer.
     *
     * @param fw file writer
     * @param scen scenario
     * @throws IOException is some error occurs
     */
    private void writeScenarioResults(FileWriter fw, Scenario scen) throws IOException{
        List<Query> failedQueries = scen.getFailedQueries();
        LOG.debug("Failed queries [{}]: {}", failedQueries.size(), failedQueries);
        if(failedQueries == null || failedQueries.isEmpty()){
            return;
        }
        fw.append(LS);
        appendLine(fw, "----------------------");
        appendLine(fw, "Failed queries [" + scen.getId() + "]");
        for(Query q : failedQueries){
            fw.append("    ")
                .append(q.getSuite().getId())
                .append('_')
                .append(q.getId())
                .append(" - ");
            QueryResult qr = q.getResult();
            if(qr.isError()){
                appendLine(fw, qr.getErrors().get(0));
            } else {
                appendLine(fw, qr.getException().toString());
            }
        }
    }

    /**
     * Writers header of the summary file of the scenario to provided file writer.
     *
     * @param fw file writer
     * @param scen scenario
     * @throws IOException is some error occurs
     */
    private void writeScenarioSummaryHeader(FileWriter fw, Scenario scen) throws IOException{
        appendLine(fw, "Scenario - " + scen.getId());
        appendLine(fw, "======================");
        appendLine(fw, "Start Time:           " + formatDate(scen.getStartTime()));
        appendLine(fw, "End Time:             " + formatDate(scen.getEndTime()));
        appendLine(fw, "Elapsed:              " + timeToString(scen.getDuration()));
        appendLine(fw, "----------------------");
        appendLine(fw, "Number of all suites: " + scen.getSuites().size());
        appendLine(fw, pad("Name", NAME_PAD) + pad("Pass", RESULTS_PAD) + pad("Fail", RESULTS_PAD)
            + pad("Skip", RESULTS_PAD) + pad("Total", RESULTS_PAD));
        int overallAll = 0;
        int overallPass = 0;
        int overallFail = 0;
        int overallSkip = 0;
        for(Suite s : scen.getSuites()){
            int all = s.getNumberOfAllQueries();
            int pass = s.getNumberOfPassedQueries();
            int fail = s.getNumberOfFailedQueries();
            int skip = all - fail - pass;
            appendLine(fw, pad(s.getId(), NAME_PAD) + pad(Integer.toString(pass), RESULTS_PAD)
                    + pad(Integer.toString(fail), RESULTS_PAD) + pad(Integer.toString(skip), RESULTS_PAD)
                    + pad(Integer.toString(all), RESULTS_PAD));
            overallAll += all;
            overallPass += pass;
            overallFail += fail;
            overallSkip += skip;
        }
        appendLine(fw, "----------------------");
        appendLine(fw, pad("Totals", NAME_PAD) + pad(Integer.toString(overallPass), RESULTS_PAD)
                + pad(Integer.toString(overallFail), RESULTS_PAD) + pad(Integer.toString(overallSkip), RESULTS_PAD)
                + pad(Integer.toString(overallAll), RESULTS_PAD));
    }

    /**
     * Appends a new (system-dependent) line to the writer {@code wr}.
     *
     * @param wr {@link Writer}
     * @param line line to be appended
     * @throws IOException is some error occurs
     */
    private void appendLine(Writer wr, String line) throws IOException{
        wr.append(line).append(LS);
    }

    /**
     * Formats date. If date is less than or equal to 0, current date is returned.
     *
     * @param date date
     * @return formatted date
     */
    private String formatDate(long date){
        if(date <= 0){
            date = System.currentTimeMillis();
        }
        return DATE_FORMAT.format(new java.util.Date(date));
    }

    /**
     * Pads string {@code str} to required length {@code len}.
     * New spaces (if any) will be appended to the end.
     *
     * @param str string
     * @param length required length. If {@code len} is less than or equals to length of {@code str},
     *      input string will be returned unchanged.
     * @return padded input string
     */
    String pad(String str, int length){
        StringBuilder sb = new StringBuilder(str);
        sb.setLength(Math.max(length, sb.length()));
        return sb.toString().replace('\u0000', ' ');
    }

    /**
     * Converts time in milliseconds to its string representation.
     *
     * @param time time in milliseconds
     * @return string representation if {@code time}
     */
    String timeToString(long time){
        if(time < 0){
            return Long.toString(time);
        }
        long tmp = time / 1000;
        long milis = time - tmp * 1000;
        time = tmp;
        tmp /= 60;
        long sec = time - tmp * 60;
        time = tmp;
        tmp /= 60;
        long min = time - tmp * 60;
        long hours = tmp;
        String hoursStr = Long.toString(hours);
        if(hoursStr.length() == 1){
            hoursStr = '0' + hoursStr;
        }
        String minStr = Long.toString(min);
        if(minStr.length() == 1){
            minStr = '0' + minStr;
        }
        String secStr = Long.toString(sec);
        if(secStr.length() < 2){
            secStr = '0' + secStr;
        }
        String milisStr = Long.toString(milis);
        if(milisStr.length() == 1){
            milisStr = "00" + milisStr;
        } else if(milisStr.length() == 2){
            milisStr = "0" + milisStr;
        }
        return hoursStr + ":" + minStr + ":" + secStr + "." + milisStr;
    }

    public static File scenarioDir(File base, String id){
        return new File(base, id);
    }

    public static File suiteSummary(File scenarioBase, String id, boolean withTimeStamp){
        return withTimeStamp
                ? new File(scenarioBase, id + ".txt")
                : new File(scenarioBase, id + TS_FORMAT.format(new java.util.Date(System.currentTimeMillis()))  + ".txt");
    }

    public static File scenarioSummary(File base, String id){
        return new File(base, "Summary_" + id + ".txt");
    }

    public static File summaryErrors(File base){
        return new File(base, "Summary_errors.txt");
    }

    public static File summaryTotals(File base){
        return new File(base, "Summary_totals.txt");
    }
}
