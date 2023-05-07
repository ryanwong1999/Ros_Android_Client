package com.utils;

public class LanguageSettings {

    private static LanguageSettings language = null;

    private String currentLanguage = "zh";

    private LanguageSettings() {
    }

    public static LanguageSettings getInstance() {
        if (language == null) {
            language = new LanguageSettings();
        } else {

        }
        return language;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void switchCurrentLanguage() {
        if (currentLanguage.equals("zh")) {
            currentLanguage = "en";
        } else {
            currentLanguage = "zh";
        }
    }
}

