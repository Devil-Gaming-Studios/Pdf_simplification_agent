package com.example.pdf_agent.Tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class Text_Extractor {
    public String text_extractor(String path)
    {
        try(PDDocument document = PDDocument.load(new File(path));)
        {
            PDFTextStripper stripper = new PDFTextStripper();

            String text = stripper.getText(document);

            return text;
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Error extracting text from PDF: " + e.getMessage();
        }


    }
}
