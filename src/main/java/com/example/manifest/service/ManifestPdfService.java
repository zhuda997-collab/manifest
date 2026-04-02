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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 货单 PDF 生成服务
 * 参照标准订货单格式设计
 */
@Service
public class ManifestPdfService {

    private static final Logger log = LoggerFactory.getLogger(ManifestPdfService.class);

    // 颜色定义
    private static final Color BLACK       = Color.BLACK;
    private static final Color WHITE       = Color.WHITE;
    private static final Color GRAY_TEXT   = new Color(0x66, 0x66, 0x66);
    private static final Color TABLE_HEAD  = new Color(0xF0, 0xF0, 0xF0);
    private static final Color LIGHT_GRAY  = new Color(0xDD, 0xDD, 0xDD);

    // 公司名称（写死在模板里）
    private static final String COMPANY_NAME = "山西侯马农友种业订货单";

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ManifestRepository manifestRepository;
    private final CustomerRepository customerRepository;

    // 静态字体缓存
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

    @Transactional(readOnly = true)
    public byte[] generatePdf(Integer manifestId) throws IOException {
        Manifest manifest = manifestRepository.findByIdWithCustomerAndItems(manifestId).orElse(null);
        if (manifest == null) {
            throw new IllegalArgumentException("货单不存在: ID=" + manifestId);
        }

        Customer customer = null;
        if (manifest.getCustomerId() != null) {
            customer = customerRepository.findById(manifest.getCustomerId()).orElse(null);
        }
        if (customer == null && manifest.getCustomer() != null) {
            customer = manifest.getCustomer();
        }

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        // 1. 公司标题 + 订单信息（右上角）
        addHeader(doc, manifest);
        // 2. 客户及收货信息
        addCustomerInfo(doc, manifest, customer);
        // 3. 商品明细表格（7列）
        addItemsTable(doc, manifest);
        // 4. 合计（数量、金额、运费、优惠、未付）
        addSummarySection(doc, manifest);
        // 5. 底部（状态 + 公司信息）
        addFooterSection(doc, manifest);

        doc.close();
        return out.toByteArray();
    }

    // ══════════════════════════════════════════════════════
    //  1. 标题 + 订单信息
    // ══════════════════════════════════════════════════════
    private void addHeader(Document doc, Manifest manifest) throws DocumentException {
        // 标题行：公司名称（大字居中）
        Paragraph title = new Paragraph(COMPANY_NAME, new Font(CJK_BASE_FONT, 18, Font.BOLD, BLACK));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(2);
        doc.add(title);

        // 右上角三行列：订单号 | 下单时间 | 打印时间
        String orderNo = manifest.getOrderNumber() != null ? manifest.getOrderNumber()
                           : ("DD-" + (manifest.getOrderDate() != null ? manifest.getOrderDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "00000000")
                           + String.format("%05d", manifest.getId()));
        String orderTime = manifest.getCreatedAt() != null ? manifest.getCreatedAt().format(DATE_TIME_FMT) : "—";
        String printTime = LocalDateTime.now().format(DATE_TIME_FMT);

        PdfPTable orderTable = new PdfPTable(3);
        orderTable.setWidthPercentage(100);
        orderTable.setWidths(new float[]{2f, 2f, 2f});
        orderTable.setSpacingAfter(10);

        addHeaderCell(orderTable, "订单号", orderNo);
        addHeaderCell(orderTable, "下单时间", orderTime);
        addHeaderCell(orderTable, "打印时间", printTime);

        doc.add(orderTable);
    }

