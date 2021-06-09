import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

public class Manager {
    Logger logger;
    String readerName;
    String readerCfg;
    String writerName;
    String writerCfg;
    String input;
    String output;
    IReader reader;
    IWriter writer;

    ArrayDeque<Pair<String, String>> executorsWithCfg = new ArrayDeque<>();


    private Manager(Logger log) {
        logger = log;
    }

    private boolean setConfig(String configFileName) {
        logger.info("\nProcessing: Configuring manager");

        if (configFileName == null) {
            logger.info("\nError: Invalid Manager config path.");
            return false;
        }

        ManagerGrammar grammar = new ManagerGrammar();
        HashMap<String, String> statements = CFGParser.fileDivisionByDelimiter(configFileName, grammar.delimiter());

        if (statements == null || !CFGParser.isGrammaticallyCorrect(statements, grammar)) {
            logger.info("\nError: Manager config grammar error");
            return false;
        }
        readerName = statements.get(grammar.token(0));
        writerName = statements.get(grammar.token(1));
        readerCfg = statements.get(grammar.token(2));
        writerCfg = statements.get(grammar.token(3));
        input = statements.get(grammar.token(4));
        output = statements.get(grammar.token(5));
        statements.remove(grammar.token(0));
        statements.remove(grammar.token(1));
        statements.remove(grammar.token(2));
        statements.remove(grammar.token(3));
        statements.remove(grammar.token(4));
        statements.remove(grammar.token(5));
        int number = 1;
        while(true) {
            String leftPart = grammar.executorSymbol + number;

            if (statements.get(leftPart) == null) {
                break;
            }

            String[] temp = CFGParser.lineDivisionByDelimiter(statements.get(leftPart) ,grammar.addingDelimiter);
            statements.remove(leftPart);

            if (temp == null) {
                logger.info("\nError: Manager config grammar error");
                return false;
            }

            executorsWithCfg.addLast(new Pair<>(temp[0], temp[1]));
            number++;
        }
        if (!statements.isEmpty()) {
            logger.info("\nWarning: Manager config warning");
        }
        return true;
    }



    <T extends IConfigurable> T createExecutor(String executorName)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Class<?> executorClass = Class.forName(executorName);
        Constructor<?> constructor = executorClass.getConstructor(Logger.class);
        T executor = (T)constructor.newInstance(logger);
        return executor;
    }

    boolean isSemanticCorrect() {
        logger.info("\nProcessing: Checking manager config for semantic errors");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(output);
            FileInputStream inputStream = new FileInputStream(input);
            IReader reader = createExecutor(readerName);
            IWriter writer = createExecutor(writerName);
            if (reader == null || writer == null) {
                logger.info("\nERROR: Manager config semantic error");
                return false;
            }

            Iterator<Pair<String, String>> value = executorsWithCfg.iterator();

            for (int i = 0; i < executorsWithCfg.size(); ++i) {
                IExecutor exe = createExecutor(value.next().first);
                if (exe == null) {
                    logger.info("\nERROR: Manager config semantic error");
                    return false;
                }
            }
        } catch(ClassNotFoundException | IOException | NoSuchMethodException | IllegalAccessException
                | InstantiationException | InvocationTargetException e) {
            logger.info("\nERROR: Manager config semantic error");
            return false;
        }
        return true;
    }

    public RC createPipeline() {
        RC rc;
        logger.info("\nProcessing: Creating pipeline");
        try {
            reader = createExecutor(readerName);
            rc = reader.setConfig(readerCfg);
            if (rc != RC.CODE_SUCCESS) {
                return rc;
            }

            FileInputStream inputStream = new FileInputStream(input);
            rc = reader.setInputStream(inputStream);
            if (rc != RC.CODE_SUCCESS) {
                return rc;
            }
            rc = reader.setProducer(null);
            if (rc != RC.CODE_SUCCESS) {
                return rc;
            }

            Pair<String, String> executorData = executorsWithCfg.pollFirst();
            IExecutor currExecutor = null, prevExecutor = null;
            if (executorData != null) {
                prevExecutor = createExecutor(executorData.first);
                rc = prevExecutor.setConfig(executorData.second);
                if (rc != RC.CODE_SUCCESS) {
                    return rc;
                }
                rc = prevExecutor.setProducer(reader);
                if (rc != RC.CODE_SUCCESS) {
                    return rc;
                }
                rc = reader.setConsumer(prevExecutor);
                if (rc != RC.CODE_SUCCESS) {
                    return rc;
                }
                executorData = executorsWithCfg.pollFirst();
                while (executorData != null) {
                    currExecutor = createExecutor(executorData.first);
                    rc = currExecutor.setConfig(executorData.second);
                    if (rc != RC.CODE_SUCCESS) {
                        return rc;
                    }
                     rc =  currExecutor.setProducer(prevExecutor);
                    if (rc != RC.CODE_SUCCESS) {
                        return rc;
                    }
                    rc = prevExecutor.setConsumer(currExecutor);
                    if (rc != RC.CODE_SUCCESS) {
                        return rc;
                    }
                    executorData = executorsWithCfg.pollFirst();
                    prevExecutor = currExecutor;
                }
            }

            writer = createExecutor(writerName);
            rc = writer.setConfig(writerCfg);
            if (rc != RC.CODE_SUCCESS) {
                return rc;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(output);
            rc = writer.setOutputStream(fileOutputStream);
            if (rc != RC.CODE_SUCCESS) {
                return rc;
            }
            rc = writer.setConsumer(null);
            if (rc != RC.CODE_SUCCESS) {
                return rc;
            }
            if (prevExecutor == null) {
                rc = writer.setProducer(reader);
                if (rc != RC.CODE_SUCCESS) {
                    return rc;
                }
                rc = reader.setConsumer(writer);
            } else {
                rc = writer.setProducer(prevExecutor);
                if (rc != RC.CODE_SUCCESS) {
                    return rc;
                }
                rc = prevExecutor.setConsumer(writer);
            }
            if (rc != RC.CODE_SUCCESS) {
                return rc;
            }

        } catch(IllegalAccessException |
                ClassNotFoundException |
                InstantiationException |
                NoSuchMethodException |
                FileNotFoundException |
                InvocationTargetException e) {
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        return RC.CODE_SUCCESS;
    }

    public static Manager createManager(String cfg, Logger logger) {
        Manager manager = new Manager(logger);
        if (!manager.setConfig(cfg) || !manager.isSemanticCorrect()) {
            return null;
        }
        return manager;
    }

    public RC execute() {
        return reader.execute();
    }
}
