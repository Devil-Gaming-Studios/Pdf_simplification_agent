package com.example.pdf_agent.Services;

import com.example.pdf_agent.Entities.Section;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfRenderService {
    private static final PDType1Font BODY = PDType1Font.HELVETICA;
    private static final PDType1Font BOLD = PDType1Font.HELVETICA_BOLD;
    private static final float MARGIN = 50, WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private PDDocument doc;
    private PDPageContentStream cs;
    private float y;

    public byte[] render(String title, List<Section> sections) throws IOException {
        try (PDDocument d = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc = d;
            newPage();
            line(title + " (Simplified)", BOLD, 16);
            line("Simplified copy for easier reading. The original document is the authoritative version.", BODY, 9);
            y -= 10;

            for (Section s : sections) {
                y -= 8;
                line(s.getTitle() + "  (pages " + s.getPageStart() + "-" + s.getPageEnd() + ")", BOLD, 13);
                for (String para : s.getSimplifiedText().split("\n")) line(para, BODY, 11);

                List<String> warnings = new ArrayList<>();
                try { for (JsonNode n : mapper.readTree(s.getWarnings())) warnings.add(n.asText()); } catch (Exception ignored) {}
                if (!warnings.isEmpty()) {
                    line("Important warnings:", BOLD, 11);
                    for (String w : warnings) line("- " + w, BODY, 11);
                }
            }
            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void newPage() throws IOException {
        if (cs != null) cs.close();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = PDRectangle.A4.getHeight() - MARGIN;
    }

    // wraps text to the page width and starts a new page when needed
    private void line(String text, PDType1Font font, float size) throws IOException {
        text = text.replace("\u20B9", "Rs.").replace("\u2022", "-").replaceAll("[^\\x20-\\x7E\\u00A0-\\u00FF]", "?");
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (font.getStringWidth(test) / 1000 * size > WIDTH && cur.length() > 0) {
                write(cur.toString(), font, size);
                cur = new StringBuilder(word);
            } else cur = new StringBuilder(test);
        }
        write(cur.toString(), font, size);
    }

    private void write(String s, PDType1Font font, float size) throws IOException {
        if (y < MARGIN + size) newPage();
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(s);
        cs.endText();
        y -= size * 1.4f;
    }
}