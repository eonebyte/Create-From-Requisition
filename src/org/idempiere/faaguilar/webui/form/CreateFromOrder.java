/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.idempiere.faaguilar.webui.form;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Vector;
import java.util.logging.Level;

import org.compiere.apps.IStatusBar;
import org.compiere.grid.CreateFrom;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MBPartner;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;

/**
 * 
 * @author Fabian Aguilar faaguilar@gmail.com
 * @author Ahmad Fauzi Ridwan @eonebyte (Adapted for iDempiere 13 / Java 17)
 */
public abstract class CreateFromOrder extends CreateFrom {

    public CreateFromOrder(GridTab mTab) {
        super(mTab);
        if (log.isLoggable(Level.INFO))
            log.info(mTab.toString());
    }

    public boolean dynInit() throws Exception {
        log.config("");
        setTitle(Msg.getElement(Env.getCtx(), "C_Order_ID", false)
                + " .. " + Msg.translate(Env.getCtx(), "CreateFrom"));
        return true;
    }

    protected Vector<Vector<Object>> getRequisitionData(Object Requisition, Object Org, Object User,
            Object CostCenter) {
        Vector<Vector<Object>> data = new Vector<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.M_Requisition_ID, r.DocumentNo, r.DateRequired, r.PriorityRule, rl.M_Product_ID,")
                .append(" p.Name AS ProductName, rl.Description, rl.Qty, rl.C_BPartner_ID, bp.Name AS BpName,")
                .append(" rl.M_RequisitionLine_ID, u.Name AS Username, o.Name AS OrgName,")
                .append(" c.C_Charge_ID, c.name AS ChargeName")
                .append(" FROM M_Requisition r")
                .append(" INNER JOIN M_RequisitionLine rl ON (r.M_Requisition_ID = rl.M_Requisition_ID)")
                .append(" INNER JOIN AD_User u ON (r.AD_User_ID = u.AD_User_ID)")
                .append(" INNER JOIN AD_Org o ON (r.AD_Org_ID = o.AD_Org_ID)")
                .append(" LEFT OUTER JOIN M_Product p ON (rl.M_Product_ID = p.M_Product_ID)")
                .append(" LEFT OUTER JOIN C_Charge c ON (rl.C_Charge_ID = c.C_Charge_ID)")
                .append(" LEFT OUTER JOIN C_BPartner bp ON (rl.C_BPartner_ID = bp.C_BPartner_ID)")
                .append(" WHERE r.DocStatus = 'CO' AND rl.C_OrderLine_ID IS NULL");

        if (Requisition != null)
            sql.append(" AND rl.M_Requisition_ID = ?");
        if (Org != null)
            sql.append(" AND r.AD_Org_ID = ?");
        if (User != null)
            sql.append(" AND r.AD_User_ID = ?");
        if (CostCenter != null)
            sql.append(" AND r.C_CostCenter_ID = ?");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            int i = 1;
            pstmt = DB.prepareStatement(sql.toString(), null);
            if (Requisition != null)
                pstmt.setInt(i++, (Integer) Requisition);
            if (Org != null)
                pstmt.setInt(i++, (Integer) Org);
            if (User != null)
                pstmt.setInt(i++, (Integer) User);
            if (CostCenter != null)
                pstmt.setInt(i++, (Integer) CostCenter);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                Vector<Object> line = new Vector<>();
                line.add(Boolean.FALSE); // 0-Selection
                line.add(rs.getString(13)); // 1-OrgName

                KeyNamePair pp = new KeyNamePair(rs.getInt(11), rs.getString(2).trim());
                line.add(pp); // 2-DocumentNo + LineID
                line.add(rs.getTimestamp(3)); // 3-DateRequired

                String bpName = rs.getString(10);
                line.add(bpName != null
                        ? new KeyNamePair(rs.getInt(9), bpName.trim())
                        : null); // 4-BPartner

                String prodName = rs.getString(6);
                line.add(prodName != null
                        ? new KeyNamePair(rs.getInt(5), prodName.trim())
                        : null); // 5-Product

                line.add(rs.getString(15)); // 6-Charge (nullable)
                line.add(rs.getBigDecimal(8)); // 7-Qty
                line.add(rs.getString(7)); // 8-Description
                line.add(rs.getString(12) != null ? rs.getString(12).trim() : ""); // 9-User
                data.add(line);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, sql.toString(), e);
        } finally {
            DB.close(rs, pstmt);
        }
        return data;
    }

    public void info(IMiniTable miniTable, IStatusBar statusBar) {
    }

    protected void configureMiniTable(IMiniTable miniTable) {
        miniTable.setColumnClass(0, Boolean.class, false);
        miniTable.setColumnClass(1, String.class, true);
        miniTable.setColumnClass(2, String.class, true);
        miniTable.setColumnClass(3, Timestamp.class, true);
        miniTable.setColumnClass(4, String.class, true);
        miniTable.setColumnClass(5, String.class, true);
        miniTable.setColumnClass(6, String.class, true);
        miniTable.setColumnClass(7, BigDecimal.class, true);
        miniTable.setColumnClass(8, String.class, true);
        miniTable.setColumnClass(9, String.class, true);
        miniTable.autoSize();
    }

    public boolean save(IMiniTable miniTable, String trxName) {
        int C_Order_ID = (Integer) getGridTab().getValue("C_Order_ID");
        MOrder order = new MOrder(Env.getCtx(), C_Order_ID, trxName);
        if (log.isLoggable(Level.CONFIG))
            log.config(order.toString());

        boolean headerUpdated = false;

        int taxIdToUse = 1000012;
        MBPartner bp = MBPartner.get(Env.getCtx(), order.getC_BPartner_ID());
        if (bp != null && bp.isPOTaxExempt()) {
            taxIdToUse = 1000002; // Non-PPN / Exempt
        }

        for (int i = 0; i < miniTable.getRowCount(); i++) {
            if ((Boolean) miniTable.getValueAt(i, 0)) {
                KeyNamePair pp = (KeyNamePair) miniTable.getValueAt(i, 2);
                int M_RequisitionLine_ID = pp.getKey();
                MRequisitionLine rLine = new MRequisitionLine(Env.getCtx(), M_RequisitionLine_ID, trxName);

                /*
                 * UPDATE HEADER ORDER
                 */
                if (!headerUpdated) {

                    int M_Requisition_ID = rLine.getM_Requisition_ID();

                    MRequisition req = new MRequisition(
                            Env.getCtx(),
                            M_Requisition_ID,
                            trxName);

                    // Department
                    order.set_ValueOfColumn(
                            "C_Department_ID",
                            req.get_Value("C_Department_ID"));

                    // Cost Center
                    order.set_ValueOfColumn(
                            "C_CostCenter_ID",
                            req.get_Value("C_CostCenter_ID"));

                    order.saveEx();

                    headerUpdated = true;
                }

                /*
                 * CREATE ORDER LINE
                 */
                MOrderLine orderLine = new MOrderLine(order);
                orderLine.setDatePromised(rLine.getDateRequired());
                if (rLine.getM_Product_ID() > 0) {
                    // orderLine.setProduct(MProduct.get(Env.getCtx(), rLine.getM_Product_ID()));
                    orderLine.setM_Product_ID(rLine.getM_Product_ID());
                    orderLine.setM_AttributeSetInstance_ID(rLine.getM_AttributeSetInstance_ID());

                    MProduct product = orderLine.getProduct();
                    Object whCatValue = product.get_Value("LCO_WithholdingCategory_ID");
                    int withholdingCatId = 0;

                    if (whCatValue != null) {
                        withholdingCatId = ((Integer) whCatValue).intValue();
                    }

                    if (withholdingCatId > 0) {
                        orderLine.set_ValueOfColumn("ADW_IsWithholding", "Y");

                        // Ambil rate dengan optimized JOIN query
                        String sqlTax = "SELECT ct.rate FROM c_tax ct "
                                + "INNER JOIN LCO_WithholdingCalc lwc ON ct.c_tax_id = lwc.c_tax_id "
                                + "INNER JOIN LCO_WithholdingRule lwr ON lwc.lco_withholdingcalc_id = lwr.lco_withholdingcalc_id "
                                + "INNER JOIN M_Product mp ON lwr.lco_withholdingcategory_id = mp.lco_withholdingcategory_id "
                                + "WHERE mp.M_Product_ID = ?";

                        // Mengambil nilai BigDecimal secara aman dari DB util iDempiere
                        BigDecimal rate = DB.getSQLValueBD(trxName, sqlTax, product.getM_Product_ID());
                        if (rate != null) {
                            orderLine.set_ValueOfColumn("ADW_WithholdingRate", rate);
                        } else {
                            orderLine.set_ValueOfColumn("ADW_WithholdingRate", BigDecimal.ZERO);
                        }
                    } else {
                        // Jika kriteria withholding tidak terpenuhi, set default kosong/nol
                        orderLine.set_ValueOfColumn("ADW_IsWithholding", "N");
                        orderLine.set_ValueOfColumn("ADW_WithholdingRate", BigDecimal.ZERO);
                    }

                } else {
                    orderLine.setC_Charge_ID(rLine.getC_Charge_ID());
                }
                orderLine.setPriceEntered(rLine.getPriceActual());
                orderLine.setPriceActual(rLine.getPriceActual());
                orderLine.setAD_Org_ID(rLine.getAD_Org_ID());
                orderLine.setQty(rLine.getQty());
                orderLine.setC_Tax_ID(taxIdToUse); // PPN 12 % DPP Nilai Lain / Tax Exempt
                orderLine.saveEx();

                rLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
                rLine.saveEx();
            }
        }
        return true;
    }

    protected Vector<String> getOISColumnNames() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
        columnNames.add(Msg.getElement(Env.getCtx(), "AD_Org_ID"));
        columnNames.add(Msg.translate(Env.getCtx(), "Documentno"));
        columnNames.add(Msg.translate(Env.getCtx(), "DateRequired"));
        columnNames.add(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
        columnNames.add(Msg.getElement(Env.getCtx(), "M_Product_ID", false));
        columnNames.add(Msg.getElement(Env.getCtx(), "C_Charge_ID", false));
        columnNames.add(Msg.getElement(Env.getCtx(), "Qty"));
        columnNames.add(Msg.getElement(Env.getCtx(), "Description", false));
        columnNames.add(Msg.getElement(Env.getCtx(), "AD_User_ID", false));
        return columnNames;
    }
}