package com.example.manifest.service;

import com.example.manifest.entity.Customer;
import com.example.manifest.entity.Manifest;
import com.example.manifest.entity.ManifestItem;
import com.example.manifest.repository.CustomerRepository;
import com.example.manifest.repository.ManifestRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 货单 PDF 生成服务
 * 生成标准送货单，用于随货打印放置
 */
@Service
public class ManifestPdfService {

    private static final Logger log = LoggerFactory.getLogger(ManifestPdfService.class);

    // 颜色定义
    private static final Color PRIMARY_COLOR = new Color(0x1A, 0x56, 0x7C); // 标题/强调文字用
    private static final Color LIGHT_GRAY   = new Color(0xDD, 0xDD, 0xDD); // 边框线

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ManifestRepository manifestRepository;
    private final CustomerRepository customerRepository;

    // 静态字体缓存（避免每次创建 PDF 都重新加载字体文件）
    private static final BaseFont CJK_BASE_FONT;
    static {
        BaseFont bf = null;
        String[] paths = {
            "/System/Library/Fonts/Hiragino Sans GB.ttc,0",
            "/System/Library/Fonts/Supplemental/Songti.ttc,0",
        };
        for (String p : paths) {
            try {
                bf = BaseFont.createFont(p, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                break;
            } catch (Exception ignored) {}
        }
        if (bf == null) {
            try {
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            } catch (Exception ignored) {}
        }
        CJK_BASE_FONT = bf;
    }

    public ManifestPdfService(ManifestRepository manifestRepository, CustomerRepository customerRepository) {
        this.manifestRepository = manifestRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * 为指定货单生成 PDF 字节数组
     * 直接从数据库查询真实数据，避免 Hibernate 代理问题
     */
    @Transactional(readOnly = true)
    public byte[] generatePdf(Integer manifestId) throws IOException {
        // 直接查数据库获取完整数据
        Manifest manifest = manifestRepository.findByIdWithCustomerAndItems(manifestId).orElse(null);
        if (manifest == null) {
            throw new IllegalArgumentException("货单不存在: ID=" + manifestId);
        }

        // 直接查客户数据（绕过 Hibernate 代理字段访问问题）
        Customer customer = null;
        if (manifest.getCustomerId() != null) {
            customer = customerRepository.findById(manifest.getCustomerId()).orElse(null);
        }
        if (customer == null && manifest.getCustomer() != null) {
            // fallback：尝试从已加载的 manifest.customer 中取值
            customer = manifest.getCustomer();
            log.info("使用 manifest.customer: name=[{}], phone=[{}]",
                    customer.getCustomerName(), customer.getPhone());
        }

        log.info("PDF数据加载完成: manifestId={}, customerId={}, itemsCount={}",
                manifest.getId(),
                manifest.getCustomerId(),
                manifest.getItems() != null ? manifest.getItems().size() : 0);

        Document doc = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        addTitle(doc, manifest);
        addInfoSection(doc, manifest, customer);
        addPaymentShippingSection(doc, manifest);
        addItemsTable(doc, manifest);
        addTotalAndNotes(doc, manifest);
        addSignatureSection(doc);

        doc.close();
        return out.toByteArray();
    }

    // ── 标题 ────────────────────────────────────────────────
    private void addTitle(Document doc, Manifest manifest) throws DocumentException {
        Paragraph title = new Paragraph("送  货  单", createFont(24, true, PRIMARY_COLOR));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        String dateStr = manifest.getOrderDate() != null
                ? manifest.getOrderDate().format(DATE_FMT)
                : (manifest.getCreatedAt() != null ? manifest.getCreatedAt().toLocalDate().format(DATE_FMT) : "");

        // 货单编号：日期 + 客户ID + 货单ID
        String manifestNo = dateStr + "-C" + manifest.getCustomerId() + "-M" + manifest.getId();

        Paragraph subPara = new Paragraph("No. " + manifestNo, createFont(10, false, Color.GRAY));
        subPara.setAlignment(Element.ALIGN_CENTER);
        subPara.setSpacingAfter(10);
        doc.add(subPara);
    }

    // ── 信息区块：客户 + 单号 + 日期 ──────────────────────────
    private void addInfoSection(Document doc, Manifest manifest, Customer customer) throws DocumentException {
        String custName = customer != null ? str(customer.getCustomerName()) : "—";
        String custPhone = customer != null ? str(customer.getPhone()) : "";
        String custAddr  = customer != null ? str(customer.getAddress()) : "";

        // 两行三列表格
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 2f, 2.5f});
        table.setSpacingAfter(10);

        // Row 1: [客户名][单号][日期]
        // Row 2: [电话+地址][空][空]

        // Row 1, Col 1: 客户名
        PdfPCell r1c1 = new PdfPCell();
        r1c1.setPadding(8);
        r1c1.setBorder(Rectangle.BOTTOM);
        r1c1.setBorderColor(LIGHT_GRAY);
        r1c1.addElement(new Paragraph("客户", new Font(CJK_BASE_FONT, 9, Font.NORMAL, Color.GRAY)));
        r1c1.addElement(new Paragraph(custName, new Font(CJK_BASE_FONT, 13, Font.BOLD, Color.BLACK)));
        table.addCell(r1c1);

        // Row 1, Col 2: 客户ID
        PdfPCell r1c2 = new PdfPCell();
        r1c2.setPadding(8);
        r1c2.setBorder(Rectangle.BOTTOM);
        r1c2.setBorderColor(LIGHT_GRAY);
        r1c2.addElement(new Paragraph("客户ID", new Font(CJK_BASE_FONT, 9, Font.NORMAL, Color.GRAY)));
        r1c2.addElement(new Paragraph("C" + manifest.getCustomerId(), new Font(CJK_BASE_FONT, 11, Font.BOLD, Color.BLACK)));
        table.addCell(r1c2);

        // Row 1, Col 3: 日期
        String dateStr = manifest.getOrderDate() != null
                ? manifest.getOrderDate().format(DATE_FMT)
                : (manifest.getCreatedAt() != null ? manifest.getCreatedAt().toLocalDate().format(DATE_FMT) : "—");
        PdfPCell r1c3 = new PdfPCell();
        r1c3.setPadding(8);
        r1c3.setBorder(Rectangle.BOTTOM);
        r1c3.setBorderColor(LIGHT_GRAY);
        r1c3.addElement(new Paragraph("日期", new Font(CJK_BASE_FONT, 9, Font.NORMAL, Color.GRAY)));
        r1c3.addElement(new Paragraph(dateStr, new Font(CJK_BASE_FONT, 10, Font.BOLD, Color.BLACK)));
        table.addCell(r1c3);

        // Row 2, Col 1: 电话 + 地址
        PdfPCell r2c1 = new PdfPCell();
        r2c1.setPadding(8);
        r2c1.setBorder(Rectangle.NO_BORDER);
        if (!isBlank(custPhone)) {
            r2c1.addElement(new Paragraph("电话：" + custPhone, new Font(CJK_BASE_FONT, 9, Font.NORMAL, Color.DARK_GRAY)));
        }
        if (!isBlank(custAddr)) {
            r2c1.addElement(new Paragraph("地址：" + custAddr, new Font(CJK_BASE_FONT, 9, Font.NORMAL, Color.DARK_GRAY)));
        }
        table.addCell(r2c1);

        // Row 2, Col 2: 空
        PdfPCell r2c2 = new PdfPCell();
        r2c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(r2c2);

        // Row 2, Col 3: 空
        PdfPCell r2c3 = new PdfPCell();
        r2c3.setBorder(Rectangle.NO_BORDER);
        table.addCell(r2c3);

        doc.add(table);
    }

