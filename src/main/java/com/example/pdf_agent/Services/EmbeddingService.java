package com.example.pdf_agent.Services;

import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
    private final OllamaEmbeddingModel model = OllamaEmbeddingModel.builder()
            .baseUrl("http://localhost:11434").modelName("nomic-embed-text").build();

    public float[] embed(String text) { return model.embed(text).content().vector(); }

    public static String toStr(float[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) sb.append(i == 0 ? "" : ",").append(v[i]);
        return sb.toString();
    }
    public static float[] fromStr(String s) {
        String[] p = s.split(",");
        float[] v = new float[p.length];
        for (int i = 0; i < p.length; i++) v[i] = Float.parseFloat(p[i]);
        return v;
    }
    public static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-9);
    }
}