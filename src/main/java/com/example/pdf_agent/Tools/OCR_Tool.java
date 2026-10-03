package com.example.pdf_agent.Tools;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OCR_Tool {
    public String ocr_tool(byte[] pdfBytes) {

                try(PDDocument document = PDDocument.load(pdfBytes);)
                {
                    PDFRenderer pdfRenderer = new PDFRenderer(document);
                    String result = "";
                    for(int i = 0; i < document.getNumberOfPages();i++) {
                        BufferedImage image = pdfRenderer.renderImageWithDPI(i, 300);
                        Tesseract tesseract = new Tesseract();
                        try {

                            tesseract.setDatapath("tessdata");
                            tesseract.setLanguage("eng");

                            String text
                                    = tesseract.doOCR(image);

                            result = result + "page " + (i + 1) + ": " + text + "\n";
                        } catch (TesseractException e) {
                            e.printStackTrace();
                            return "Error performing OCR: " + e.getMessage();
                        }
                    }
                    return result;
                }
                catch(IOException e) {
                    e.printStackTrace();
                    return "Error loading PDF document: " + e.getMessage();
                }



    }
}
