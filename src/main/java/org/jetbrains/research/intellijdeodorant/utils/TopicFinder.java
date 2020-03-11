package org.jetbrains.research.intellijdeodorant.utils;

import org.jetbrains.research.intellijdeodorant.utils.math.HumaniseCamelCase;
import org.jetbrains.research.intellijdeodorant.utils.math.Stemmer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class TopicFinder {
    private static final ArrayList<String> stopWords = getStopWords();

    public static List<String> findTopics(List<String> codeElements) {
        HumaniseCamelCase humaniser = new HumaniseCamelCase();
        List<String> lowerCaseWords = new ArrayList<>();
        for (String codeElement : codeElements) {
            //split based on underscores
            String[] tokens = codeElement.split("_");
            for (String token : tokens) {
                //split based on camel case
                String[] camelCaseTokens = humaniser.humanise(token).split("\\s");
                for (String camelCaseToken : camelCaseTokens) {
                    String lowerCaseToken = camelCaseToken.toLowerCase();
                    //remove stop words
                    if (!stopWords.contains(lowerCaseToken)) {
                        //stem the word
                        Stemmer stemmer = new Stemmer();
                        stemmer.add(lowerCaseToken.toCharArray(), lowerCaseToken.length());
                        stemmer.stem();
                        String stemmed = stemmer.toString();
                        if (!stemmed.isEmpty())
                            lowerCaseWords.add(stemmed);
                    }
                }
            }
        }

        //count the frequencies of the words
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String word : lowerCaseWords) {
            if (frequencyMap.containsKey(word)) {
                frequencyMap.put(word, frequencyMap.get(word) + 1);
            } else {
                frequencyMap.put(word, 1);
            }
        }
        List<String> topFrequentWords = new ArrayList<>();
        int max = 0;
        for (String key : frequencyMap.keySet()) {
            int frequency = frequencyMap.get(key);
            if (frequency > max) {
                max = frequency;
                topFrequentWords.clear();
                topFrequentWords.add(key);
            } else if (frequency == max) {
                topFrequentWords.add(key);
            }
        }
        return topFrequentWords;
    }

    private static ArrayList<String> getStopWords() {
        ArrayList<String> stopWords = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(TopicFinder.class.getClassLoader().getResourceAsStream("glasgowstoplist.txt"))))) {
            String next = in.readLine();
            while (next != null) {
                stopWords.add(next);
                next = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopWords;
    }
}