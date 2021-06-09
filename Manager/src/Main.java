import ru.spbstu.pipeline.RC;

import java.util.logging.Logger;

public class Main {
    public static void RCProcessing(Logger logger, RC rc) {
        switch (rc) {
            case CODE_SUCCESS -> logger.info("Success");
            case CODE_FAILED_PIPELINE_CONSTRUCTION -> logger.info("ERROR: Failed pipeline construction");
            case CODE_CONFIG_GRAMMAR_ERROR -> logger.info("ERROR: Config grammar error");
            case CODE_CONFIG_SEMANTIC_ERROR -> logger.info("ERROR: Config semantic error");
            case CODE_FAILED_TO_READ -> logger.info("ERROR: Failed to read");
            case CODE_FAILED_TO_WRITE -> logger.info("ERROR: Failed to write");
            case CODE_INVALID_ARGUMENT -> logger.info("ERROR: Invalid arguments");
            case CODE_INVALID_INPUT_STREAM -> logger.info("ERROR: Invalid input stream");
            case CODE_INVALID_OUTPUT_STREAM -> logger.info("ERROR: Invalid output stream");
        }
    }


    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Main.class.getName());
        if (args.length != 1) {
            logger.info("ERROR: impossible to configure manager");
        }

        Manager manager = Manager.createManager(args[0], logger);
        if (manager != null) {
            RC rc = manager.createPipeline();
            RCProcessing(logger, rc);
            if (rc != RC.CODE_SUCCESS) {
                return;
            }
            rc = manager.execute();
            RCProcessing(logger, rc);
        } else {
            logger.info("ERROR: manager config error");
        }
    }
}
