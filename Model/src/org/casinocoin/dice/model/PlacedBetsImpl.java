package org.casinocoin.dice.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.jbo.AttributeDef;
import oracle.jbo.Key;
import oracle.jbo.server.EntityDefImpl;
import oracle.jbo.server.EntityImpl;
import oracle.jbo.server.TransactionEvent;
// ---------------------------------------------------------------------
// ---    File generated by Oracle ADF Business Components Design Time.
// ---    Tue Aug 26 14:27:31 CEST 2014
// ---    Custom code may be added to this class.
// ---    Warning: Do not modify method signatures of generated methods.
// ---------------------------------------------------------------------
public class PlacedBetsImpl extends EntityImpl {
    /**
     * AttributesEnum: generated enum for identifying attributes and accessors. DO NOT MODIFY.
     */
    public enum AttributesEnum {
        Id,
        AbsId,
        ScsId,
        BetTime,
        BetValue,
        SenderCoinAddress,
        TxId,
        Confirmations,
        GameValue,
        LuckyHash,
        LuckyNumber,
        BetResult,
        Payout,
        BetError,
        Refunded,
        PayoutExecuted,
        PayoutExecutedTime,
        PayoutTxId,
        JackpotBet,
        JackpotPayoutTxId,
        JackpotResult,
        JackpotValue,
        AvailableBets;
        private static AttributesEnum[] vals = null;
        private static final int firstIndex = 0;

        public int index() {
            return AttributesEnum.firstIndex() + ordinal();
        }

        public static final int firstIndex() {
            return firstIndex;
        }

        public static int count() {
            return AttributesEnum.firstIndex() + AttributesEnum.staticValues().length;
        }

        public static final AttributesEnum[] staticValues() {
            if (vals == null) {
                vals = AttributesEnum.values();
            }
            return vals;
        }
    }


    public static final int ID = AttributesEnum.Id.index();
    public static final int ABSID = AttributesEnum.AbsId.index();
    public static final int SCSID = AttributesEnum.ScsId.index();
    public static final int BETTIME = AttributesEnum.BetTime.index();
    public static final int BETVALUE = AttributesEnum.BetValue.index();
    public static final int SENDERCOINADDRESS = AttributesEnum.SenderCoinAddress.index();
    public static final int TXID = AttributesEnum.TxId.index();
    public static final int CONFIRMATIONS = AttributesEnum.Confirmations.index();
    public static final int GAMEVALUE = AttributesEnum.GameValue.index();
    public static final int LUCKYHASH = AttributesEnum.LuckyHash.index();
    public static final int LUCKYNUMBER = AttributesEnum.LuckyNumber.index();
    public static final int BETRESULT = AttributesEnum.BetResult.index();
    public static final int PAYOUT = AttributesEnum.Payout.index();
    public static final int BETERROR = AttributesEnum.BetError.index();
    public static final int REFUNDED = AttributesEnum.Refunded.index();
    public static final int PAYOUTEXECUTED = AttributesEnum.PayoutExecuted.index();
    public static final int PAYOUTEXECUTEDTIME = AttributesEnum.PayoutExecutedTime.index();
    public static final int PAYOUTTXID = AttributesEnum.PayoutTxId.index();
    public static final int JACKPOTBET = AttributesEnum.JackpotBet.index();
    public static final int JACKPOTPAYOUTTXID = AttributesEnum.JackpotPayoutTxId.index();
    public static final int JACKPOTRESULT = AttributesEnum.JackpotResult.index();
    public static final int JACKPOTVALUE = AttributesEnum.JackpotValue.index();
    public static final int AVAILABLEBETS = AttributesEnum.AvailableBets.index();

    /**
     * This is the default constructor (do not remove).
     */
    public PlacedBetsImpl() {
    }

    /**
     * @param id key constituent

     * @return a Key object based on given key constituents.
     */
    public static Key createPrimaryKey(Integer id) {
        return new Key(new Object[] { id });
    }

    /**
     * @return the definition object for this instance class.
     */
    public static synchronized EntityDefImpl getDefinitionObject() {
        return EntityDefImpl.findDefObject("org.casinocoin.dice.model.PlacedBets");
    }


    @Override
    protected void doDML(int i, TransactionEvent transactionEvent) {
        //got to call first to super, so the record is posted 
        //and we can then ask for the last insert id
        super.doDML(i, transactionEvent); 
        //after the record is inserted, we can ask for the last insert id
        if (i == DML_INSERT) {
            populateAutoincrementAtt();
        }
    }
    
    /*
    * Determines if the Entity PK is marked as an autoincrement col
    * and executes a MySQL function to retrieve the last insert id
    */
    private void populateAutoincrementAtt() {
        EntityDefImpl entdef = this.getEntityDef();
        AttributeDef pk = null;
        //look for primary key with Autoincrement property set
        for (AttributeDef att : entdef.getAttributeDefs()) {
            if (att.isPrimaryKey() && (att.getProperty("AI") != null 
                && new Boolean(att.getProperty("AI").toString()))) {
                pk = att;
                break;
            }
        }
        if (pk != null) {
            try (PreparedStatement stmt = this.getDBTransaction().createPreparedStatement("SELECT last_insert_id()", 1)) {
                stmt.execute();
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        setAttribute(pk.getName(), rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

