import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import ru.spbstu.pipeline.*;

public class Reader_ implements IReader {

    IConsumer consumer;
    IProducer producer;
    FileInputStream inputStream;
    int sizeOfBuff;
    byte[] sliceOfData;


    final TYPE[] outputTypes = new TYPE[]{TYPE.BYTE, TYPE.CHAR, TYPE.SHORT};
    final Logger logger;

    public Reader_(Logger log) {
        logger = log;
    }

    private byte[] readPart() {
        if (inputStream == null) {
            return null;
        }

        try {
            int newDataSize;
            if (inputStream.available() == 0) {
                return null;
            } else newDataSize = Math.min(inputStream.available(), sizeOfBuff);

            byte[] newData = new byte[newDataSize];
            inputStream.read(newData);

            return newData;
        } catch (IOException exception) {
            return null;
        }
    }

    public TYPE[] getOutputTypes() {
        return outputTypes;
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

    public RC setInputStream(FileInputStream fis) {
        logger.info("\nProcessing: Setting input stream for Reader");
        if (fis == null) {
            logger.info("\nError: invalid input stream.");
            return RC.CODE_INVALID_ARGUMENT;
        }
        inputStream = fis;
        return RC.CODE_SUCCESS;
    }

    public RC setConsumer(IConsumer c) {
        logger.info("\nProcessing: Setting consumer for Reader");
        if (c == null) {
            logger.info("\nError: invalid argument");
            return RC.CODE_INVALID_ARGUMENT;
        }
        consumer = c;
        return RC.CODE_SUCCESS;
    }

    public RC setProducer(IProducer p) {
        logger.info("\nProcessing: Setting producer for Reader");
        producer = null;
        return RC.CODE_SUCCESS;
    }

    public RC setConfig(String configFileName) {
        logger.info("\nProcessing: Configuring Reader");
        if (configFileName == null) {
            logger.info("\nError: Invalid reader config path.");
            return RC.CODE_INVALID_ARGUMENT;
        }

        Reader_Grammar grammar = new Reader_Grammar();
        HashMap<String, String> statements = CFGParser.fileDivisionByDelimiter(configFileName, grammar.delimiter());

        if (statements == null) {
            logger.info("\nError: Manager config grammar error");
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        if (!CFGParser.isGrammaticallyCorrect(statements, grammar)) {
            logger.info("\nError: Manager config grammar error");
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        if (statements.size() > grammar.numberTokens()) {
            logger.info("\nWarning: Reader config warning");
        }

        String value = statements.get(grammar.token(0));
        char[] temp = value.toCharArray();
        for (char ch : temp) {
            if (!Character.isDigit(ch) && ch != '.') {
                logger.info("\nError: Manager config semantic error");
                return RC.CODE_CONFIG_SEMANTIC_ERROR;
            }
        }
        sizeOfBuff = Integer.parseInt(value);
        if (sizeOfBuff <= 0) {
            logger.info("\nError: Manager config semantic error");
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        }
        return RC.CODE_SUCCESS;
    }

    public RC execute() {
        logger.info("\nProcessing: Reading first slice of data");
        sliceOfData = readPart();
        if (sliceOfData == null) {
            logger.info("\nError: Empty stream.");
            return RC.CODE_FAILED_TO_READ;
        }
        while (sliceOfData != null) {
            logger.info("\nProcessing: Transferring data to consumer: " + Arrays.toString(sliceOfData));
            RC code = consumer.execute();
            if (code != RC.CODE_SUCCESS) {
                return code;
            }
            logger.info("\nProcessing: Reading next slice of data");
            sliceOfData = readPart();
        }
        return RC.CODE_SUCCESS;
    }

}

