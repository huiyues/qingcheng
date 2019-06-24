import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;

public class SearchIndex {

    private IndexSearcher indexSearcher;
    private IndexReader indexReader;


    @Before
    public void test() throws Exception {
        indexReader = DirectoryReader.open(FSDirectory.open(new File("E:\\RuanJian\\chrome\\indexBank").toPath()));
        indexSearcher= new IndexSearcher(indexReader);
    }


    //根据区间进行查询
    @Test
    public void testQuery() throws IOException {
        //参数1：搜索域  参数2和参数3是搜索范围
        Query query = LongPoint.newRangeQuery("size",1l , 100l);
        printResult(query);
    }



    private void printResult(Query query) throws IOException {
        TopDocs topDocs = indexSearcher.search(query, 10);
        System.out.println("总记录数:"+topDocs.totalHits);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Document document = indexReader.document(doc);
            System.out.println(document.get("name"));
            System.out.println(document.get("path"));
            //System.out.println(document.get("content"));
            System.out.println(document.get("size"));
        }
        indexReader.close();
    }


    //语句搜索域查询queryParser
    @Test
    public void testQueryParser() throws ParseException, IOException {
        //创建queryParser对象
        //需要两个参数，一个默认搜索域和分析器
        QueryParser queryParser = new QueryParser("name",new IKAnalyzer());
        Query query = queryParser.parse("Lucene是Java的开始");

        printResult(query);
    }
}
