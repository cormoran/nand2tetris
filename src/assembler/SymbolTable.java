
package src.assembler;

import java.util.HashMap;

class SymbolTable {
    HashMap<String, Integer> hashTable;

    public SymbolTable() {
        hashTable = new HashMap<String, Integer>();
    }

    public void addEntry(String symbol, int address) {
        hashTable.put(symbol, address);
    }

    public boolean contains(String symbol) {
        return hashTable.containsKey(symbol);
    }

    public int getAddress(String symbol) {
        return hashTable.get(symbol).intValue();
    }
}