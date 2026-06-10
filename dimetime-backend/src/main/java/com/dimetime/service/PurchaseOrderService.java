package com.dimetime.service;

import com.dimetime.entity.PurchaseOrder;
import com.dimetime.entity.User;
import com.dimetime.entity.Company;
import com.dimetime.repository.PurchaseOrderRepository;
import com.dimetime.repository.UserRepository;
import com.dimetime.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<PurchaseOrder> getPurchaseOrder(String poNumber) {
        return purchaseOrderRepository.findByPoNumber(poNumber);
    }

    public List<PurchaseOrder> getPOsBySupplier(String supplier) {
        Optional<User> userOpt = userRepository.findByUsername(supplier);
        if (userOpt.isPresent()) {
            return purchaseOrderRepository.findBySupplierId(userOpt.get().getId());
        }
        return purchaseOrderRepository.findBySupplierOrderByCreatedAtDesc(supplier);
    }

    public List<PurchaseOrder> getPOsByManufacturer(String manufacturer) {
        Optional<User> userOpt = userRepository.findByUsername(manufacturer);
        if (userOpt.isPresent()) {
            return purchaseOrderRepository.findByManufacturerId(userOpt.get().getId());
        }
        return purchaseOrderRepository.findByManufacturerOrderByCreatedAtDesc(manufacturer);
    }

    public List<PurchaseOrder> getPOsBySupplierId(Long supplierId) {
        return purchaseOrderRepository.findBySupplierId(supplierId);
    }

    public List<PurchaseOrder> getPOsByManufacturerId(Long manufacturerId) {
        return purchaseOrderRepository.findByManufacturerId(manufacturerId);
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrder po) {
        long currentCount = purchaseOrderRepository.count();
        String poNumber = String.format("PO-2026-%03d", currentCount + 1);
        po.setPoNumber(poNumber);
        po.setStatus("PENDING");

        // Try to link supplier company
        if (po.getSupplier() != null) {
            Optional<User> sUserOpt = userRepository.findByUsername(po.getSupplier());
            if (sUserOpt.isEmpty()) {
                // Try searching by company name/user company name
                List<User> users = userRepository.findAll();
                for (User u : users) {
                    if ("SUPPLIER".equalsIgnoreCase(u.getRole()) && 
                        (po.getSupplier().equalsIgnoreCase(u.getCompanyName()) || 
                         (u.getCompany() != null && po.getSupplier().equalsIgnoreCase(u.getCompany().getName())))) {
                        sUserOpt = Optional.of(u);
                        break;
                    }
                }
            }
            if (sUserOpt.isPresent()) {
                User sUser = sUserOpt.get();
                po.setSupplierUser(sUser);
                po.setSupplierUsername(sUser.getUsername());
                po.setSupplierCompany(sUser.getCompany());
                if (sUser.getCompany() != null) {
                    po.setSupplierCompanyName(sUser.getCompany().getName());
                    po.setSupplierAddress(sUser.getCompany().getAddress());
                    po.setSupplierGstNumber(sUser.getCompany().getGstNumber());
                } else {
                    po.setSupplierCompanyName(sUser.getCompanyName());
                    po.setSupplierAddress(sUser.getAddress());
                    po.setSupplierGstNumber(sUser.getGstNumber());
                }
                String companyName = sUser.getCompany() != null ? sUser.getCompany().getName() : sUser.getCompanyName();
                po.setSupplier(companyName != null ? companyName : sUser.getUsername());
            } else {
                // Try searching by company name directly in companyRepository
                Optional<Company> sComp = companyRepository.findByName(po.getSupplier());
                if (sComp.isPresent()) {
                    po.setSupplierCompany(sComp.get());
                    po.setSupplierCompanyName(sComp.get().getName());
                    po.setSupplierAddress(sComp.get().getAddress());
                    po.setSupplierGstNumber(sComp.get().getGstNumber());
                }
            }
        }

        // Try to link manufacturer company
        if (po.getManufacturer() != null) {
            Optional<User> mUserOpt = userRepository.findByUsername(po.getManufacturer());
            if (mUserOpt.isEmpty()) {
                // Try searching by company name/user company name
                List<User> users = userRepository.findAll();
                for (User u : users) {
                    if ("MANUFACTURER".equalsIgnoreCase(u.getRole()) && 
                        (po.getManufacturer().equalsIgnoreCase(u.getCompanyName()) || 
                         (u.getCompany() != null && po.getManufacturer().equalsIgnoreCase(u.getCompany().getName())))) {
                        mUserOpt = Optional.of(u);
                        break;
                    }
                }
            }
            if (mUserOpt.isPresent()) {
                User mUser = mUserOpt.get();
                po.setManufacturerUser(mUser);
                po.setManufacturerUsername(mUser.getUsername());
                po.setManufacturerCompany(mUser.getCompany());
                if (mUser.getCompany() != null) {
                    po.setManufacturerCompanyName(mUser.getCompany().getName());
                    po.setManufacturerAddress(mUser.getCompany().getAddress());
                    po.setManufacturerGstNumber(mUser.getCompany().getGstNumber());
                } else {
                    po.setManufacturerCompanyName(mUser.getCompanyName());
                    po.setManufacturerAddress(mUser.getAddress());
                    po.setManufacturerGstNumber(mUser.getGstNumber());
                }
                String companyName = mUser.getCompany() != null ? mUser.getCompany().getName() : mUser.getCompanyName();
                po.setManufacturer(companyName != null ? companyName : mUser.getUsername());
            }
        }

        PurchaseOrder saved = purchaseOrderRepository.save(po);

        // Pre-generate QR code and save it to the database
        try {
            byte[] qrBytes = generateQrCodeBytes(saved);
            saved.setQrCodeImage(qrBytes);
            saved = purchaseOrderRepository.save(saved);
        } catch (Exception e) {
            System.err.println("Failed to pre-generate QR code for PO: " + e.getMessage());
        }

        auditLogService.logActivity("Generated PO " + poNumber + " assigned to manufacturer: " + po.getManufacturer(), 
                po.getSupplierUser() != null ? po.getSupplierUser().getUsername() : po.getSupplier());
        return saved;
    }

    @Transactional
    public PurchaseOrder updateStatus(String poNumber, String status, String operator) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findByPoNumber(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("PO not found: " + poNumber);
        }
        PurchaseOrder po = poOpt.get();
        po.setStatus(status);
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        auditLogService.logActivity("Updated PO " + poNumber + " status to " + status, operator);
        return saved;
    }

    @Transactional
    public PurchaseOrder updateDispatch(String poNumber, String carrier, String trackingNumber, String operator) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findByPoNumber(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("PO not found: " + poNumber);
        }
        PurchaseOrder po = poOpt.get();
        po.setDispatchCarrier(carrier);
        po.setDispatchTrackingNumber(trackingNumber);
        po.setStatus("DISPATCHED");
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        auditLogService.logActivity("Material dispatched for PO " + poNumber + " by " + operator, operator);
        auditLogService.logActivity("Dispatched material for PO " + poNumber + " via " + carrier + " (Tracking: " + trackingNumber + ")", operator);

        String supplierUsername = po.getSupplierUsername();
        if (supplierUsername == null || supplierUsername.isEmpty()) {
            supplierUsername = "supplier";
        }
        notificationService.createNotification(supplierUsername, "Material dispatched for PO " + poNumber + " via " + carrier + " (Tracking: " + trackingNumber + ").");

        return saved;
    }

    @Transactional
    public PurchaseOrder updateMtcFileName(String poNumber, String mtcFileName, String operator) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findByPoNumber(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("PO not found: " + poNumber);
        }
        PurchaseOrder po = poOpt.get();
        po.setMtcFileName(mtcFileName);
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        auditLogService.logActivity("Linked MTC Certificate " + mtcFileName + " to PO " + poNumber, operator);
        return saved;
    }

    public byte[] generateQrCodeBytes(PurchaseOrder po) throws Exception {
        String qrContent = String.format(
            "{\"poNumber\":\"%s\",\"supplier\":\"%s\",\"manufacturer\":\"%s\",\"grade\":\"%s\",\"quantity\":\"%s\",\"dimensions\":\"%s\",\"status\":\"%s\"}",
            po.getPoNumber(),
            po.getSupplierCompanyName() != null ? po.getSupplierCompanyName() : (po.getSupplier() != null ? po.getSupplier() : ""),
            po.getManufacturerCompanyName() != null ? po.getManufacturerCompanyName() : (po.getManufacturer() != null ? po.getManufacturer() : ""),
            po.getMaterialGrade() != null ? po.getMaterialGrade() : (po.getGrade() != null ? po.getGrade() : ""),
            po.getQuantity() != null ? po.getQuantity() : "",
            po.getDimension() != null ? po.getDimension() : (po.getRequiredDimension() != null ? po.getRequiredDimension() : ""),
            po.getStatus() != null ? po.getStatus() : ""
        );
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 250, 250);
        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        }
    }

    @Transactional
    public byte[] generateQrCode(String poNumber) throws Exception {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findByPoNumber(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("PO not found: " + poNumber);
        }
        PurchaseOrder po = poOpt.get();
        if (po.getQrCodeImage() == null || po.getQrCodeImage().length == 0) {
            byte[] qrBytes = generateQrCodeBytes(po);
            po.setQrCodeImage(qrBytes);
            purchaseOrderRepository.save(po);
            return qrBytes;
        }
        return po.getQrCodeImage();
    }

    public byte[] generatePurchaseOrderPdf(String poNumber) throws Exception {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findByPoNumber(poNumber);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("PO not found: " + poNumber);
        }
        PurchaseOrder po = poOpt.get();
        
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        
        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, java.awt.Color.DARK_GRAY);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, java.awt.Color.BLACK);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, java.awt.Color.BLACK);
        Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, java.awt.Color.BLACK);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Font.NORMAL, java.awt.Color.GRAY);
        
        // Title Table / Company Logo placeholder
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 2});
        
        PdfPCell titleCell = new PdfPCell(new Paragraph("PURCHASE ORDER", titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(titleCell);
        
        PdfPCell logoCell = new PdfPCell(new Paragraph("DIMETIME SCM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, new java.awt.Color(34, 197, 94))));
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(logoCell);
        
        document.add(headerTable);
        document.add(new Paragraph("\n"));
        
        // PO Info Table
        PdfPTable poInfoTable = new PdfPTable(2);
        poInfoTable.setWidthPercentage(100);
        poInfoTable.addCell(createCell("PO Number: " + po.getPoNumber(), boldBodyFont));
        poInfoTable.addCell(createCell("Date: " + po.getCreatedAt().toLocalDate().toString(), bodyFont));
        poInfoTable.addCell(createCell("RFQ Number: " + (po.getRfqNumber() != null ? po.getRfqNumber() : "N/A"), bodyFont));
        poInfoTable.addCell(createCell("Quotation Ref: " + (po.getQuotationReferenceNumber() != null ? po.getQuotationReferenceNumber() : "N/A"), bodyFont));
        document.add(poInfoTable);
        
        document.add(new Paragraph("\n"));
        
        // Supplier & Manufacturer Addresses Table
        PdfPTable addressTable = new PdfPTable(2);
        addressTable.setWidthPercentage(100);
        addressTable.setSpacingBefore(10f);
        
        // Supplier Header
        PdfPCell supHeader = new PdfPCell(new Paragraph("Supplier Details", sectionFont));
        supHeader.setBackgroundColor(new java.awt.Color(240, 240, 240));
        supHeader.setPadding(5);
        addressTable.addCell(supHeader);
        
        // Manufacturer Header
        PdfPCell manHeader = new PdfPCell(new Paragraph("Manufacturer Details", sectionFont));
        manHeader.setBackgroundColor(new java.awt.Color(240, 240, 240));
        manHeader.setPadding(5);
        addressTable.addCell(manHeader);
        
        // Supplier Info
        String supInfo = String.format("%s\nAddress: %s\nGST: %s\nUsername: %s",
            po.getSupplierCompanyName() != null ? po.getSupplierCompanyName() : po.getSupplier(),
            po.getSupplierAddress() != null ? po.getSupplierAddress() : "N/A",
            po.getSupplierGstNumber() != null ? po.getSupplierGstNumber() : "N/A",
            po.getSupplierUsername() != null ? po.getSupplierUsername() : po.getSupplier()
        );
        PdfPCell supCell = new PdfPCell(new Paragraph(supInfo, bodyFont));
        supCell.setPadding(8);
        addressTable.addCell(supCell);
        
        // Manufacturer Info
        String manInfo = String.format("%s\nAddress: %s\nGST: %s\nUsername: %s",
            po.getManufacturerCompanyName() != null ? po.getManufacturerCompanyName() : po.getManufacturer(),
            po.getManufacturerAddress() != null ? po.getManufacturerAddress() : "N/A",
            po.getManufacturerGstNumber() != null ? po.getManufacturerGstNumber() : "N/A",
            po.getManufacturerUsername() != null ? po.getManufacturerUsername() : po.getManufacturer()
        );
        PdfPCell manCell = new PdfPCell(new Paragraph(manInfo, bodyFont));
        manCell.setPadding(8);
        addressTable.addCell(manCell);
        
        document.add(addressTable);
        document.add(new Paragraph("\n"));
        
        // Material & Specs Table
        Paragraph matHeading = new Paragraph("Material & Specifications", sectionFont);
        matHeading.setSpacingAfter(5f);
        document.add(matHeading);
        
        PdfPTable specTable = new PdfPTable(5);
        specTable.setWidthPercentage(100);
        specTable.setWidths(new float[]{2, 1, 1, 1, 1});
        
        specTable.addCell(createHeaderCell("Material Description", boldBodyFont));
        specTable.addCell(createHeaderCell("Grade", boldBodyFont));
        specTable.addCell(createHeaderCell("Required Dimension", boldBodyFont));
        specTable.addCell(createHeaderCell("Quantity", boldBodyFont));
        specTable.addCell(createHeaderCell("MTC Req", boldBodyFont));
        
        String desc = po.getMaterialDescription() != null ? po.getMaterialDescription() : (po.getMaterialName() != null ? po.getMaterialName() : po.getMaterial());
        specTable.addCell(createCell(desc, bodyFont));
        specTable.addCell(createCell(po.getMaterialGrade() != null ? po.getMaterialGrade() : po.getGrade(), bodyFont));
        specTable.addCell(createCell(po.getRequiredDimension() != null ? po.getRequiredDimension() : po.getDimension(), bodyFont));
        specTable.addCell(createCell(po.getQuantity(), bodyFont));
        specTable.addCell(createCell((po.getMtcRequired() != null && po.getMtcRequired()) ? "YES" : "NO", bodyFont));
        document.add(specTable);
        
        document.add(new Paragraph("\n"));
        
        // Commercial & Delivery Summary Table
        PdfPTable commTable = new PdfPTable(2);
        commTable.setWidthPercentage(100);
        commTable.setSpacingBefore(10f);
        
        // Commercial Section
        PdfPCell commHeader = new PdfPCell(new Paragraph("Commercial Summary", sectionFont));
        commHeader.setBackgroundColor(new java.awt.Color(240, 240, 240));
        commHeader.setPadding(5);
        commTable.addCell(commHeader);
        
        // Delivery Section
        PdfPCell delivHeader = new PdfPCell(new Paragraph("Delivery Information", sectionFont));
        delivHeader.setBackgroundColor(new java.awt.Color(240, 240, 240));
        delivHeader.setPadding(5);
        commTable.addCell(delivHeader);
        
        Double uPrice = po.getUnitPrice() != null ? po.getUnitPrice() : 0.0;
        Double tPrice = po.getTotalPrice() != null ? po.getTotalPrice() : 0.0;
        Double gstAmt = tPrice * (po.getGstTax() != null ? po.getGstTax() : 18.0) / 100.0;
        Double netAmt = tPrice + gstAmt;
        
        String commInfo = String.format("Unit Price: %s %.2f\nTotal Price: %s %.2f\nGST Rate: %.1f%%\nGST Amount: %s %.2f\nNet Amount: %s %.2f",
            po.getCurrency(), uPrice,
            po.getCurrency(), tPrice,
            po.getGstTax() != null ? po.getGstTax() : 18.0,
            po.getCurrency(), gstAmt,
            po.getCurrency(), netAmt
        );
        PdfPCell commCell = new PdfPCell(new Paragraph(commInfo, bodyFont));
        commCell.setPadding(8);
        commTable.addCell(commCell);
        
        String delivInfo = String.format("Delivery Date: %s\nLocation: %s\nTerms: %s",
            po.getRequiredDeliveryDate() != null ? po.getRequiredDeliveryDate().toString() : "N/A",
            po.getDeliveryLocation() != null ? po.getDeliveryLocation() : "N/A",
            po.getDeliveryTerms() != null ? po.getDeliveryTerms() : "As per contract"
        );
        PdfPCell delivCell = new PdfPCell(new Paragraph(delivInfo, bodyFont));
        delivCell.setPadding(8);
        commTable.addCell(delivCell);
        
        document.add(commTable);
        document.add(new Paragraph("\n"));
        
        // QR Code & Signature Section
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{1, 1});
        
        // Generate QR code bytes
        byte[] qrBytes = generateQrCode(po.getPoNumber());
        Image qrImg = Image.getInstance(qrBytes);
        qrImg.scaleAbsolute(100f, 100f);
        
        PdfPCell qrCell = new PdfPCell(qrImg);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        footerTable.addCell(qrCell);
        
        // Signature Block
        Paragraph sigBlock = new Paragraph("\n\n\n_______________________\nAuthorized Signature\n" + (po.getSupplierCompanyName() != null ? po.getSupplierCompanyName() : po.getSupplier()), boldBodyFont);
        PdfPCell sigCell = new PdfPCell(sigBlock);
        sigCell.setBorder(Rectangle.NO_BORDER);
        sigCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sigCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        footerTable.addCell(sigCell);
        
        document.add(footerTable);
        
        // Footer notice
        document.add(new Paragraph("\n\n"));
        Paragraph footerNotice = new Paragraph("This is a system generated Purchase Order released through DimeTime SCM. Offline QR verification supported.", footerFont);
        footerNotice.setAlignment(Element.ALIGN_CENTER);
        document.add(footerNotice);
        
        document.close();
        return out.toByteArray();
    }
    
    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(6);
        return cell;
    }
    
    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    @Transactional
    public void deletePurchaseOrder(Long id, String operator) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isPresent()) {
            String poNum = poOpt.get().getPoNumber();
            purchaseOrderRepository.deleteById(id);
            auditLogService.logActivity("Deleted Purchase Order: " + poNum, operator);
        }
    }

    @Transactional
    public PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrder poDetails, String operator) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty()) {
            throw new IllegalArgumentException("Purchase Order not found with id: " + id);
        }
        PurchaseOrder po = poOpt.get();
        po.setMaterial(poDetails.getMaterial());
        po.setGrade(poDetails.getGrade());
        po.setDimension(poDetails.getDimension());
        po.setQuantity(poDetails.getQuantity());
        po.setStatus(poDetails.getStatus());
        po.setSupplier(poDetails.getSupplier());
        po.setManufacturer(poDetails.getManufacturer());
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditLogService.logActivity("Updated Purchase Order: " + po.getPoNumber(), operator);
        return saved;
    }
}
