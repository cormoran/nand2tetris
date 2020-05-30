package src.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class SymbolTable {
    private HashMap<SymbolKind, HashMap<String, SymbolInfoObject>> symbolTable;
    private HashMap<SymbolKind, Integer> varIndex;

    private final Set<SymbolKind> functionScopeSymbolKinds = new HashSet<SymbolKind>(
            Arrays.asList(SymbolKind.ARG, SymbolKind.VAR));
    private final Set<SymbolKind> classScopeSymbolKinds = new HashSet<SymbolKind>(
            Arrays.asList(SymbolKind.STATIC, SymbolKind.FIELD));
    private final List<SymbolKind> allSymbolKinds = Arrays.asList(SymbolKind.ARG, SymbolKind.VAR, SymbolKind.STATIC,
            SymbolKind.FIELD); // order: function scope -> class scope

    SymbolTable() {
        symbolTable = new HashMap<>();
        varIndex = new HashMap<>();
        for (SymbolKind kind : allSymbolKinds) {
            symbolTable.put(kind, new HashMap<>());
            varIndex.put(kind, 0);
        }
    }

    public void startSubroutine() {
        for (SymbolKind i : functionScopeSymbolKinds) {
            symbolTable.put(i, new HashMap<>());
            varIndex.put(i, 0);
        }
    }

    public void define(String name, String type, SymbolKind kind) throws Exception {
        HashMap<String, SymbolInfoObject> table = symbolTable.get(kind);
        Integer index = varIndex.get(kind);
        if (functionScopeSymbolKinds.contains(kind)) {
            for (SymbolKind k : functionScopeSymbolKinds) {
                if (symbolTable.get(k).containsKey(name)) {
                    throw new Exception(name + " is already defined");
                }
            }
        }
        if (classScopeSymbolKinds.contains(kind)) {
            for (SymbolKind k : classScopeSymbolKinds) {
                if (symbolTable.get(k).containsKey(name)) {
                    throw new Exception(name + " is already defined");
                }
            }
        }
        table.put(name, new SymbolInfoObject(index, type, kind));
        varIndex.put(kind, index + 1);
    }

    public int varCount(SymbolKind kind) {
        return varIndex.get(kind);
    }

    public SymbolKind kindOf(String name) throws Exception {
        for (SymbolKind i : allSymbolKinds) {
            if (symbolTable.get(i).containsKey(name))
                return i;
        }
        throw new Exception("unknown symbol " + name);
    }

    public String typeOf(String name) throws Exception {
        for (SymbolKind i : allSymbolKinds) {
            if (symbolTable.get(i).containsKey(name)) {
                return symbolTable.get(i).get(name).type;
            }
        }
        throw new Exception("unknown symbol " + name);
    }

    public int indexOf(String name) throws Exception {
        for (SymbolKind i : allSymbolKinds) {
            if (symbolTable.get(i).containsKey(name)) {
                return symbolTable.get(i).get(name).index;
            }
        }
        throw new Exception("unknown symbol " + name);
    }

    public boolean contains(String name) {
        for (SymbolKind i : allSymbolKinds) {
            if (symbolTable.get(i).containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    class SymbolInfoObject {
        int index;
        String type;
        SymbolKind kind;

        SymbolInfoObject(int index, String type, SymbolKind kind) {
            this.index = index;
            this.type = type;
            this.kind = kind;
        }
    }
}