import ru.spbstu.pipeline.BaseGrammar;

import java.util.*;
import java.io.*;

public class CFGParser {

    public static String[] lineDivisionByDelimiter(String s, String delimiter) {
        String[] lexemes = s.split(delimiter);
        if (lexemes.length != 2) {
            return null;
        }
        return lexemes;
    }

    public static HashMap<String, String> fileDivisionByDelimiter(String cfgName, String delimiter) {

        HashMap<String, String> statements = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(cfgName)))
        {
            String s = reader.readLine();

            while (s != null){
                String[] lexemes = lineDivisionByDelimiter(s, delimiter);
                if (lexemes == null || statements.containsKey(lexemes[0])) {
                    return null;
                }
                statements.put(lexemes[0], lexemes[1]);

                s = reader.readLine();
            }
        }
        catch(Exception ex){
            return null;
        }
        return statements;
    }

    public static boolean isGrammaticallyCorrect(HashMap<String, String> statements, BaseGrammar grammar) {

        for (int i = 0; i < grammar.numberTokens(); i++) {
            if (!statements.containsKey(grammar.token(i))) {
                return false;
            };
        }
        return true;
    }

}