    // ── 付款 & 出货方式 ──────────────────────────────────────
    private void addPaymentShippingSection(Document doc, Manifest manifest) throws DocumentException {
        String payment = str(manifest.getPaymentMethod());
        String shipping = str(manifest.getShippingMethod());

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});
        table.setSpacingAfter(8);

        // 付款方式
        PdfPCell payCell = new PdfPCell();
        payCell.setPadding(10);
        payCell.setBorderColor(LIGHT_GRAY);
        payCell.addElement(new Paragraph("💳 付款方式", new Font(CJK_BASE_FONT, 9, Font.NORMAL, new Color(0x59, 0x30, 0x7B))));
        payCell.addElement(new Paragraph(isBlank(payment) ? "现金" : payment, new Font(CJK_BASE_FONT, 13, Font.BOLD, new Color(0x59, 0x30, 0x7B))));
        payCell.setBackgroundColor(new Color(0xF5, 0xF0, 0xFF));
        table.addCell(payCell);

        // 出货方式
        PdfPCell shipCell = new PdfPCell();
        shipCell.setPadding(10);
        shipCell.setBorderColor(LIGHT_GRAY);
        shipCell.addElement(new Paragraph("🚚 出货方式", new Font(CJK_BASE_FONT, 9, Font.NORMAL, new Color(0x15, 0x8B, 0x31))));
        shipCell.addElement(new Paragraph(isBlank(shipping) ? "物流" : shipping, new Font(CJK_BASE_FONT, 13, Font.BOLD, new Color(0x15, 0x8B, 0x31))));
        shipCell.setBackgroundColor(new Color(0xF0, 0xFF, 0xF0));
        table.addCell(shipCell);

        doc.add(table);
    }

    // ── 产品明细表格 ─────────────────────────────────────────
    private void addItemsTable(Document doc, Manifest manifest) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.5f, 1.5f, 0.8f, 1.5f, 1.5f});
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);

        // ── 表头 ─────────────────────────────────────────
        String[] headers = {"产品名称", "产品号", "件数", "单价(元)", "小计(元)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, createFont(10, true, Color.BLACK)));
            cell.setBackgroundColor(Color.WHITE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(Color.BLACK);
            table.addCell(cell);
        }

        // ── 数据行 ───────────────────────────────────────
        List<ManifestItem> items = manifest.getItems();
        if (items != null && !items.isEmpty()) {
            for (ManifestItem item : items) {
                String name = str(item.getProductName());
                String submodel = str(item.getSubmodelName());
                String displayName = !isBlank(submodel) ? name + " / " + submodel : name;
                if (isBlank(displayName)) displayName = "—";

                addCell(table, displayName, Element.ALIGN_LEFT, Color.WHITE);
                addCell(table, str(item.getProductNo()), Element.ALIGN_CENTER, Color.WHITE);
                addCell(table, String.valueOf(item.getQuantity() != null ? item.getQuantity() : 0), Element.ALIGN_CENTER, Color.WHITE);
                addCell(table, yuan(item.getUnitPrice()), Element.ALIGN_RIGHT, Color.WHITE);
                addCell(table, yuan(item.getSubtotal()), Element.ALIGN_RIGHT, Color.WHITE);
            }
        } else {
            // 无明细
            PdfPCell empty = new PdfPCell(new Phrase("（暂无产品明细）", createFont(9, false, Color.GRAY)));
            empty.setColspan(5);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(16);
            empty.setBackgroundColor(Color.WHITE);
            table.addCell(empty);
        }

        // ── 合计行 ───────────────────────────────────────
        PdfPCell totalLabel = new PdfPCell(new Phrase("货单总价", createFont(11, true, Color.BLACK)));
        totalLabel.setBackgroundColor(Color.WHITE);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setColspan(4);
        totalLabel.setPadding(10);
        totalLabel.setBorder(Rectangle.TOP);
        totalLabel.setBorderColor(Color.BLACK);
        table.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase("¥" + yuan(manifest.getTotalPrice()), createFont(14, true, PRIMARY_COLOR)));
        totalValue.setBackgroundColor(Color.WHITE);
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValue.setPadding(10);
        totalValue.setBorder(Rectangle.TOP);
        totalValue.setBorderColor(Color.BLACK);
        table.addCell(totalValue);

        doc.add(table);
    }

    // ── 备注 ────────────────────────────────────────────────
    private void addTotalAndNotes(Document doc, Manifest manifest) throws DocumentException {
        String notes = manifest.getNotes();
        if (isBlank(notes)) {
            return;
        }

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(10);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(LIGHT_GRAY);
        cell.addElement(p("备注", 9, true, Color.GRAY));
        cell.addElement(p(notes, 10, false, Color.BLACK));
        table.addCell(cell);

        doc.add(table);
    }

    // ── 签收栏 ──────────────────────────────────────────────
    private void addSignatureSection(Document doc) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});
        table.setSpacingBefore(24);

        for (String label : new String[]{"发货人（签字）", "收货人（签字）"}) {
            PdfPCell cell = new PdfPCell();
            cell.setFixedHeight(64);
            cell.setBorderColor(LIGHT_GRAY);
            cell.setPadding(8);
            Paragraph para = p(label, 9, false, Color.GRAY);
            para.setSpacingBefore(36);
            cell.addElement(para);
            table.addCell(cell);
        }

        doc.add(table);
    }

    // ══════════════════════════════════════════════════════════
    //  工具方法
    // ══════════════════════════════════════════════════════════

    private Font createFont(int size, boolean bold, Color color) {
        int style = bold ? Font.BOLD : Font.NORMAL;
        if (CJK_BASE_FONT != null) {
            return new Font(CJK_BASE_FONT, size, style, color);
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private String yuan(Integer fen) {
        if (fen == null) return "0.00";
        return String.format("%,.2f", fen / 100.0);
    }

    /** 安全字符串，空/ null /全空白 时返回空字符串，并去掉首尾空白 */
    private String str(String s) {
        if (s == null || "null".equals(s)) return "";
        s = s.trim();
        return s.isEmpty() ? "" : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank() || "null".equals(s);
    }

    /** 快捷 Paragraph 构造 */
    private Paragraph p(String text, int size, boolean bold, Color color) {
        return new Paragraph(text, createFont(size, bold, color));
    }

    private void addCell(PdfPTable table, String text, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, createFont(9, false, Color.BLACK)));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bg);
        cell.setPadding(9);
        cell.setBorderColor(LIGHT_GRAY);
        table.addCell(cell);
    }
}
