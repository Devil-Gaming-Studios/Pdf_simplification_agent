package com.example.pdf_agent.Tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class Text_Extractor {
    public List<PageText> text_extractor(byte[] pdfBytes) throws IOException {
        List<PageText> pages = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= document.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                pages.add(new PageText(p, stripper.getText(document).trim(), false));
            }
        }
        return pages;
    }
}
