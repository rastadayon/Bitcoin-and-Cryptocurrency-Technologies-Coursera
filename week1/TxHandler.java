import javax.swing.table.TableRowSorter;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */

    private boolean validateSignature(UTXO utxo, int index, Transaction tx, Transaction.Input input){
        Transaction.Output currOutput = utxoPool.getTxOutput(utxo);
        PublicKey PK = currOutput.address;
        return Crypto.verifySignature(PK, tx.getRawDataToSign(index), input.signature);
    }

    private boolean checkNonNegativeValues(ArrayList<Transaction.Output> outputs){
        for (Transaction.Output output : outputs) {
            if(output.value < 0)
                return false;
        }
        return true;
    }

    private boolean isTransactionFeeValid(double inputSum, ArrayList<Transaction.Output> outputs){
        double outputSum = 0;
        for (Transaction.Output output : outputs){
            outputSum += output.value;
        }
        return (inputSum >= outputSum);
    }

    public boolean isValidTx(Transaction tx) {

        double inputSum = 0;
        double outputSum = 0;

        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        Set <UTXO> claimedUTXOs = new HashSet<UTXO>();

        for(int i = 0; i < inputs.size(); i++){
            UTXO currUTXO = new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex);
            //first item to check
            if(! utxoPool.contains(currUTXO))
                return false;
            //second item to check
            if(!validateSignature(currUTXO, i, tx, inputs.get(i)))
                return false;
            //third item to check
            if(!claimedUTXOs.add(currUTXO))
                return false;

            inputSum += utxoPool.getTxOutput(currUTXO).value;
        }
        //fourth item to check
        if(!checkNonNegativeValues(outputs))
            return false;
        if(!isTransactionFeeValid(inputSum, outputs))
            return false;

        return true;
    }

    private void removeOldCoins(Transaction tx){
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        for (int i = 0; i < inputs.size(); i++){
            UTXO oldCoin = new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex);
            utxoPool.removeUTXO(oldCoin);
        }
    }

    private void addNewCoins(Transaction tx){
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        for (int i = 0; i < outputs.size(); i++){
            UTXO newCoin = new UTXO(tx.getHash(), i);
            utxoPool.addUTXO(newCoin, outputs.get(i));
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<>();
        for (int i = 0; i < possibleTxs.length; i++){
            if(isValidTx(possibleTxs[i])){
                validTxs.add(possibleTxs[i]);
                removeOldCoins(possibleTxs[i]);
                addNewCoins(possibleTxs[i]);
            }
        }
        Transaction[] validTxsArr = new Transaction[validTxs.size()];
        validTxs.toArray(validTxsArr);
        return validTxsArr;
    }

}
