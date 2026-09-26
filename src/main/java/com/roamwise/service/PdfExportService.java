package com.roamwise.service;

import com.roamwise.dto.ActivityResponse;
import com.roamwise.dto.DayResponse;
import com.roamwise.dto.TripResponse;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PdfExportService {

  public byte[] generatePdf(TripResponse trip) throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage(PDRectangle.A4);

    document.addPage(page);
    PDPageContentStream content = new PDPageContentStream(document, page);

    Float y = 750F;

    content.beginText();
    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
    content.newLineAtOffset(50, y);
    content.showText(trip.getDestination());
    content.endText();
    y -= 30;

    content.beginText();
    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 20);
    content.newLineAtOffset(50, y);
    content.showText(
        "From " + trip.getOrigin() + " | " + trip.getStartDate() + " to " + trip.getEndDate());
    content.endText();

    y -= 35;

    for (DayResponse day : trip.getDays()) {
      if (y < 80) {
        content.close();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        content = new PDPageContentStream(document, page);

        y = 750f;
      }

      content.beginText();
      content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
      content.newLineAtOffset(50, y);
      content.showText("Day " + day.getDayNo() + " - " + day.getDayDate());
      content.endText();
      y -= 22;
      for (ActivityResponse activity : day.getActivities()) {
        if (y < 60) {
          content.close();
          page = new PDPage(PDRectangle.A4);
          document.addPage(page);
          content = new PDPageContentStream(document, page);
          y = 750f;
        }

        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        content.newLineAtOffset(60, y);
        content.showText(
            activity.getStartTime() + " - " + activity.getEndTime() + "  " + activity.getName());
        content.endText();
        y -= 16;
      }

      y -= 15; // extra gap between days
    }

    content.close();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    document.save(out);
    document.close();
    return out.toByteArray();
  }
}
