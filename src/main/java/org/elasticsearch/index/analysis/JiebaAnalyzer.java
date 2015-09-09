package org.elasticsearch.index.analysis;

import com.huaban.analysis.jieba.WordDictionary;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiebaAnalyzer extends Analyzer {
    private final ESLogger log = Loggers.getLogger(JiebaAnalyzer.class);

    private final CharArraySet stopWords;

    private static final String DEFAULT_STOPWORD_FILE = "stopwords.txt";

    private static final String DEFAULT_SYNONYM_FILE = "synonym.txt";

    private static final String STOPWORD_FILE_COMMENT = "//";

    //同义词map 一个词对应N个同义词的list
    public static Map<String, List<String>> syonoymMap = new HashMap<String, List<String>>();


    /**
     * Returns an unmodifiable instance of the default stop-words set.
     *
     * @return an unmodifiable instance of the default stop-words set.
     */
    public static CharArraySet getDefaultStopSet() {
        return DefaultSetHolder.DEFAULT_STOP_SET;
    }

    static {
        System.out.println("start init syonoym map...");
        try {
            BufferedReader bufr = new BufferedReader(IOUtils.getDecodingReader(JiebaAnalyzer.class,
                    DEFAULT_SYNONYM_FILE, StandardCharsets.UTF_8));
            String str = null;
            while ((str = bufr.readLine()) != null) {
                str = str.trim();
                if(str.startsWith("#") || str.isEmpty()){
                    continue;
                }

                String[] words = str.split(",");
                if (words.length > 1) {
                    for (int i = 0; i < words.length; i++) {
                        List<String> synonmyList = new ArrayList<String>();
                        for (int j = 0; j < words.length; j++) {
                            if (i != j) {
                                synonmyList.add(words[j].toLowerCase());
                            }
                        }
                        syonoymMap.put(words[i].toLowerCase(), synonmyList);
                    }
                }
            }
            System.out.println("end init syonoym map...");
            bufr.close();
        } catch (IOException e) {
            System.out.println("init syonoym map error...");
            e.printStackTrace();
        }
    }


    /**
     * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer
     * class accesses the static final set the first time.;
     */
    private static class DefaultSetHolder {
        static final CharArraySet DEFAULT_STOP_SET;

        static {
            try {
                DEFAULT_STOP_SET = loadDefaultStopWordSet();
            } catch (IOException ex) {
                // default set should always be present as it is part of the
                // distribution (JAR)
                throw new RuntimeException(
                        "Unable to load default stopword set");
            }
        }

        static CharArraySet loadDefaultStopWordSet() throws IOException {
            // make sure it is unmodifiable as we expose it in the outer class
            return CharArraySet.unmodifiableSet(WordlistLoader.getWordSet(
                    IOUtils.getDecodingReader(JiebaAnalyzer.class,
                            DEFAULT_STOPWORD_FILE, StandardCharsets.UTF_8),
                    STOPWORD_FILE_COMMENT));
        }


    }

    private File configFile;
    private String type;

    private CharArraySet loadStopWords(File configFile) {
        try {
            return CharArraySet.unmodifiableSet(WordlistLoader.getWordSet(
                    new FileReader(new File(new File(configFile, "jieba"),
                            "stopwords.txt")), STOPWORD_FILE_COMMENT));
        } catch (IOException e) {
            return DefaultSetHolder.DEFAULT_STOP_SET;
        }
    }

    public JiebaAnalyzer(Settings indexSettings, Settings settings) {
        super();
        type = settings.get("seg_mode", "index");
        boolean stop = settings.getAsBoolean("stop", true);

        Environment env = new Environment(indexSettings);
        configFile = env.configFile();
        this.stopWords = stop ? this.loadStopWords(configFile)
                : CharArraySet.EMPTY_SET;
        WordDictionary.getInstance().init(configFile.toPath());
    }

    public JiebaAnalyzer(String segMode, File configFile, boolean isStop) {
        super();

        this.type = segMode;
        this.configFile = configFile;
        WordDictionary.getInstance().init(
                new File(configFile, "jieba").toPath());
        this.stopWords = isStop ? this.loadStopWords(configFile)
                : CharArraySet.EMPTY_SET;

        this.log.info("JiebaAnalyzer isStop = {}", isStop);
        this.log.info("JiebaAnalyzer stopWords = {}", this.stopWords.toString());
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                     Reader reader) {
        Tokenizer tokenizer;
        if (type.equals("other")) {
            tokenizer = new OtherTokenizer(Version.LUCENE_CURRENT, reader);
        } else {
            tokenizer = new SentenceTokenizer(reader);
        }
        TokenStream result = new JiebaTokenFilter(type, tokenizer);
        if (!type.equals("other") && !stopWords.isEmpty()) {
            result = new StopFilter(Version.LUCENE_CURRENT, result, stopWords);
        }
        return new TokenStreamComponents(tokenizer, result);
    }


}
