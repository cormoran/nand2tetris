package src.compiler;

import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, SymbolInfoObject> classSymbolTable, subroutineSymbolTable;
    private HashMap<SymbolKind, Integer> classVarCount, subroutineVarCount;
    private int classIndex, subroutineIndex;

    SymbolTable() {
        classSymbolTable = new HashMap<>();
        subroutineSymbolTable = new HashMap<>();
        classVarCount = new HashMap<>();
        subroutineVarCount = new HashMap<>();
        classIndex = 0;
        subroutineIndex = 0;
    }

    public void startSubroutine() {
        subroutineSymbolTable = new HashMap<>();
        subroutineVarCount = new HashMap<>();
        subroutineIndex = 0;
    }

    public void define(String name, String type, SymbolKind kind) throws Exception {
        switch (kind) {
            case STATIC:
            case FIELD:
                if (classSymbolTable.containsKey(name))
                    throw new Exception(name + " is already defined in current class scope");
                classSymbolTable.put(name, new SymbolInfoObject(classIndex++, type, kind));
                classVarCount.put(kind, classVarCount.getOrDefault(kind, 0) + 1);
                break;
            case ARG:
            case VAR:
                if (subroutineSymbolTable.containsKey(name))
                    throw new Exception(name + " is already defined in current subroutine scope");
                subroutineSymbolTable.put(name, new SymbolInfoObject(subroutineIndex++, type, kind));
                subroutineVarCount.put(kind, subroutineVarCount.getOrDefault(kind, 0) + 1);
                break;
            default:
                throw new Exception("unknown kind " + kind.toString());
        }
    }

    public int varCount(SymbolKind kind) {
        return subroutineVarCount.getOrDefault(kind, 0) + classVarCount.getOrDefault(kind, 0);
    }

    public SymbolKind kindOf(String name) throws Exception {
        if (subroutineSymbolTable.containsKey(name))
            return subroutineSymbolTable.get(name).kind;
        if (classSymbolTable.containsKey(name))
            return classSymbolTable.get(name).kind;
        throw new Exception("unknown symbol " + name);
    }

    public String typeOf(String name) throws Exception {
        if (subroutineSymbolTable.containsKey(name))
            return subroutineSymbolTable.get(name).type;
        if (classSymbolTable.containsKey(name))
            return classSymbolTable.get(name).type;
        throw new Exception("unknown symbol " + name);
    }

    public int indexOf(String name) throws Exception {
        if (subroutineSymbolTable.containsKey(name))
            return subroutineSymbolTable.get(name).index;
        if (classSymbolTable.containsKey(name))
            return classSymbolTable.get(name).index;
        throw new Exception("unknown symbol " + name);
    }

    public boolean contains(String name) {
        return subroutineSymbolTable.containsKey(name) || classSymbolTable.containsKey(name);
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