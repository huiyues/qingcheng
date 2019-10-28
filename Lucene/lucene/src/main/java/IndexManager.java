import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;

public class IndexManager {

    private IndexWriter indexWriter;

    @Before
    public void test() throws Exception{
        indexWriter = new IndexWriter(FSDirectory.open(new File("E:\\RuanJian\\chrome\\indexBank").toPath()),
                new IndexWriterConfig(new IKAnalyzer()));
    }


    @After
    public void after() throws IOException {
        indexWriter.close();
    }


    @Test
    public void testIndexManager() throws Exception {
        //创建indexWrite并且要以IKAnalyzer作为解析器
        indexWriter = new IndexWriter(FSDirectory.open(new File("E:\\RuanJian\\chrome\\indexBank").toPath()),
                new IndexWriterConfig(new IKAnalyzer()));

        //创建文档对象
        Document document = new Document();
        document.add(new TextField("name", "这是新添加内容", Field.Store.YES));
        document.add(new StoredField("path", "c:/tem/index"));
        document.add(new TextField("content", "这是新添加文本内容", Field.Store.YES));
        document.add(new LongPoint("size", 1000));

        //将文档对象写入到索引库
        indexWriter.addDocument(document);

    }

    //删除所有
    @Test
    public void testDelete() throws IOException {
        indexWriter.deleteAll();
    }

    //删除指定的名称的文档对象
    @Test
    public void testDeleteId() throws IOException {
     indexWriter.deleteDocuments(new Term("name","apache"));
    }


    //修改
    @Test
    public void testUpdate() throws IOException {
     Document document = new Document();
     document.add(new TextField("name","这是更新之后的内容",Field.Store.YES));
     document.add(new TextField("name2","这是更新之后的内容2",Field.Store.YES));
     document.add(new TextField("name3","这是更新之后的内容3",Field.Store.YES));

     indexWriter.updateDocument(new Term("name","spring"),document );
    }


    @Test
    public void testFind()throws Exception{

        IndexReader indexReader =  DirectoryReader.open(FSDirectory.open(
                new File("E:\\RuanJian\\chrome\\indexBank").toPath())
        );
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        Query query = new TermQuery(new Term("name","apache"));
        TopDocs topDocs = indexSearcher.search(query, 10);
        System.out.println("总记录数:"+topDocs.totalHits);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;
            Document document = indexReader.document(doc);
            System.out.println(document.get("name"));
            System.out.println(document.get("path"));
           // System.out.println(document.get("content"));
            System.out.println(document.get("size"));
            System.out.println("------------------华丽的分割线");
        }
    }
}
