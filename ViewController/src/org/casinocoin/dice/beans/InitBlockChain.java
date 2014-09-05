package org.casinocoin.dice.beans;

import oracle.adf.share.logging.ADFLogger;

import oracle.jbo.ApplicationModule;
import oracle.jbo.client.Configuration;

import org.apache.myfaces.trinidad.event.PollEvent;

import org.casinocoin.dice.DiceServer;
import org.casinocoin.dice.model.common.SettingsView;
import org.casinocoin.store.BlockStoreException;

public class InitBlockChain {
    
    private static ADFLogger log = ADFLogger.createADFLogger(InitBlockChain.class);
    
    private static boolean serverBlockChainUpToDate = false;
    private ApplicationModule diceAM;
    private String amDef = "org.casinocoin.dice.model.DiceAppModule";
    private String config = "AppModuleLocal";
    
    public InitBlockChain() {
        log.info("InitBlockChain()");
    }

    public void checkBlockChainUpToDate(PollEvent pollEvent) {
        log.info("checkBlockChainUpToDate: " + pollEvent.toString());
        try{
            int storeHeight = DiceServer.getDiceService().store().getChainHead().getHeight();
            int peerHeight = DiceServer.getDiceService().peerGroup().getMostCommonChainHeight();
            if(storeHeight == peerHeight){
                serverBlockChainUpToDate = true;
                // check if we need to set init_complete
                if(!StartPage.isServerInitComplete()){
                    // update database
                    diceAM = Configuration.createRootApplicationModule(amDef,config);
                    SettingsView settingsVo = (SettingsView) diceAM.findViewObject("SettingsView1");
                    settingsVo.setServerInitComplete("1");
                    // commit changes
                    diceAM.getTransaction().commit();
                    // release app module
                    Configuration.releaseRootApplicationModule(diceAM,true);
                    // reinit StartPage by calling the init
                    StartPage sp = new StartPage();
                    sp.showDiceStart();
                }
            } else {
                serverBlockChainUpToDate = false;
            }
        } catch(BlockStoreException be){
            serverBlockChainUpToDate = false;
        }
    }
}
