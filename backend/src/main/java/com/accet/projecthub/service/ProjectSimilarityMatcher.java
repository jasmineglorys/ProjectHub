package com.accet.projecthub.service;

import com.accet.projecthub.entity.Project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ProjectSimilarityMatcher {

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "for", "in", "of", "on", "the", "to", "with",
            "app", "application", "project", "system", "using", "based"
    );

    private ProjectSimilarityMatcher() {
    }

    public static List<String> matchingFields(Project first, Project second) {
        List<String> matchingFields = new ArrayList<>();
        if (similarText(first.getTitle(), second.getTitle(), 0.62, 2)) {
            matchingFields.add("Title");
        }
        if (similarText(first.getDescription(), second.getDescription(), 0.62, 10)) {
            matchingFields.add("Description / abstract");
        }
        if (similarText(first.getAchievement(), second.getAchievement(), 0.68, 6)) {
            matchingFields.add("Achievement details");
        }
        if (similarTechnologies(first.getTechnologies(), second.getTechnologies())) {
            matchingFields.add("Technologies");
        }
        return matchingFields;
    }

    private static boolean similarText(String first, String second,
                                       double jaccardThreshold, int minimumSharedWords) {
        String normalizedFirst = normalize(first);
        String normalizedSecond = normalize(second);
        if (normalizedFirst.isEmpty() || normalizedSecond.isEmpty()) return false;
        if (normalizedFirst.equals(normalizedSecond)) return true;

        Set<String> firstWords = words(normalizedFirst);
        Set<String> secondWords = words(normalizedSecond);
        if (firstWords.isEmpty() || secondWords.isEmpty()) return false;

        Set<String> sharedWords = new HashSet<>(firstWords);
        sharedWords.retainAll(secondWords);
        if (sharedWords.size() < minimumSharedWords) return false;

        Set<String> allWords = new HashSet<>(firstWords);
        allWords.addAll(secondWords);
        double jaccard = (double) sharedWords.size() / allWords.size();
        double containment = (double) sharedWords.size()
                / Math.min(firstWords.size(), secondWords.size());
        return jaccard >= jaccardThreshold || containment >= 0.9;
    }

    private static boolean similarTechnologies(Set<String> first, Set<String> second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) return false;

        Set<String> firstTechnologies = normalizeTechnologies(first);
        Set<String> secondTechnologies = normalizeTechnologies(second);
        Set<String> shared = new HashSet<>(firstTechnologies);
        shared.retainAll(secondTechnologies);
        if (shared.size() < 2) return false;

        Set<String> all = new HashSet<>(firstTechnologies);
        all.addAll(secondTechnologies);
        return (double) shared.size() / all.size() >= 0.75;
    }

    private static Set<String> normalizeTechnologies(Set<String> technologies) {
        Set<String> normalized = new HashSet<>();
        for (String technology : technologies) {
            if (technology != null && !technology.isBlank()) {
                normalized.add(normalize(technology));
            }
        }
        return normalized;
    }

    private static Set<String> words(String value) {
        Set<String> words = new HashSet<>();
        for (String word : value.split(" ")) {
            if (word.length() > 1 && !STOP_WORDS.contains(word)) words.add(word);
        }
        return words;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return NON_WORD.matcher(value.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }
}