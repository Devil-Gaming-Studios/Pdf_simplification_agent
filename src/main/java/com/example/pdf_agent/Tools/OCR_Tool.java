package com.example.pdf_agent.Tools;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class OCR_Tool {
    // OCRs only the given pages (1-based) and returns them flagged as ocr=true
    public List<PageText> ocr_tool(byte[] pdfBytes, List<Integer> pageNumbers) throws IOException, TesseractException {
        Tesseract tesseract = new Tesseract();          // created once, not per page
        tesseract.setDatapath("C:\\Users\\Ravi\\IdeaProjects\\PDF_Agent\\src\\main\\java\\com\\example\\pdf_agent\\Tools\\tessdata");
        tesseract.setLanguage("eng");

        List<PageText> out = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int p : pageNumbers) {
                BufferedImage image = renderer.renderImageWithDPI(p - 1, 300);
                out.add(new PageText(p, tesseract.doOCR(image).trim(), true));
            }
        }
        return out;
    }
}