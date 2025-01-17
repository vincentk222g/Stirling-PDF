package stirling.software.SPDF.controller.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import stirling.software.SPDF.model.api.general.CropPdfForm;
import stirling.software.SPDF.utils.WebResponseUtils;

@RestController
@RequestMapping("/api/v1/general")
@Tag(name = "General", description = "General APIs")
public class BookletUnbind {

    private static final Logger logger = LoggerFactory.getLogger(BookletUnbind.class);

    @PostMapping(value = "/bookletunbind", consumes = "multipart/form-data")
    @Operation(
            summary = "Unbind Scanned Booklet document",
            description =
                    "This operation takes an input PDF file and crops it according to the given coordinates. Input:PDF Output:PDF Type:SISO")
    public ResponseEntity<byte[]> cropPdf(@ModelAttribute CropPdfForm form) throws IOException {
        PDDocument sourceDocument = Loader.loadPDF(form.getFileInput().getBytes());

        PDDocument newDocument = new PDDocument();

        int totalPages = sourceDocument.getNumberOfPages();

        LayerUtility layerUtility = new LayerUtility(newDocument);

        logger.info("split pages in half, alternating sides, right and left");
        logger.info("-------------------------------------------------------");

        for (int i = 0; i < totalPages; i++) {
            PDPage sourcePage = sourceDocument.getPage(i);
            // Create a new page with the size of the source page
            PDPage newPage = new PDPage(sourcePage.getMediaBox());
            float rotation = sourcePage.getRotation();
            logger.info("Rotation " + rotation);
            float width;
            float height;
            width = sourcePage.getMediaBox().getWidth();
            height = sourcePage.getMediaBox().getHeight();
            logger.info("Width " + width);
            logger.info("Height " + height);
            newDocument.addPage(newPage);
            PDPageContentStream contentStream =
                    new PDPageContentStream(newDocument, newPage, AppendMode.OVERWRITE, true, true);

            // Import the source page as a form XObject
            PDFormXObject formXObject = layerUtility.importPageAsForm(sourceDocument, i);

            float x, y = 0;
            float recWidth = width / 2;
            float recHeight = height;
            if (i % 2 == 0) { // Si i est paire
                x = width / 2;
            } else { // Si i est impaire
                x = 0;
            }
            logger.info(
                    "Size and Position : x="
                            + x
                            + ", y="
                            + y
                            + ", Width="
                            + recWidth
                            + ", Height="
                            + recHeight);

            contentStream.saveGraphicsState();
            // Define the crop area
            contentStream.addRect(x, y, recWidth, recHeight);
            contentStream.clip();
            // Draw the entire formXObject
            contentStream.drawForm(formXObject);
            contentStream.restoreGraphicsState();
            contentStream.close();
            // Now, set the new page's media box to the cropped size
            newPage.setMediaBox(new PDRectangle(x, y, recWidth, recHeight));
        }

        logger.info(
                "add pages in reverse order and split pages in half, alternating sides, right and left");
        for (int i = totalPages - 1; i >= 0; i--) {
            PDPage sourcePage = sourceDocument.getPage(i);
            // Create a new page with the size of the source page
            PDPage newPage = new PDPage(sourcePage.getMediaBox());
            float rotation = sourcePage.getRotation();
            logger.info("Rotation " + rotation);
            float width;
            float height;
            width = sourcePage.getMediaBox().getWidth();
            height = sourcePage.getMediaBox().getHeight();
            logger.info("Width " + width);
            logger.info("Height " + height);
            newDocument.addPage(newPage);
            PDPageContentStream contentStream =
                    new PDPageContentStream(newDocument, newPage, AppendMode.OVERWRITE, true, true);

            // Import the source page as a form XObject
            PDFormXObject formXObject = layerUtility.importPageAsForm(sourceDocument, i);

            float x, y = 0;
            float recWidth = width / 2;
            float recHeight = height;
            if (i % 2 == 0) { // Si i est paire
                x = 0;
            } else { // Si i est impaire
                x = width / 2;
            }
            logger.info(
                    "Size and Position : x="
                            + x
                            + ", y="
                            + y
                            + ", Width="
                            + recWidth
                            + ", Height="
                            + recHeight);

            contentStream.saveGraphicsState();
            // Define the crop area
            contentStream.addRect(x, y, recWidth, recHeight);
            contentStream.clip();
            // Draw the entire formXObject
            contentStream.drawForm(formXObject);
            contentStream.restoreGraphicsState();
            contentStream.close();
            // Now, set the new page's media box to the cropped size
            newPage.setMediaBox(new PDRectangle(x, y, recWidth, recHeight));
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newDocument.save(baos);
        newDocument.close();
        sourceDocument.close();

        byte[] pdfContent = baos.toByteArray();
        return WebResponseUtils.bytesToWebResponse(
                pdfContent,
                form.getFileInput().getOriginalFilename().replaceFirst("[.][^.]+$", "")
                        + "_unbind.pdf");
    }
}
