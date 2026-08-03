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

import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.apps.form.WCreateFromWindow;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.compiere.model.GridTab;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MRequisition;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Messagebox;

/**
 * @author Fabian Aguilar faaguilar@gmail.com
 * @author Ahmad Fauzi Ridwan @eonebyte (Adapted for iDempiere 13 / ZK 10.x)
 */
public class WCreateFromOrderUI extends CreateFromOrder implements EventListener<Event> {

	private WCreateFromWindow window;
	private int p_WindowNo;
	private CLogger log = CLogger.getCLogger(getClass());

	protected Label requisitionLabel = new Label(Msg.translate(Env.getCtx(), "Requisition"));
	protected WEditor requisitionLookup;

	protected Label userLabel = new Label(Msg.translate(Env.getCtx(), "User"));
	protected WEditor userLookup;

	protected Label orgLabel = new Label(Msg.translate(Env.getCtx(), "Org"));
	protected WTableDirEditor orgLookup;

	protected Label costCenterLabel = new Label(Msg.translate(Env.getCtx(), "CostCenter"));
	protected WTableDirEditor costCenterLookup;

	public WCreateFromOrderUI(GridTab tab) {
		super(tab);
		log.info(getGridTab().toString());

		window = new WCreateFromWindow(this, getGridTab().getWindowNo());
		p_WindowNo = getGridTab().getWindowNo();

		try {
			if (!dynInit())
				return;
			zkInit();
			setInitOK(true);
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			setInitOK(false);
		}
		AEnv.showWindow(window);
	}

	@Override
	public boolean dynInit() throws Exception {
		log.config("");
		super.dynInit();

		// Tombol Refresh
		Button refreshButton = window.getConfirmPanel().createButton(ConfirmPanel.A_REFRESH);
		refreshButton.addEventListener(Events.ON_CLICK, this);
		window.getConfirmPanel().addButton(refreshButton);

		// FDialog sudah dihapus di iDempiere 13, diganti Messagebox
		if (getGridTab().getValue("C_Order_ID") == null) {
			Messagebox.show(
					Msg.translate(Env.getCtx(), "SaveErrorRowNotFound"),
					"Error", Messagebox.OK, Messagebox.ERROR);
			return false;
		}
		if (getGridTab().getValueAsBoolean("IsSOTrx")) {
			Messagebox.show(
					"Only Purchase Order is supported.",
					"Error", Messagebox.OK, Messagebox.ERROR);
			return false;
		}

		window.setTitle("Create From Requisition");

		MLookup lookup = MLookupFactory.get(
				Env.getCtx(), p_WindowNo, 0,
				MColumn.getColumn_ID(MRequisition.Table_Name, MRequisition.COLUMNNAME_M_Requisition_ID),
				DisplayType.Search);
		requisitionLookup = new WSearchEditor("M_Requisition_ID", false, false, true, lookup);

		lookup = MLookupFactory.get(
				Env.getCtx(), p_WindowNo, 0,
				MColumn.getColumn_ID(MRequisition.Table_Name, MRequisition.COLUMNNAME_AD_User_ID),
				DisplayType.Search);
		userLookup = new WSearchEditor("AD_User_ID", false, false, true, lookup);

		lookup = MLookupFactory.get(
				Env.getCtx(), p_WindowNo, 0,
				MColumn.getColumn_ID(MRequisition.Table_Name, MRequisition.COLUMNNAME_AD_Org_ID),
				DisplayType.TableDir);
		orgLookup = new WTableDirEditor("AD_Org_ID", false, false, true, lookup);
		orgLookup.setValue(getGridTab().getValue("AD_Org_ID"));
		orgLookup.getComponent().addEventListener(Events.ON_CHANGE, this);

		lookup = MLookupFactory.get(
				Env.getCtx(), p_WindowNo, 0,
				MColumn.getColumn_ID(MRequisition.Table_Name, "C_CostCenter_ID"),
				DisplayType.TableDir);
		costCenterLookup = new WTableDirEditor("C_CostCenter_ID", false, false, true, lookup);
		costCenterLookup.setValue(getGridTab().getValue("C_CostCenter_ID"));
		costCenterLookup.getComponent().addEventListener(Events.ON_CHANGE, this);

		return true;
	}

	protected void zkInit() throws Exception {
		requisitionLabel.setText(Msg.getElement(Env.getCtx(), "M_Requisition_ID"));
		userLabel.setText(Msg.getElement(Env.getCtx(), "AD_User_ID", false));
		orgLabel.setText(Msg.getElement(Env.getCtx(), "AD_Org_ID", false));
		costCenterLabel.setText(Msg.getElement(Env.getCtx(), "C_CostCenter_ID", false));

		Borderlayout parameterLayout = new Borderlayout();
		parameterLayout.setHeight("130px");
		parameterLayout.setWidth("100%");

		Panel parameterPanel = window.getParameterPanel();
		parameterPanel.appendChild(parameterLayout);

		Grid parameterStdLayout = GridFactory.newGridLayout();
		Panel parameterStdPanel = new Panel();
		parameterStdPanel.appendChild(parameterStdLayout);

		Center center = new Center();
		parameterLayout.appendChild(center);
		center.appendChild(parameterStdPanel);

		Columns columns = new Columns();
		parameterStdLayout.appendChild(columns);

		Column col1 = new Column();
		columns.appendChild(col1);
		Column col2 = new Column();
		col2.setWidth("35%");
		columns.appendChild(col2);
		Column col3 = new Column();
		col3.setWidth("15%");
		columns.appendChild(col3);
		Column col4 = new Column();
		col4.setWidth("35%");
		columns.appendChild(col4);

		Rows rows = (Rows) parameterStdLayout.newRows();

		Row row1 = rows.newRow();
		row1.appendChild(requisitionLabel.rightAlign());
		row1.appendChild(requisitionLookup.getComponent());
		row1.appendChild(orgLabel.rightAlign());
		row1.appendChild(orgLookup.getComponent());

		Row row2 = rows.newRow();
		row2.appendChild(userLabel.rightAlign());
		row2.appendChild(userLookup.getComponent());
		row2.appendChild(costCenterLabel.rightAlign());
		row2.appendChild(costCenterLookup.getComponent());
		// row2.appendChild(new Label()); // placeholder
		// row2.appendChild(new Label()); // placeholder
	}

	@Override
	public void onEvent(Event e) throws Exception {
		if (log.isLoggable(Level.CONFIG))
			log.config("Action=" + e.getTarget().getId());
		if (e.getTarget().equals(window.getConfirmPanel().getButton(ConfirmPanel.A_REFRESH))) {
			loadRequisition();
			window.tableChanged(null);
		}
	}

	protected void loadRequisition() {
		loadTableOIS(getRequisitionData(
				requisitionLookup.getValue(),
				orgLookup.getValue(),
				userLookup.getValue(),
				costCenterLookup.getValue()));
	}

	protected void loadTableOIS(Vector<?> data) {
		window.getWListbox().clear();
		window.getWListbox().getModel().removeTableModelListener(window);
		ListModelTable model = new ListModelTable(data);
		model.addTableModelListener(window);
		window.getWListbox().setData(model, getOISColumnNames());
		configureMiniTable(window.getWListbox());
	}

	@Override
	public void showWindow() {
		window.setVisible(true);
	}

	@Override
	public void closeWindow() {
		window.dispose();
	}

	@Override
	public Object getWindow() {
		return window;
	}
}