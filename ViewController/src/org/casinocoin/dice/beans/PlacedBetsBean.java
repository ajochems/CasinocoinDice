package org.casinocoin.dice.beans;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.data.RichTable;

import oracle.adf.view.rich.context.AdfFacesContext;

import org.apache.myfaces.trinidad.event.PollEvent;

public class PlacedBetsBean {
    private RichTable placedBetsTable;

    public PlacedBetsBean() {
    }

    public void setPlacedBetsTable(RichTable placedBetsTable) {
        this.placedBetsTable = placedBetsTable;
    }

    public RichTable getPlacedBetsTable() {
        return placedBetsTable;
    }
    
    public void refreshPlacedBets(PollEvent pollEvent) {
        BindingContext bc = BindingContext.getCurrent();
        DCBindingContainer bindings = (DCBindingContainer) bc.getCurrentBindingsEntry();
        DCIteratorBinding placedBetsIter = bindings.findIteratorBinding("PlacedBetsView1Iterator");
        placedBetsIter.executeQuery();
        AdfFacesContext.getCurrentInstance().addPartialTarget(placedBetsTable);
    }
}
