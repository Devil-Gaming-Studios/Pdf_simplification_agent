package com.example.pdf_agent.Services;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GuardrailService {
    private static final Pattern ADVICE = Pattern.compile(
            "(?i)\\b(should i (buy|sell|hold|invest)|which (fund|stock) (should|is best)|is it (a )?good (time|idea) to (buy|sell|invest)|what should i invest in)\\b");
    private static final Pattern NUM = Pattern.compile("\\d+(?:[.,]\\d+)*%?");

    public boolean isAdviceRequest(String msg) { return ADVICE.matcher(msg).find(); }

    // numbers in the answer that do not appear anywhere in the source text
    public List<String> unsupportedNumbers(String answer, String sourceText) {
        String src = sourceText.replace(",", "");
        List<String> bad = new ArrayList<>();
        Matcher m = NUM.matcher(answer.replaceAll("(?i)page\\s*\\d+", ""));   // ignore page citations
        while (m.find()) {
            String n = m.group().replace(",", "");
            if (n.length() > 1 && !src.contains(n)) bad.add(m.group());
        }
        return bad;
    }

    public List<String> ocrWarnings(String taggedPdfText) {
        List<String> w = new ArrayList<>();
        Matcher m = Pattern.compile("\\[Page (\\d+), OCR\\]").matcher(taggedPdfText);
        while (m.find()) w.add("Page " + m.group(1) + " was read with OCR and may contain errors.");
        return w;
    }
}