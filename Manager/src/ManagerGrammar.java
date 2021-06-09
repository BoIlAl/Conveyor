import ru.spbstu.pipeline.BaseGrammar;

public class ManagerGrammar extends BaseGrammar {
    public final String addingDelimiter = "-";
    public final String executorSymbol = "executor";

    ManagerGrammar() {
        super(new String[]{"readerName", "writerName", "readerCfg", "writerCfg", "input", "output"});
    }
}

