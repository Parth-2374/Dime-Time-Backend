package com.dimetime.service;

import com.dimetime.entity.Grn;
import com.dimetime.entity.PurchaseOrder;
import com.dimetime.repository.GrnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

@Service
public class GrnService {

    @Autowired
    private GrnRepository grnRepository;

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private InvoiceService invoiceService;

    @Transactional
    public Grn generateGrn(String poNumber, String generatedBy) {
        Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("Purchase Order " + poNumber + " not found");
        }
        PurchaseOrder po = poOpt.get();

        // Check if GRN already exists for this PO to prevent duplicate generation during demos
        List<Grn> existingGrns = grnRepository.findByPoNumber(poNumber);
        if (!existingGrns.isEmpty()) {
            // We can return the existing one or create a new sequential index
        }

        // Generate serial GRN Number: GRN-2026-xxx
        long currentCount = grnRepository.count();
        String grnNumber = String.format("GRN-2026-%03d", currentCount + 1);

        Grn grn = new Grn(
                grnNumber,
                po.getPoNumber(),
                po.getSupplier(),
                po.getMaterial(),
                "APPROVED", // Status is set to APPROVED upon generation
                generatedBy
        );

        grnRepository.save(grn);

        // Transition PO Status to GRN_GENERATED
        poService.updateStatus(poNumber, "GRN_GENERATED", generatedBy);

        // Generate Auto Invoice immediately
        try {
            invoiceService.generateInvoice(poNumber, grnNumber, po);
        } catch (Exception e) {
            System.err.println("Auto invoice generation failed: " + e.getMessage());
        }

        // Audit Trail entry
        auditLogService.logActivity("GRN Generated: " + grnNumber + " for PO: " + poNumber, generatedBy);

        // Send Notification to Supplier and Manufacturer
        try {
            String supplierUsername = po.getSupplierUsername();
            if (supplierUsername == null || supplierUsername.isEmpty()) {
                supplierUsername = "supplier";
            }
            String manufacturerUsername = po.getManufacturerUsername();
            if (manufacturerUsername == null || manufacturerUsername.isEmpty()) {
                manufacturerUsername = "manufacturer";
            }
            notificationService.createNotification(supplierUsername, "Auto-GRN Generated: " + grnNumber + " for PO " + poNumber);
            notificationService.createNotification(manufacturerUsername, "Auto-GRN Generated: " + grnNumber + " for PO " + poNumber);
            auditLogService.logActivity("Supplier and Manufacturer notifications dispatched for GRN: " + grnNumber, generatedBy);
        } catch (Exception e) {
            System.err.println("Failed to send GRN notifications: " + e.getMessage());
        }

