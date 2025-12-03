package uj.wmii.pwj.w7.insurance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.stream.Collectors;

public class FloridaInsurance {

    private static final String ZIP_FILE = "FL_insurance.csv.zip";
    private static final String CSV_FILE = "FL_insurance.csv";
    private static final String COUNT_FILE = "count.txt";
    private static final String TIV2012_FILE = "tiv2012.txt";
    private static final String MOST_VALUABLE_FILE = "most_valuable.txt";
    private static final String MOST_VAL_FORMAT = "%s,%.2f%n";
    private static final String DOUBLE_FORMAT = "%.2f";
    private static final String HEADER = "country,value%n";

    public static void main(String[] args) {

        Path zipPath = Paths.get(ZIP_FILE);
        List<InsuranceEntry> entries;

        try {
            entries = loadFromZip(zipPath);
            createCount(entries);
            createTiv2012(entries);
            createMostValuable(entries);
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    private static List<InsuranceEntry> loadFromZip(Path zipPath) throws IOException {

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry csvEntry = zipFile.getEntry(CSV_FILE);
            if (csvEntry == null) {
                throw new IOException("csv file not found: " + CSV_FILE);
            }

            try (InputStream is = zipFile.getInputStream(csvEntry);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                return br.lines()
                        .skip(1)
                        .map(line -> line.split(","))
                        .map(columns -> new InsuranceEntry(
                                columns[2],
                                Double.parseDouble(columns[7]),
                                Double.parseDouble(columns[8])
                        ))
                        .collect(Collectors.toList());
            }
        }
    }

    private static void createCount(List<InsuranceEntry> entries) throws IOException {

        long count = entries.stream()
                .map(InsuranceEntry::county)
                .distinct()
                .count();

        Files.writeString(Paths.get(COUNT_FILE), Long.toString(count));
    }

    private static void createTiv2012(List<InsuranceEntry> entries) throws IOException {

        double sum = entries.stream()
                .mapToDouble(InsuranceEntry::tiv2012)
                .sum();

        Files.writeString(Paths.get(TIV2012_FILE), String.format(Locale.US, DOUBLE_FORMAT, sum));
    }

    private static void createMostValuable(List<InsuranceEntry> entries) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(HEADER));

        entries.stream()
                .collect(Collectors.groupingBy(
                        InsuranceEntry::county,
                        Collectors.summingDouble(e -> e.tiv2012() - e.tiv2011())
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> sb.append(String.format(Locale.US, MOST_VAL_FORMAT, e.getKey(), e.getValue())));

        Files.writeString(Paths.get(MOST_VALUABLE_FILE), sb.toString());
    }
}