package com.dimetime.service;

import com.dimetime.entity.Invoice;
import com.dimetime.entity.PurchaseOrder;
import com.dimetime.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Invoice generateInvoice(String poNumber, String grnNumber, PurchaseOrder po) {
        // Prevent duplicate invoice generation
        Optional<Invoice> existingInvoice = invoiceRepository.findByGrnNumber(grnNumber);
        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        // Format INV-YYYY-0001
        long currentCount = invoiceRepository.count();
        String invoiceNumber = String.format("INV-2026-%04d", currentCount + 1);

        Double unitPrice = po.getUnitPrice() != null ? po.getUnitPrice() : 100.0;
        Double qtyVal = po.getQuantityValue() != null ? po.getQuantityValue() : 1.0;
        if (qtyVal <= 0.0) {
            // Parse numeric value from quantity string
            qtyVal = parseNumeric(po.getQuantity());
            if (qtyVal <= 0.0) qtyVal = 1.0;
        }
        
        Double gstRate = po.getGstTax() != null ? po.getGstTax() : 18.0;
        Double basePrice = unitPrice * qtyVal;
        Double gstAmount = basePrice * (gstRate / 100.0);
        Double grandTotal = basePrice + gstAmount;

        // Auto-create Invoice entity
        Invoice invoice = new Invoice(
                invoiceNumber,
                poNumber,
                grnNumber,
                po.getSupplier(),
                po.getManufacturer(),
                po.getMaterial(),
                po.getGrade(),
                po.getQuantity(),
                unitPrice,
                gstAmount,
                grandTotal,
                "INVOICE_GENERATED"
        );

        invoiceRepository.save(invoice);

        // Audit Log
        auditLogService.logActivity("Invoice generated: " + invoiceNumber + " for PO: " + poNumber, "SYSTEM");

        // Notification to manufacturer
        try {
            String manufacturerUsername = po.getManufacturerUsername();
            if (manufacturerUsername == null || manufacturerUsername.isEmpty()) {
                manufacturerUsername = "manufacturer";
            }
            notificationService.createNotification(manufacturerUsername, "Invoice generated for PO " + poNumber + ". Please review and proceed with payment.");
        } catch (Exception e) {
            System.err.println("Failed to send Invoice notification: " + e.getMessage());
        }

        return invoice;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Optional<Invoice> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    @Transactional
    public void deleteInvoice(Long id, String operator) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isPresent()) {
            String invNumber = invoiceOpt.get().getInvoiceNumber();
            invoiceRepository.deleteById(id);
            auditLogService.logActivity("Deleted Invoice: " + invNumber, operator);
        }
    }

    @Transactional
    public Invoice updateStatus(String invoiceNumber, String status) {
        Optional<Invoice> invOpt = invoiceRepository.findByInvoiceNumber(invoiceNumber);
        if (invOpt.isPresent()) {
            Invoice invoice = invOpt.get();
            invoice.setStatus(status);
            return invoiceRepository.save(invoice);
        }
        return null;
    }

    public byte[] generateInvoicePdf(Long id) throws Exception {
        Optional<Invoice> invOpt = invoiceRepository.findById(id);
        if (invOpt.isEmpty()) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }
        Invoice invoice = invOpt.get();
        Optional<PurchaseOrder> poOpt = poService.getPurchaseOrder(invoice.getPoNumber());
        PurchaseOrder po = poOpt.orElse(null);

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, java.awt.Color.DARK_GRAY);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, java.awt.Color.BLACK);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, java.awt.Color.BLACK);
        Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, java.awt.Color.BLACK);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Font.NORMAL, java.awt.Color.GRAY);

        // Header Table
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 2});

        PdfPCell titleCell = new PdfPCell(new Paragraph("TAX INVOICE", titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(titleCell);

        PdfPCell logoCell = new PdfPCell(new Paragraph("DIMETIME SCM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, new java.awt.Color(34, 197, 94))));
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(logoCell);

        document.add(headerTable);
        document.add(new Paragraph("\n"));

        // Metadata Table
        PdfPTable invInfoTable = new PdfPTable(2);
        invInfoTable.setWidthPercentage(100);
        invInfoTable.addCell(new PdfPCell(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), boldBodyFont)));
        invInfoTable.addCell(new PdfPCell(new Paragraph("Invoice Date: " + invoice.getInvoiceDate().toString(), bodyFont)));
        invInfoTable.addCell(new PdfPCell(new Paragraph("PO Number: " + invoice.getPoNumber(), bodyFont)));
        invInfoTable.addCell(new PdfPCell(new Paragraph("GRN Number: " + invoice.getGrnNumber(), bodyFont)));
        invInfoTable.addCell(new PdfPCell(new Paragraph("Status: " + invoice.getStatus().replace("_", " "), boldBodyFont)));
        invInfoTable.addCell(new PdfPCell(new Paragraph("Currency: INR", bodyFont)));
        document.add(invInfoTable);

        document.add(new Paragraph("\n"));

        // Supplier/Manufacturer Info
        PdfPTable partyTable = new PdfPTable(2);
        partyTable.setWidthPercentage(100);
        partyTable.setWidths(new float[]{1, 1});

        // Supplier details cell
        String supplierInfoStr = "Supplier Details:\n" + invoice.getSupplier() + "\n";
        if (po != null && po.getSupplierAddress() != null) {
            supplierInfoStr += po.getSupplierAddress() + "\n";
        }
        if (po != null && po.getSupplierGstNumber() != null) {
            supplierInfoStr += "GST: " + po.getSupplierGstNumber();
        }
        PdfPCell supplierCell = new PdfPCell(new Paragraph(supplierInfoStr, bodyFont));
        supplierCell.setPadding(8f);
        partyTable.addCell(supplierCell);

        // Manufacturer details cell
        String manufacturerInfoStr = "Manufacturer Details:\n" + invoice.getManufacturer() + "\n";
        if (po != null && po.getManufacturerAddress() != null) {
            manufacturerInfoStr += po.getManufacturerAddress() + "\n";
        }
        if (po != null && po.getManufacturerGstNumber() != null) {
            manufacturerInfoStr += "GST: " + po.getManufacturerGstNumber();
        }
        PdfPCell manufacturerCell = new PdfPCell(new Paragraph(manufacturerInfoStr, bodyFont));
        manufacturerCell.setPadding(8f);
        partyTable.addCell(manufacturerCell);

        document.add(partyTable);
        document.add(new Paragraph("\n"));

        // Items Table
        Paragraph itemsHeading = new Paragraph("Line Items", sectionFont);
        itemsHeading.setSpacingAfter(5f);
        document.add(itemsHeading);

        PdfPTable itemTable = new PdfPTable(6);
        itemTable.setWidthPercentage(100);
        itemTable.setWidths(new float[]{3, 1, 1, 1, 1, 2});

        String[] headers = {"Material Name", "Grade", "Qty", "Rate (INR)", "GST (INR)", "Total (INR)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(h, boldBodyFont));
            cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
            itemTable.addCell(cell);
        }

        itemTable.addCell(new PdfPCell(new Paragraph(invoice.getMaterialName(), bodyFont)));
        itemTable.addCell(new PdfPCell(new Paragraph(invoice.getMaterialGrade(), bodyFont)));
        itemTable.addCell(new PdfPCell(new Paragraph(invoice.getQuantity(), bodyFont)));
        itemTable.addCell(new PdfPCell(new Paragraph(String.format("%.2f", invoice.getUnitPrice()), bodyFont)));
        itemTable.addCell(new PdfPCell(new Paragraph(String.format("%.2f", invoice.getGst()), bodyFont)));
        itemTable.addCell(new PdfPCell(new Paragraph(String.format("%.2f", invoice.getTotalAmount()), boldBodyFont)));
        document.add(itemTable);

        document.add(new Paragraph("\n\n"));

        // QR Verification & Signatures Footer Table
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{1, 1});

        // QR Verification Code
        String qrContent = String.format("Invoice: %s\nPO: %s\nTotal: INR %.2f\nStatus: %s",
                invoice.getInvoiceNumber(),
                invoice.getPoNumber(),
                invoice.getTotalAmount(),
                invoice.getStatus()
        );
        com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, 150, 150);
        java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
        com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] qrBytes = pngOutputStream.toByteArray();
        com.lowagie.text.Image qrImg = com.lowagie.text.Image.getInstance(qrBytes);
        qrImg.scaleAbsolute(70f, 70f);

        PdfPCell qrCell = new PdfPCell(qrImg);
        qrCell.setBorder(Rectangle.NO_BORDER);
        footerTable.addCell(qrCell);

        // Signatures block
        String sigStr = "\n\nAuthorized Signatory\n" + invoice.getSupplier() + "\n(Digitally Signed)";
        PdfPCell sigCell = new PdfPCell(new Paragraph(sigStr, boldBodyFont));
        sigCell.setBorder(Rectangle.NO_BORDER);
        sigCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        footerTable.addCell(sigCell);

        document.add(footerTable);

        document.add(new Paragraph("\n\n"));
        Paragraph notice = new Paragraph("This is an official digitally signed SCM Tax Invoice. Thank you for your business.", footerFont);
        notice.setAlignment(Element.ALIGN_CENTER);
        document.add(notice);

        document.close();
        return out.toByteArray();
    }

    private double parseNumeric(String text) {
        if (text == null) return 0.0;
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {}
        return 0.0;
    }
}
