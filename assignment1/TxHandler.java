import java.util.ArrayList;
import java.util.List;

public class TxHandler {

    /**
     * Current collection of unspent transaction outputs
     */
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
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
    public boolean isValidTx(Transaction tx) {
        UTXOPool uniqueUTXOPool = new UTXOPool();
        double inputValuesSum = 0;
        double outputValuesSum = 0;

        for (int i = 0; i < tx.numInputs(); i++){
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!utxoPool.contains(utxo))
                return false;

            Transaction.Output output = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature))
                return false;

            if (uniqueUTXOPool.contains(utxo))
                return  false;
            uniqueUTXOPool.addUTXO(utxo, output);
            inputValuesSum += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()){
            if (output.value < 0)
                return false;
            outputValuesSum += output.value;
        }

        return inputValuesSum >= outputValuesSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> transactions = new ArrayList<>();

        for (Transaction tx : possibleTxs){
            if (isValidTx(tx)){
                transactions.add(tx);
                for (Transaction.Input input : tx.getInputs()){
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++){
                    Transaction.Output output = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, output);
                }
            }
        }

        return transactions.toArray(new Transaction[transactions.size()]);
    }

}
