package src.vmtranslator;

public class Command {
    public CommandType type;
    public String arg1;
    public Integer arg2;

    public Command() {
        type = CommandType.C_NULL;
    }

    @Override
    public String toString() {
        return String.format("Command: type=%s arg1=%s arg2=%s", type.toString(), arg1 != null ? arg1 : "null",
                arg2 != null ? arg2.toString() : "null");

    }
}