    private void addHeaderCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.addElement(new Paragraph(label, new Font(CJK_BASE_FONT, 8, Font.NORMAL, GRAY_TEXT)));
        cell.addElement(new Paragraph(value, new Font(CJK_BASE_FONT, 9, Font.NORMAL, BLACK)));
        table.addCell(cell);
    }

    // ══════════════════════════════════════════════════════
    //  2. 客户及收货信息
    // ══════════════════════════════════════════════════════
    private void addCustomerInfo(Document doc, Manifest manifest, Customer customer) throws DocumentException {
        String custName      = customer != null ? str(customer.getCustomerName()) : "—";
        String contact       = customer != null ? str(customer.getCustomerName()) : "—";  // 联系人同客户名
        String custPhone     = customer != null ? str(customer.getPhone()) : "";
        String receiverName  = customer != null ? str(customer.getReceiverName()) : "";
        String receiverPhone = customer != null ? str(customer.getReceiverPhone()) : "";
        String receiverAddr  = customer != null ? str(customer.getAddress()) : "";

        // 如果收货人为空，默认同客户名
        if (isBlank(receiverName)) receiverName = custName;
        if (isBlank(receiverPhone)) receiverPhone = custPhone;

        // 两行三列表格
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 1.5f, 1f, 1f, 1f, 1.5f});
        table.setSpacingAfter(8);

        // Row 1: 客户名称 | 联系人 | 联系电话 | 收货人 | 收货电话 | [空]
        addInfoCell(table, "客户名称", custName, true);
        addInfoCell(table, "联系人", contact, true);
        addInfoCell(table, "联系电话", custPhone, true);
        addInfoCell(table, "收货人", receiverName, true);
        addInfoCell(table, "收货电话", receiverPhone, true);
        addInfoCell(table, "收货地址", receiverAddr, false);  // Row1 Col6 跨行

        // Row 2: [空] [空] [空] [空] [空] 收货地址（续）
        PdfPCell empty1 = newCell("", Rectangle.NO_BORDER, 4);
        PdfPCell empty2 = newCell("", Rectangle.NO_BORDER, 4);
        PdfPCell empty3 = newCell("", Rectangle.NO_BORDER, 4);
        PdfPCell empty4 = newCell("", Rectangle.NO_BORDER, 4);
        PdfPCell empty5 = newCell("", Rectangle.NO_BORDER, 4);
        PdfPCell addrCell = newCell("", Rectangle.NO_BORDER, 4);
        table.addCell(empty1);
        table.addCell(empty2);
        table.addCell(empty3);
        table.addCell(empty4);
        table.addCell(empty5);
        table.addCell(addrCell);

        doc.add(table);
    }

    private void addInfoCell(PdfPTable table, String label, String value, boolean addBorder) {
        int border = addBorder ? Rectangle.BOTTOM : Rectangle.BOTTOM;
        PdfPCell cell = new PdfPCell();
        cell.setBorder(border);
        cell.setBorderColor(LIGHT_GRAY);
        cell.setPadding(4);
        Paragraph labelP = new Paragraph(label, new Font(CJK_BASE_FONT, 7, Font.NORMAL, GRAY_TEXT));
        labelP.setLeading(9f, 0f);
        cell.addElement(labelP);
        Paragraph valueP = new Paragraph(isBlank(value) ? "—" : value, new Font(CJK_BASE_FONT, 8, Font.NORMAL, BLACK));
        valueP.setLeading(10f, 0f);
        cell.addElement(valueP);
        table.addCell(cell);
    }

    // ══════════════════════════════════════════════════════
    //  3. 商品明细表格（7列）
    // ══════════════════════════════════════════════════════
    private void addItemsTable(Document doc, Manifest manifest) throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        // 序号 | 商品名称 | 商品规格 | 备注 | 数量 | 单价 | 小计
        table.setWidths(new float[]{0.5f, 2.5f, 1.8f, 1f, 0.7f, 0.9f, 1.1f});
        table.setSpacingBefore(4);
        table.setSpacingAfter(0);

        // 表头
        String[] headers = {"序号", "商品名称", "商品规格", "备注", "数量", "单价(元)", "小计(元)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(CJK_BASE_FONT, 9, Font.BOLD, BLACK)));
            cell.setBackgroundColor(TABLE_HEAD);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(7);
            cell.setBorder(Rectangle.BOX);
            cell.setBorderColor(LIGHT_GRAY);
            table.addCell(cell);
        }

        List<ManifestItem> items = manifest.getItems();
        int totalQty = 0;

        if (items != null && !items.isEmpty()) {
            int idx = 1;
            for (ManifestItem item : items) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                totalQty += qty;

                addTableCell(table, String.valueOf(idx++), Element.ALIGN_CENTER);
                String name = str(item.getProductName());
                String submodel = str(item.getSubmodelName());
                addTableCell(table, isBlank(name) ? "—" : name, Element.ALIGN_LEFT);
                // 商品规格
                String spec = isBlank(submodel) ? "—" : submodel;
                addTableCell(table, spec, Element.ALIGN_LEFT);
                addTableCell(table, "—", Element.ALIGN_CENTER);  // 备注列
                addTableCell(table, String.valueOf(qty), Element.ALIGN_CENTER);
                addTableCell(table, yuan(item.getUnitPrice()), Element.ALIGN_RIGHT);
                addTableCell(table, yuan(item.getSubtotal()), Element.ALIGN_RIGHT);
            }
        } else {
            PdfPCell empty = new PdfPCell(new Phrase("（暂无产品明细）", new Font(CJK_BASE_FONT, 9, Font.NORMAL, GRAY_TEXT)));
            empty.setColspan(7);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(14);
            empty.setBackgroundColor(WHITE);
            empty.setBorder(Rectangle.BOX);
            empty.setBorderColor(LIGHT_GRAY);
            table.addCell(empty);
        }

        // 页合计行
        int totalFen = manifest.getTotalPrice() != null ? manifest.getTotalPrice() : 0;
        PdfPCell pageLabel = new PdfPCell(new Phrase("本页合计", new Font(CJK_BASE_FONT, 9, Font.BOLD, BLACK)));
        pageLabel.setColspan(4);
        pageLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        pageLabel.setPadding(7);
        pageLabel.setBackgroundColor(TABLE_HEAD);
        pageLabel.setBorder(Rectangle.BOX);
        pageLabel.setBorderColor(LIGHT_GRAY);
        table.addCell(pageLabel);

        addTableCell(table, String.valueOf(totalQty), Element.ALIGN_CENTER);
        addTableCell(table, "", Element.ALIGN_CENTER);
        addTableCell(table, yuan(totalFen), Element.ALIGN_RIGHT);

        doc.add(table);
    }

    private void addTableCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(CJK_BASE_FONT, 9, Font.NORMAL, BLACK)));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(WHITE);
        cell.setPadding(7);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(LIGHT_GRAY);
        table.addCell(cell);
    }

    // ══════════════════════════════════════════════════════
    //  4. 合计区（总计 + 运费 + 优惠 + 未付金额）
    // ══════════════════════════════════════════════════════
    private void addSummarySection(Document doc, Manifest manifest) throws DocumentException {
        int totalFen   = manifest.getTotalPrice() != null ? manifest.getTotalPrice() : 0;
        int freightFen = manifest.getFreight() != null ? manifest.getFreight() : 0;
        int discFen    = manifest.getDiscount() != null ? manifest.getDiscount() : 0;
        int unpaidFen  = totalFen - discFen + freightFen; // 未付 = 总计 - 优惠 + 运费

        double totalAmt   = totalFen / 100.0;
        double freightAmt = freightFen / 100.0;
        double discAmt    = discFen / 100.0;
        double unpaidAmt  = unpaidFen / 100.0;

        String upperTotal = toChineseUpper((int) totalAmt);
        DecimalFormat df = new DecimalFormat("#,##0.00");

        // 计算总数量
        int totalQty = 0;
        if (manifest.getItems() != null) {
            for (ManifestItem item : manifest.getItems()) {
                totalQty += item.getQuantity() != null ? item.getQuantity() : 0;
            }
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 1.5f, 1.5f, 1.5f});
        table.setSpacingAfter(0);

        // 总计行（大写 + 数量 + 金额 + 运费 + 优惠）
        // 左侧：大写金额 + 右侧三列：运费 | 优惠合计
        PdfPCell leftCell = new PdfPCell();
        leftCell.setColspan(2);
        leftCell.setBorder(Rectangle.BOX);
        leftCell.setBorderColor(LIGHT_GRAY);
        leftCell.setPadding(8);
        leftCell.setBackgroundColor(TABLE_HEAD);
        leftCell.addElement(p("总计", 8, false, GRAY_TEXT));
        leftCell.addElement(p("大写: " + upperTotal + "元整", 10, true, BLACK));
        table.addCell(leftCell);

        // 运费
        PdfPCell freightCell = new PdfPCell();
        freightCell.setBorder(Rectangle.BOX);
        freightCell.setBorderColor(LIGHT_GRAY);
        freightCell.setPadding(8);
        freightCell.setBackgroundColor(TABLE_HEAD);
        freightCell.addElement(p("运费", 8, false, GRAY_TEXT));
        freightCell.addElement(p(df.format(freightAmt), 10, true, BLACK));
        table.addCell(freightCell);

        // 优惠合计
        PdfPCell discCell = new PdfPCell();
        discCell.setBorder(Rectangle.BOX);
        discCell.setBorderColor(LIGHT_GRAY);
        discCell.setPadding(8);
        discCell.setBackgroundColor(TABLE_HEAD);
        discCell.addElement(p("优惠合计", 8, false, GRAY_TEXT));
        discCell.addElement(p(df.format(discAmt), 10, true, BLACK));
        table.addCell(discCell);

        // 未付金额
        PdfPCell unpaidCell = new PdfPCell();
        unpaidCell.setBorder(Rectangle.BOX);
        unpaidCell.setBorderColor(LIGHT_GRAY);
        unpaidCell.setPadding(8);
        unpaidCell.setBackgroundColor(new Color(0xFF, 0xF0, 0xF0));
        unpaidCell.addElement(p("未付金额", 8, false, GRAY_TEXT));
        unpaidCell.addElement(p(df.format(unpaidAmt), 11, true, new Color(0xCC, 0x00, 0x00)));
        table.addCell(unpaidCell);

        doc.add(table);
    }

    // ══════════════════════════════════════════════════════
    //  5. 底部（备注 + 状态 + 公司信息）
    // ══════════════════════════════════════════════════════
    private void addFooterSection(Document doc, Manifest manifest) throws DocumentException {
        String notes = str(manifest.getNotes());
        String orderStatus = "待审核";
        String payStatus = manifest.getPaymentStatus() != null ? manifest.getPaymentStatus() : "未付";
        int totalFen = manifest.getTotalPrice() != null ? manifest.getTotalPrice() : 0;
        int discFen = manifest.getDiscount() != null ? manifest.getDiscount() : 0;
        int freightFen = manifest.getFreight() != null ? manifest.getFreight() : 0;
        double unpaidAmt = (totalFen - discFen + freightFen) / 100.0;

        // 备注行
        PdfPTable notesTable = new PdfPTable(2);
        notesTable.setWidthPercentage(100);
        notesTable.setWidths(new float[]{1, 4});
        notesTable.setSpacingBefore(0);
        notesTable.setSpacingAfter(6);

        PdfPCell noteLabelCell = new PdfPCell(new Phrase("备注", new Font(CJK_BASE_FONT, 9, Font.NORMAL, GRAY_TEXT)));
        noteLabelCell.setBorder(Rectangle.BOX);
        noteLabelCell.setBorderColor(LIGHT_GRAY);
        noteLabelCell.setPadding(8);
        noteLabelCell.setBackgroundColor(TABLE_HEAD);
        notesTable.addCell(noteLabelCell);

        PdfPCell noteValCell = new PdfPCell(new Phrase(isBlank(notes) ? "—" : notes, new Font(CJK_BASE_FONT, 9, Font.NORMAL, BLACK)));
        noteValCell.setBorder(Rectangle.BOX);
        noteValCell.setBorderColor(LIGHT_GRAY);
        noteValCell.setPadding(8);
        notesTable.addCell(noteValCell);

        doc.add(notesTable);

        // 状态栏（操作员 + 订单状态 + 付款状态 + 未付）
        PdfPTable statusTable = new PdfPTable(4);
        statusTable.setWidthPercentage(100);
        statusTable.setWidths(new float[]{1.5f, 1.5f, 1.5f, 2f});
        statusTable.setSpacingAfter(10);

        // 操作员
        PdfPCell opCell = new PdfPCell();
        opCell.setBorder(Rectangle.BOX);
        opCell.setBorderColor(LIGHT_GRAY);
        opCell.setPadding(8);
        opCell.addElement(p("操作员", 8, false, GRAY_TEXT));
        opCell.addElement(p("张艳茹", 9, true, BLACK));
        statusTable.addCell(opCell);

        // 下单人
        PdfPCell ordererCell = new PdfPCell();
        ordererCell.setBorder(Rectangle.BOX);
        ordererCell.setBorderColor(LIGHT_GRAY);
        ordererCell.setPadding(8);
        ordererCell.addElement(p("下单人", 8, false, GRAY_TEXT));
        ordererCell.addElement(p("张艳茹", 9, true, BLACK));
        statusTable.addCell(ordererCell);

        // 订单状态
        PdfPCell ordStatusCell = new PdfPCell();
        ordStatusCell.setBorder(Rectangle.BOX);
        ordStatusCell.setBorderColor(LIGHT_GRAY);
        ordStatusCell.setPadding(8);
        ordStatusCell.addElement(p("订单状态", 8, false, GRAY_TEXT));
        ordStatusCell.addElement(p(orderStatus, 9, true, BLACK));
        statusTable.addCell(ordStatusCell);

        // 付款状态 + 未付金额
        PdfPCell payStatusCell = new PdfPCell();
        payStatusCell.setBorder(Rectangle.BOX);
        payStatusCell.setBorderColor(LIGHT_GRAY);
        payStatusCell.setPadding(8);
        payStatusCell.addElement(p("付款状态", 8, false, GRAY_TEXT));
        payStatusCell.addElement(p(payStatus, 9, true, BLACK));
        statusTable.addCell(payStatusCell);

        doc.add(statusTable);

        // 公司信息
        PdfPTable companyTable = new PdfPTable(1);
        companyTable.setWidthPercentage(100);
        companyTable.setSpacingAfter(0);

        PdfPCell coCell = new PdfPCell();
        coCell.setBorder(Rectangle.NO_BORDER);
        coCell.setPadding(8);
        coCell.setBackgroundColor(TABLE_HEAD);
        coCell.addElement(p("公司名称：" + COMPANY_NAME.replace("订货单", ""), 8, false, GRAY_TEXT));
        coCell.addElement(p("公司电话：0350-8505555", 8, false, GRAY_TEXT));
        coCell.addElement(p("公司地址：山西侯马东站旁边", 8, false, GRAY_TEXT));
        companyTable.addCell(coCell);

        doc.add(companyTable);
    }

    // ══════════════════════════════════════════════════════
    //  工具方法
    // ══════════════════════════════════════════════════════

    private Font createFont(int size, boolean bold, Color color) {
        int style = bold ? Font.BOLD : Font.NORMAL;
        if (CJK_BASE_FONT != null) {
            return new Font(CJK_BASE_FONT, size, style, color);
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private Paragraph p(String text, int size, boolean bold, Color color) {
        return new Paragraph(text, createFont(size, bold, color));
    }

    private String yuan(Integer fen) {
        if (fen == null) return "0.00";
        return String.format("%,.2f", fen / 100.0);
    }

    private String str(String s) {
        if (s == null || "null".equals(s)) return "";
        s = s.trim();
        return s.isEmpty() ? "" : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank() || "null".equals(s);
    }

    private PdfPCell newCell(String text, int border, int padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(CJK_BASE_FONT, 9, Font.NORMAL, BLACK)));
        cell.setBorder(border);
        cell.setPadding(padding);
        return cell;
    }

    /**
     * 将整数金额转换为中文大写
     * 例如：1900 -> 壹仟玖佰
     */
    private String toChineseUpper(int amount) {
        String[] digits = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] units = {"", "拾", "佰", "仟", "万"};

        if (amount == 0) return "零";

        StringBuilder sb = new StringBuilder();
        int unitIndex = 0;

        while (amount > 0) {
            int digit = amount % 10;
            if (digit > 0) {
                if (unitIndex > 0) {
                    sb.insert(0, units[Math.min(unitIndex, units.length - 1)]);
                }
                sb.insert(0, digits[digit]);
            } else if (!sb.toString().endsWith("零") && amount >= 10) {
                sb.insert(0, "零");
            }
            amount /= 10;
            unitIndex++;
        }

        String result = sb.toString();
        // 去掉末尾的零（如"壹仟玖佰零" -> "壹仟玖佰"）
        while (result.endsWith("零")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
