package com.example.pdf_agent.Tools;

import com.google.adk.tools.Annotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class CalcTool {
    @Annotations.Schema(name = "compute", description = "Computes simple interest or compound growth exactly. Always use this instead of doing maths yourself.")
    public static Map<String, Object> compute(@Annotations.Schema(name = "principal") double principal,
                                              @Annotations.Schema(name = "ratePercent") double rate,
                                              @Annotations.Schema(name = "years") double years,
                                              @Annotations.Schema(name = "compound") boolean compound) {
        double result = compound ? principal * Math.pow(1 + rate / 100, years) : principal * (1 + rate / 100 * years);
        return Map.of("result", BigDecimal.valueOf(result).setScale(2, RoundingMode.HALF_UP).toString());
    }
}