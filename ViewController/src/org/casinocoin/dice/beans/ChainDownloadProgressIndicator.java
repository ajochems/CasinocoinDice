package org.casinocoin.dice.beans;


import org.apache.myfaces.trinidad.model.BoundedRangeModel;

import org.casinocoin.dice.DiceServer;

public class ChainDownloadProgressIndicator extends BoundedRangeModel {
    public ChainDownloadProgressIndicator() {
    }

    @Override
    public long getMaximum() {
        return DiceServer.peerMaxChainHeight();
    }

    @Override
    public long getValue() {
        return DiceServer.getStaticBlockHeight();
    }
}