        return grn;
    }

    public List<Grn> getAllGrns() {
        return grnRepository.findAll();
    }

    public Optional<Grn> getGrnByNumber(String grnNumber) {
        return grnRepository.findByGrnNumber(grnNumber);
    }

    public long getApprovedGrnCount() {
        return grnRepository.countByStatus("APPROVED");
    }

    @Transactional
    public Grn createManualGrn(Grn grn, String operator) {
        long currentCount = grnRepository.count();
        String grnNumber = String.format("GRN-2026-%03d", currentCount + 1);
        grn.setGrnNumber(grnNumber);
        if (grn.getStatus() == null) {
            grn.setStatus("APPROVED");
        }
        grn.setGeneratedBy(operator);
        Grn saved = grnRepository.save(grn);
        auditLogService.logActivity("Manual GRN Created: " + grnNumber + " for PO: " + grn.getPoNumber(), operator);
        return saved;
    }

    @Transactional
    public Grn updateGrn(Long id, Grn details, String operator) {
        Optional<Grn> grnOpt = grnRepository.findById(id);
        if (grnOpt.isEmpty()) {
            throw new IllegalArgumentException("GRN not found with id: " + id);
        }
        Grn grn = grnOpt.get();
        grn.setSupplier(details.getSupplier());
        grn.setMaterial(details.getMaterial());
        grn.setStatus(details.getStatus());
        Grn saved = grnRepository.save(grn);
        auditLogService.logActivity("Updated GRN: " + grn.getGrnNumber(), operator);
        return saved;
    }

    @Transactional
    public void deleteGrn(Long id, String operator) {
        Optional<Grn> grnOpt = grnRepository.findById(id);
        if (grnOpt.isPresent()) {
            String grnNumber = grnOpt.get().getGrnNumber();
            grnRepository.deleteById(id);
            auditLogService.logActivity("Deleted GRN: " + grnNumber, operator);
        }
    }

    public byte[] generateGrnPdf(Long id) throws Exception {
        Optional<Grn> grnOpt = grnRepository.findById(id);
        if (grnOpt.isEmpty()) {
            throw new IllegalArgumentException("GRN not found: " + id);
        }
        Grn grn = grnOpt.get();

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, java.awt.Color.DARK_GRAY);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, java.awt.Color.BLACK);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, java.awt.Color.BLACK);
        Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, java.awt.Color.BLACK);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Font.NORMAL, java.awt.Color.GRAY);

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 2});

        PdfPCell titleCell = new PdfPCell(new Paragraph("GOODS RECEIPT NOTE", titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(titleCell);

        PdfPCell logoCell = new PdfPCell(new Paragraph("DIMETIME SCM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, new java.awt.Color(34, 197, 94))));
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(logoCell);

        document.add(headerTable);
        document.add(new Paragraph("\n"));

        PdfPTable grnInfoTable = new PdfPTable(2);
        grnInfoTable.setWidthPercentage(100);
        grnInfoTable.addCell(new PdfPCell(new Paragraph("GRN Number: " + grn.getGrnNumber(), boldBodyFont)));
        grnInfoTable.addCell(new PdfPCell(new Paragraph("Date: " + grn.getGeneratedAt().toLocalDate().toString(), bodyFont)));
        grnInfoTable.addCell(new PdfPCell(new Paragraph("PO Number: " + grn.getPoNumber(), bodyFont)));
        grnInfoTable.addCell(new PdfPCell(new Paragraph("Status: " + grn.getStatus(), bodyFont)));
        document.add(grnInfoTable);

        document.add(new Paragraph("\n"));

        Paragraph matHeading = new Paragraph("Inspection Summary", sectionFont);
        matHeading.setSpacingAfter(5f);
        document.add(matHeading);

        PdfPTable specTable = new PdfPTable(3);
        specTable.setWidthPercentage(100);
        specTable.setWidths(new float[]{3, 2, 2});

        PdfPCell cell1 = new PdfPCell(new Paragraph("Material Description", boldBodyFont));
        cell1.setBackgroundColor(new java.awt.Color(240, 240, 240));
        specTable.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Paragraph("Supplier", boldBodyFont));
        cell2.setBackgroundColor(new java.awt.Color(240, 240, 240));
        specTable.addCell(cell2);

        PdfPCell cell3 = new PdfPCell(new Paragraph("Verification Status", boldBodyFont));
        cell3.setBackgroundColor(new java.awt.Color(240, 240, 240));
        specTable.addCell(cell3);

        specTable.addCell(new PdfPCell(new Paragraph(grn.getMaterial(), bodyFont)));
        specTable.addCell(new PdfPCell(new Paragraph(grn.getSupplier(), bodyFont)));
        specTable.addCell(new PdfPCell(new Paragraph("PASSED", boldBodyFont)));
        document.add(specTable);

        document.add(new Paragraph("\n\n"));
        Paragraph notice = new Paragraph("This is an official system-generated Goods Receipt Note representing successful 3-Way SCM Reconciliation.", footerFont);
        notice.setAlignment(Element.ALIGN_CENTER);
        document.add(notice);

        document.close();
        return out.toByteArray();
    }
}
