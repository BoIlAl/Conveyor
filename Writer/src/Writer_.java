import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import ru.spbstu.pipeline.*;

public class Writer_ implements IWriter{
    IConsumer consumer;
    IProducer producer;
    FileOutputStream outputStream;
    TYPE type;
    IMediator mediator;

    final Logger logger;
    final TYPE[] inputTypes = new TYPE[]{TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};


    public Writer_(Logger log) {
        logger = log;
    }



    public RC setConsumer(IConsumer c) {
        logger.info("\nProcessing: Setting consumer for writer");
        consumer = null;
        return RC.CODE_SUCCESS;
    }
    public RC setProducer(IProducer p) {
        logger.info("\nProcessing: Setting producer for writer");
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
        logger.info("\nProcessing: Configuring writer");

        if (configFileName == null) {
            logger.info("\nError: Invalid writer config path");
            return RC.CODE_INVALID_ARGUMENT;
        }
        Writer_Grammar grammar = new Writer_Grammar();
        HashMap<String, String> statements = CFGParser.fileDivisionByDelimiter(configFileName, grammar.delimiter());

        if (statements == null) {
            logger.info("\nError: Writer config grammar error");
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        if (!CFGParser.isGrammaticallyCorrect(statements, grammar)) {
            logger.info("\nError: Writer config grammar error");
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        if (statements.size() > grammar.numberTokens()) {
            logger.info("\nWarning: Writer config warning");
        }
        return RC.CODE_SUCCESS;
    }

    public RC execute() {
        Object undefData = mediator.getData();
        byte[] data;
        switch (type) {
            case BYTE -> data = (byte[]) undefData;
            case SHORT -> data = Caster.ShortsToBytes((short[]) undefData);
            case CHAR -> data = Caster.CharsToBytes((char[]) undefData);
            default -> {
                logger.info("\nError: writer received data of an incorrect type");
                return RC.CODE_INVALID_ARGUMENT;
            }
        }

        if (data == null) {
            return RC.CODE_SUCCESS;
        }
        try{
            logger.info("\nProcessing: Writing next slice of data: " + Arrays.toString(data));
            outputStream.write(data);
            return RC.CODE_SUCCESS;
        } catch (IOException exception) {
            return RC.CODE_FAILED_TO_WRITE;
        }
    }

    public RC setOutputStream(FileOutputStream fos) {
        logger.info("\nProcessing: Setting output stream for Writer");
        if (fos == null) {
            logger.info("\nError: invalid output stream.");
            return RC.CODE_INVALID_ARGUMENT;
        }
        outputStream = fos;
        return RC.CODE_SUCCESS;
    }
}
