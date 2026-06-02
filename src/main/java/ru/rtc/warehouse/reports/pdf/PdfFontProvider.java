package ru.rtc.warehouse.reports.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PdfFontProvider {

    private final BaseFont baseFontRegular;
    private final BaseFont baseFontBold;

    public PdfFontProvider() {
        BaseFont regular = null;
        BaseFont bold = null;

        try {
            String regularPath = new ClassPathResource("fonts/LiberationSans-Regular.ttf").getURL().toString();
            String boldPath = new ClassPathResource("fonts/LiberationSans-Bold.ttf").getURL().toString();
            regular = BaseFont.createFont(regularPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            bold = BaseFont.createFont(boldPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            log.info("LiberationSans loaded, кириллица поддерживается");
        } catch (Exception e) {
            log.warn("LiberationSans not found, trying Arial");
            try {
                regular = BaseFont.createFont("Arial", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                bold = BaseFont.createFont("Arial,Bold", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                log.info("System Arial loaded");
            } catch (Exception ex) {
                log.error("No font with Cyrillic support available", ex);
                throw new RuntimeException("Cannot initialize PDF fonts", ex);
            }
        }

        this.baseFontRegular = regular;
        this.baseFontBold = bold;
    }

    public Font regular(float size) {
        return new Font(baseFontRegular, size, Font.NORMAL);
    }

    public Font bold(float size) {
        return new Font(baseFontBold, size, Font.BOLD);
    }

    public Font regular(float size, BaseColor color) {
        Font f = regular(size);
        f.setColor(color);
        return f;
    }
}