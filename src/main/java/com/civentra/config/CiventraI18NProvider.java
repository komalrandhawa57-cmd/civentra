package com.civentra.config;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CiventraI18NProvider implements I18NProvider {

    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(Locale.ENGLISH, new Locale("hi", "IN"));
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null)
            return "";
        return key; // Default English fallback
    }
}