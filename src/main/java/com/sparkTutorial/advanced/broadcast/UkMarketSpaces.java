package com.sparkTutorial.advanced.broadcast;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class UkMarketSpaces {

    public static void main(String[] args) throws Exception {

        Logger.getLogger("org").setLevel(Level.ERROR);

        SparkConf conf = new SparkConf().setAppName("UkMarketSpaces").setMaster("local[1]");

        JavaSparkContext javaSparkContext = new JavaSparkContext(conf);

        final Broadcast<Map<String, String>> postCodeMap = javaSparkContext.broadcast(loadPostCodeMap());

        JavaRDD<String> marketsRdd = javaSparkContext.textFile("in/uk-market-spaces-identifiable-data.csv");

        JavaRDD<String> regions = marketsRdd
                .filter(line -> !line.split(",", -1)[0].equals("Timestamp"))
                .map(line -> {
                    Optional<String> postPrefix = getPostPrefix(line);
                    if (postPrefix.isPresent() && postCodeMap.value().containsKey(postPrefix.get())) {
                        return postCodeMap.value().get(postPrefix.get());
                    }
                    return "Unknown";
                });
        for (Map.Entry<String, Long> regionCounts : regions.countByValue().entrySet()) {
            System.out.println(regionCounts.getKey() + " : " + regionCounts.getValue());
        }
    }

    private static Optional<String> getPostPrefix(String line) {
        String[] splits = line.split(",", -1);
        String postcode = splits[4];
        if (postcode.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(postcode.split(" ")[0]);
    }

    private static Map<String, String> loadPostCodeMap() throws FileNotFoundException {
        Scanner postCode = new Scanner(new File("in/uk-postcode.csv"));
        Map<String, String> postCodeMap = new HashMap<>();
        while (postCode.hasNextLine()) {
            String line = postCode.nextLine();
            String[] splits = line.split(",", -1);
            postCodeMap.put(splits[0], splits[7]);
        }
        return postCodeMap;
    }

}