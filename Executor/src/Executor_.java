import ru.spbstu.pipeline.*;

import java.util.*;
import java.util.logging.Logger;

public class Executor_ implements IExecutor {
    IConsumer consumer;
    IProducer producer;
    int minSizeToSeparation;
    int numOfSeparations;
    boolean mode;
    byte[] sliceOfData;
    IMediator mediator;
    TYPE type;


    ArrayDeque<byte[]> chainsOfEquals = new ArrayDeque<>();
    final TYPE[] inputTypes = new TYPE[]{TYPE.SHORT, TYPE.CHAR};
    final TYPE[] outputTypes = new TYPE[]{TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    final Logger logger;

    public Executor_(Logger log) {
        logger = log;
    }

    private boolean isDegreeOfParam(int num) {
        if (numOfSeparations == 0 || num <= 0) {
            return false;
        }

        int res = num, rem;
        while (res != 1) {
            rem = res % numOfSeparations;
            if (rem != 0) {
                return false;
            }
            res /= numOfSeparations;
        }
        return true;
    }


    private void createNewData (byte[] data, byte numOfEquals) {
        byte[] newData = new byte[data.length + 1];
        newData[0] = numOfEquals;
        System.arraycopy(data, 0, newData, 1, newData.length - 1);
        chainsOfEquals.add(newData);
    }

    private void separate(byte[] data) {

        if (data.length <= minSizeToSeparation) {
            createNewData(data, (byte)1);
            return;
        }

        byte[][] parts = new byte[numOfSeparations][];
        for (int i = 0; i < numOfSeparations; ++i) {
            parts[i] = Arrays.copyOfRange(data, i * data.length / numOfSeparations,(i + 1) * data.length / numOfSeparations);
        }

        byte numOfEquals = 0;
        for (int i = 1; i < numOfSeparations; ++i) {
            if (Arrays.equals(parts[i], parts[i - 1])) {
                ++numOfEquals;
            }
            else {
                if (numOfEquals == 0) {
                    separate(parts[i - 1]);
                } else {
                    createNewData(parts[i - 1], (byte)(numOfEquals + 1));
                    numOfEquals = 0;
                }
            }
        }
        if (numOfEquals == 0) {
            separate(parts[numOfSeparations - 1]);
        } else {
            createNewData(parts[numOfSeparations - 1], (byte)(numOfEquals + 1));
        }
    }

    public TYPE[] getOutputTypes() {
        return outputTypes.clone();
    }

    class ByteMediator implements IMediator {
        public Object getData() {
            return sliceOfData.clone();
        }
    }
    class CharMediator implements IMediator {
        public Object getData() {
            return Caster.bytesToChars(sliceOfData);
        }
    }
    class ShortMediator implements IMediator {
        public Object getData() {
            return Caster.bytesToShorts(sliceOfData);
        }
    }

    public IMediator getMediator (TYPE type) {
        return switch (type) {
            case BYTE -> new ByteMediator();
            case CHAR -> new CharMediator();
            case SHORT -> new ShortMediator();
        };
    }

    public RC setConsumer(IConsumer c) {
        logger.info("\nProcessing: Setting consumer for B.I.A. executor");
        if (c == null) {
            logger.info("\nError: Invalid argument");
            return RC.CODE_INVALID_ARGUMENT;
        }
        consumer = c;
        return RC.CODE_SUCCESS;
    }

    public RC setProducer(IProducer p) {
        logger.info("\nProcessing: Setting producer for B.I.A. executor");
        if (p == null) {
            logger.info("\nError: Invalid argument");
            return RC.CODE_INVALID_ARGUMENT;
        }
        producer = p;
        TYPE tempType = null;

        for (TYPE type1 : inputTypes) {
            for (TYPE type2 : producer.getOutputTypes()) {
                if (type1 == type2) {
                    tempType = type1;
                    break;
                }
                if (tempType != null) {
                    break;
                }
            }
        }
        if (tempType == null){
            logger.info("\nError: Incompatible input and output types");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        type = tempType;
        mediator = producer.getMediator(type);
        return RC.CODE_SUCCESS;
    }

    public RC setConfig(String configFileName) {
        logger.info("\nProcessing: Configuring B.I.A. executor");

        if (configFileName == null) {
            logger.info("\nError: Invalid executor config path");
            return RC.CODE_INVALID_ARGUMENT;
        }

        Executor_Grammar grammar = new Executor_Grammar();
        HashMap<String, String> statements = CFGParser.fileDivisionByDelimiter(configFileName, grammar.delimiter());

        if (statements == null) {
            logger.info("\nError: Executor config grammar error");
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        if (!CFGParser.isGrammaticallyCorrect(statements, grammar)) {
            logger.info("\nError: Executor config grammar error");
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        if (statements.size() > grammar.numberTokens()) {
            logger.info("\nWarning: Executor config warning");
        }

        String value = statements.get(grammar.token(0));

        char[] temp = value.toCharArray();
        for (char ch : temp) {
            if (!Character.isDigit(ch) && ch != '.') {
                logger.info("\nExecutor: Manager config semantic error");
                return RC.CODE_CONFIG_SEMANTIC_ERROR;
            }
        }
        minSizeToSeparation = Integer.parseInt(value);
        if (minSizeToSeparation <= 0) {
            logger.info("\nExecutor: Manager config semantic error");
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }

        value = statements.get(grammar.token(1));

        temp = value.toCharArray();
        for (char ch : temp) {
            if (!Character.isDigit(ch) && ch != '.') {
                logger.info("\nExecutor: Manager config semantic error");
                return RC.CODE_CONFIG_SEMANTIC_ERROR;
            }
        }

        numOfSeparations = Integer.parseInt(value);
        if (numOfSeparations <= 0) {
            logger.info("\nExecutor: Manager config semantic error");
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }

        value = statements.get(grammar.token(2));
        mode = Boolean.parseBoolean(value);

        return RC.CODE_SUCCESS;
    }
    
    public RC execute() {
        Object undefData = mediator.getData();
        if (undefData == null) {
            return RC.CODE_INVALID_ARGUMENT;
        }
        byte[] data;
        switch (type) {
            case BYTE -> data = (byte[]) undefData;
            case SHORT -> data = Caster.ShortsToBytes((short[]) undefData);
            default -> {
                logger.info("\nError: B.I.A. executor received data of an incorrect type");
                return RC.CODE_INVALID_ARGUMENT;
            }
        }
        logger.info("\nProcessing: B.I.A. executor is starting work with slice of data: " + Arrays.toString(data));

        if (mode) {
            if (!isDegreeOfParam(data.length)) {
                logger.info("\nError: Array size is incorrect");
                return RC.CODE_INVALID_ARGUMENT;
            }
            logger.info("\nProcessing: Starting compression");
            separate(data);
        } else {
            logger.info("\nProcessing: Starting decoding");
            if (data[0] <= 0) {
                logger.info("\nError: Invalid input data");
                return RC.CODE_INVALID_ARGUMENT;
            } else {
                byte[] newData = new byte[(data.length - 1) * data[0]];
                for (int i = 1; i < data.length; ++i) {
                    for (int j = 0; j < data[0]; ++j) {
                        newData[j * (data.length - 1) + i - 1] = data[i];
                    }
                }
                chainsOfEquals.add(newData);
            }
        }

        sliceOfData = chainsOfEquals.pollFirst();
        while (sliceOfData != null) {
            logger.info("\nProcessing: Transferring data to consumer: " + Arrays.toString(sliceOfData));
            consumer.execute();
            sliceOfData = chainsOfEquals.pollFirst();
        }
        return RC.CODE_SUCCESS;
    }
}
