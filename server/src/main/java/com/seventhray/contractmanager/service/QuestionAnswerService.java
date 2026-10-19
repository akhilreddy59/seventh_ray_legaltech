package com.seventhray.contractmanager.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QuestionAnswerService {
    private static final String NOT_FOUND = "I could not find an answer in this document.";
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]{3,}");

    public String answer(String document, String question) {
        Set<String> keywords = words(question);
        if (keywords.isEmpty()) return NOT_FOUND;
        String[] sentences = document.split("(?<=[.!?])\\s+|\\n+");
        RankedSentence best = null;
        for (String value : sentences) {
            String sentence = value.trim();
            if (sentence.isEmpty()) continue;
            int score = overlap(words(sentence), keywords);
            if (score > 0 && (best == null || score > best.score())) best = new RankedSentence(sentence, score);
        }
        return best == null ? NOT_FOUND : best.sentence();
    }

    private static Set<String> words(String text) {
        var matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
        return matcher.results().map(match -> match.group()).collect(Collectors.toSet());
    }

    private static int overlap(Set<String> sentence, Set<String> question) {
        return (int) question.stream().filter(sentence::contains).count();
    }

    private record RankedSentence(String sentence, int score) {}
}
