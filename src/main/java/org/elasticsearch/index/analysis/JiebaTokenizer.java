package org.elasticsearch.index.analysis;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import org.apache.lucene.analysis.util.CharArraySet;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kshi on 9/11/15.
 */

public class JiebaTokenizer extends Tokenizer {

    private final ESLogger log = Loggers.getLogger(JiebaTokenFilter.class);

    JiebaSegmenter segmenter;

    private Iterator<SegToken> tokenIter;
    private List<SegToken> array;


    //词元文本属性
    private final CharTermAttribute termAtt;
    //词元位移属性
    private final OffsetAttribute offsetAtt;
    //词元分类属性（该属性分类参考org.wltea.analyzer.core.Lexeme中的分类常量）
    private final TypeAttribute typeAtt;
    //记录最后一个词元的结束位置
    private int endPosition;

    private int skippedPositions;
    private String type;

    private PositionIncrementAttribute posIncrAtt;

    public JiebaTokenizer(Reader in, Settings settings, Environment environment) {
        super(in);
        offsetAtt = addAttribute(OffsetAttribute.class);
        termAtt = addAttribute(CharTermAttribute.class);
        typeAtt = addAttribute(TypeAttribute.class);
        posIncrAtt = addAttribute(PositionIncrementAttribute.class);
        segmenter = new JiebaSegmenter();
        type = settings.get("seg_mode", "index");
    }

    @Override
    public boolean incrementToken() throws IOException {
        if(input == null){
            return false;
        }

        if (tokenIter == null || !tokenIter.hasNext()) {
            String se = readerToString(input);
            tokenIter = getSegList(se).iterator();
            if (!tokenIter.hasNext())
                return false;
        }
        clearAttributes();
        SegToken token = tokenIter.next();
        // System.out.println("token:" + token);
        offsetAtt.setOffset(token.startOffset, token.endOffset);
        String tokenString = token.word;
        termAtt.copyBuffer(tokenString.toCharArray(), 0, tokenString.length());
        typeAtt.setType("word");
        return true;
    }

    private String readerToString(Reader in) {
        StringBuilder builder = new StringBuilder();
        int charsRead = -1;
        char[] chars = new char[100];
        do {
            try {
                charsRead = in.read(chars, 0, chars.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (charsRead > 0)
                builder.append(chars, 0, charsRead);
        } while (charsRead > 0);
        String stringReadFromReader = builder.toString();
        return stringReadFromReader;
    }

    private List<SegToken> getSegList(String stringReadFromReader) {
        if (type.equals("other")) {
            array = new ArrayList<SegToken>();
            String token = stringReadFromReader;
            char[] ctoken = token.toCharArray();
            for (int i = 0; i < ctoken.length; i++) {
                if (ctoken[i] > 0xFF00 && ctoken[i] < 0xFF5F)
                    ctoken[i] = (char) (ctoken[i] - 0xFEE0);
                if (ctoken[i] > 0x40 && ctoken[i] < 0x5b)
                    ctoken[i] = (char) (ctoken[i] + 0x20);
            }
            token = String.valueOf(ctoken);
            array.add(new SegToken(token, 0, token.length()));
        } else if (type.equals("search")) {
            array = segmenter.process(stringReadFromReader, JiebaSegmenter.SegMode.SEARCH);
        } else {  // "index"
            array = segmenter.process(stringReadFromReader, JiebaSegmenter.SegMode.INDEX);
        }
        List<SegToken> array2 = new ArrayList<SegToken>();
        CharArraySet stopSet = JiebaAnalyzer.getDefaultStopSet();

        for (int i = 0; i < array.size(); i++) {
            String word = array.get(i).word.trim();
            if (!word.equals("") && !stopSet.contains(word)) {
                array2.add(array.get(i));
            }
        }
        return array2;
    }
}


