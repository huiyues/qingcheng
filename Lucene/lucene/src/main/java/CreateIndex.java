import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;


public class CreateIndex {


    //创建索引库
    public static void main(String[] args) throws Exception {
        //创建Director目录对象，指定索引库的位置
        Directory directory = FSDirectory.open(new File("E:\\RuanJian\\chrome\\indexBank").toPath());
        //指定中文分词解析器对象
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new IKAnalyzer());
        //创建indexWrite对象(写入索引对象)
        IndexWriter indexWriter = new IndexWriter(directory,indexWriterConfig);
        //读取磁盘上的文件，对应每个文件创建一个文档对象
        File file = new File("D:\\java视频资料\\javaSE\\day-46Lucene\\lucene\\资料\\searchsource");
        //得到一个文件数组
        File[] files = file.listFiles();
        //遍历文件
        for (File file1 : files) {
            //得到文件名
            String fileName = file1.getName();
            //得到文件路径
            String filePath = file1.getPath();
            //获取文件内容
            String fileContent = FileUtils.readFileToString(file1, "utf-8");
            //获取文件大小
            long fileSize = FileUtils.sizeOf(file1);

            //创建field域;参数1：域名称 ，参数2：文件名 ， 参数3：是否存储
            Field fieldName = new TextField("fieldName", fileName, Field.Store.YES);
            Field fieldPath = new TextField("fieldPath", filePath, Field.Store.YES);
            Field fieldContent = new TextField("fieldContent", fileContent, Field.Store.YES);
            Field fieldSize = new TextField("fieldSize", fileSize + "", Field.Store.YES);

            //创建文档对象
            Document document = new Document();
            document.add(fieldName);
            document.add(fieldPath);
            document.add(fieldContent);
            document.add(fieldSize);

            //把文档对象写入索引库
            indexWriter.addDocument(document);
        }
        //关闭写入所以对象
        indexWriter.close();
    }


    //根据索引查询索引库
    @Test
    public void queryIndex() throws Exception {
        //创建director对象
        Directory directory = FSDirectory.open(new File("E:\\RuanJian\\chrome\\indexBank").toPath());
        //创建indexReader对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建search对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        //创建query对象
        Query query = new TermQuery(new Term("name", "全文"));
        //调用查询方法进行查询,参数一：查询对象，参数二：查询多少条
        TopDocs topDocs = indexSearcher.search(query, 10);
        //查询总记录数
        System.out.println("总记录数：" + topDocs.totalHits);
        //查询文档中的详细信息
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            //获取文档id
            int docId = scoreDoc.doc;
            //通过文档Id获取文档对象
            Document document = indexReader.document(docId);
            //获取文档信息
            System.out.println(document.get("name"));
            System.out.println(document.get("path"));
            System.out.println(document.get("content"));
            System.out.println(document.get("size"));
            System.out.println("----------------------华丽分割线");
        }
        //关闭indexReader
        indexReader.close();
    }


    //标准分析器分析进行数据列表化
    @Test
    public void testTokenStream() throws IOException {
        //创建标准分析器对象
        Analyzer analyzer = new StandardAnalyzer();
        //使用分析器中的tokenStream分析文本得到一个列表
        TokenStream tokenStream = analyzer.tokenStream("", "The Spring Framework provides a comprehensive programming and configuration model.");
        //设置引用
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        //设置指针从头部开始
        tokenStream.reset();
        //遍历获取列表数据
        while (tokenStream.incrementToken()){
            //打印
            System.out.println(charTermAttribute.toString());
        }

        //关闭TokenStream
        tokenStream.close();
    }

    //中文标准数据分析器(以词进行分析)
    @Test
    public void testIK_Analyzer() throws IOException {
        //创建中文标准分析器
        Analyzer analyzer = new IKAnalyzer();
        //使用分析器分析中文文本得到关键词列表
        TokenStream tokenStream = analyzer.tokenStream("fieldName", "1）用户查询接口\n" +
                "\t\t用户输入查询条件的地方\n" +
                "\t\t例如：百度的搜索框\n" +
                "\t2）把关键词封装成一个查询对象\n" +
                "\t\t要查询的域\n" +
                "\t\t要搜索的关键词\n" +
                "\t3）执行查询\n" +
                "\t\t根据要查询的关键词到对应的域上进行搜索。\n" +
                "\t\t找到关键词，根据关键词找到 对应的文档\n" +
                "\t4）渲染结果\n" +
                "\t\t根据文档的id找到文档对象\n" +
                "\t\t对关键词进行高亮显示\n" +
                "\t\t分页处理\n" +
                "\t\t最终展示给用户看。");

        //设置引用
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        //调用方法不抛异常
        tokenStream.reset();
        //遍历列表数据
        while (tokenStream.incrementToken()){
            System.out.println(charTermAttribute.toString());
        }

        tokenStream.close();
    }


    //对存储索引值进行修改
    @Test
    public void test1()throws Exception{
        Directory directory = FSDirectory.open(new File("E:\\RuanJian\\chrome\\indexBank").toPath());
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new IKAnalyzer());
        IndexWriter indexWriter = new IndexWriter(directory,indexWriterConfig);

        File file = new File("D:\\java视频资料\\javaSE\\day-46Lucene\\lucene\\资料\\searchsource");
        File[] files = file.listFiles();
        for (File file1 : files) {
            String file1Name = file1.getName();
            String file1Path = file1.getPath();
            long size = FileUtils.sizeOf(file1);
            String content = FileUtils.readFileToString(file1, "utf-8");

            Field name = new TextField("name",file1Name,Field.Store.YES);
            Field path = new StoredField("path",file1Path);
            Field fieldContent = new TextField("content",content,Field.Store.YES);
            Field fieldSizeValue = new LongPoint("size",size);
            Field fieldSize = new StoredField("size",size);

            Document document = new Document();
            document.add(name);
            document.add(path);
            document.add(fieldContent);
            document.add(fieldSizeValue);
            document.add(fieldSize);

            indexWriter.addDocument(document);
        }

        indexWriter.close();
    }

    @Test
    public void test4() throws IOException {
        Analyzer analyzer = new IKAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("", "The Spring Framework provides a comprehensive programming and configuration model.");
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);


        tokenStream.reset();
        while (tokenStream.incrementToken()){
            System.out.println(charTermAttribute.toString());
        }

        tokenStream.close();
    }


    @Test
    public void test5() throws IOException {
      Analyzer analyzer = new StandardAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("name", "The Spring Framework provides a comprehensive programming and configuration model.");
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        while (tokenStream.incrementToken()){
            System.out.println(charTermAttribute.toString());
        }

        tokenStream.close();
    }
}
