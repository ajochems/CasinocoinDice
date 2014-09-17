package org.casinocoin.dice;

import java.text.DateFormat;

import java.util.Date;

import java.util.List;

import oracle.adf.share.logging.ADFLogger;

import org.casinocoin.core.AbstractBlockChain;
import org.casinocoin.core.BlockChainListener;
import org.casinocoin.core.Sha256Hash;
import org.casinocoin.core.StoredBlock;
import org.casinocoin.core.Transaction;
import org.casinocoin.core.VerificationException;

public class WebBlockChainListener implements BlockChainListener {
    
    private static ADFLogger log = ADFLogger.createADFLogger(WebBlockChainListener.class);

    @Override
    public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
        log.finest("New Best Block: " + block.getHeight());
        DiceServer.setBlockHeight(block.getHeight());
        DiceServer.setLastBlockTime(new Date());
    }

    @Override
    public void receiveFromBlock(Transaction tx, StoredBlock block, AbstractBlockChain.NewBlockType blockType,
                                 int relativityOffset) throws VerificationException {
        log.info("Receive from block: " + block.getHeight() + " Transaction: " + tx.toString(null));
    }

    @Override
    public void notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock block,
                                           AbstractBlockChain.NewBlockType blockType,
                                           int relativityOffset) throws VerificationException {
        log.info("Transaction is in block: " + block.getHeight());
    }

    @Override
    public void reorganize(StoredBlock splitPoint, List<StoredBlock> oldBlocks, List<StoredBlock> newBlocks) {
        log.info("Reorganize splitPoint: " + splitPoint.getHeight());
    }

    @Override
    public boolean isTransactionRelevant(Transaction tx) {
        return false;
    }
}
