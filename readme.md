# 搜索引擎第二次作业报告
## 基本任务

###	跑通流程
学习了Tomcat，MyEclipse，Lucene的使用，并通过ImageIndexer建立索引。需要注意的是存放的路径，
为了正确运行Tomcat，forIndex文件夹应当放在Tomcat安装目录下，而图片应当放到TOMCAT_PATH\webapps\ROOT\pictures\下

###	实现BM25评分算法
* 为了使用lucene4.0的新功能，将工程移植到Lucene4.0。做法如下
  	1. 下载Lucene4.0的jar包lucene-core-4.0.0.jar，并替换原先的3.5版本jar包。注意任何高于4.0的版本都会出现兼容性问题，任何低于4.0的版本也会出现兼容性问题
	2. 从https://code.google.com/archive/p/ik-analyzer/下载IKAnalyzer的IK Analyzer 2012FF_hf1 其他版本与lucene4.0不兼容。任何其他版本经过测试都存在兼容性问题
	3. 因为Lucene3.x到Lucene4.x有重大的接口变化，需要将源代码相应部分修改，例如将```ImageIndexer```构造函数修改为
	```
	    	IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    		Directory dir = FSDirectory.open(new File(indexDir));
    		iwc.setSimilarity(new BM25Similarity());
    		indexWriter = new IndexWriter(dir,iwc);
    ```
* 这时使用BM25就非常容易了，只需要把对应的Similarity修改为```BM25Similarity()```
* 如果要按照原先的架构，也非常简单，只需要通过在```SimpleScorer.score```中增加如下代码
```
		float docNorm = SimpleSimilarity.decodeNorm(norms[doc]);
		float docLen = 1.0f / (docNorm * docNorm);
		float k = K1 * (1 - b + b * docLen / avgLength);
		float r = termDocs.freq() * (K1 + 1) / (termDocs.freq() + k);
		float result = idf * r;
```

###	实现VSM模型评分
* 实现VSM也非常简单，只需要把```BM25Similarity```替换为```TFIDFSimilarity```

# 拓展任务
## HTML解析
在ImageIndexer中同时对所附带的html文件进行解析并索引，并在ImageSearch中对html域也进行搜索。
关键点在于读取html文件的编码并正确解码，同时提取内容并删掉标签和控制字段。

如下图，加入html解析后即使搜索没有在标签中出现的词汇也可以检索到相关结果。
不过缺憾在于html文件无关内容较多，而正确识别与图片真正相关的字段比较困难，所以依赖于html文本检索到的图片精确度比较低
![alt tag](https://github.com/ShengjiaZhao/ImageSearch/blob/master/reportimg/html_compare.jpg)

* 实现方法为在```ImageIndexer```中加入如下代码
```
	String pathString = locate.getNodeValue();
	pathString = pathString.replace(".jpg", ".html");
	
	File fileDir = new File(imgPath + pathString);
	while (fileDir.exists() && !fileDir.isDirectory()) {	// Use while here to facilitate break
		Pattern p = Pattern.compile("charset\\s*=\\s*([0-9a-zA-Z]+)");
		Matcher m = p.matcher(new String(Files.readAllBytes(Paths.get(imgPath + pathString))));
		String encoding;
		if (m.find()) {
			String encodingStr = m.group(1);
			if (encodingStr.equalsIgnoreCase("gb2312") ||
					encodingStr.equalsIgnoreCase("gbk")) {
				encoding = "GBK";
			} else if (encodingStr.equalsIgnoreCase("utf") ||
					encodingStr.equalsIgnoreCase("utf8")) {
				encoding = "UTF8";
			} else {
				break;
			}
		} else {
			break;
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileDir), encoding));
	
		String totalStr = "", str;
		while ((str = in.readLine()) != null) {
		    //System.out.println(str);
			int beginIndex = -1;
			for (int j = 0; j < str.length();) {
		        int codepoint = str.codePointAt(j);				        
		        if (codepoint >= 0x4e00 && codepoint <= 0x9fa5) {//aracter.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
		            if (beginIndex < 0)
		            	beginIndex = j;
		        } else {
		        	if (beginIndex >= 0) {
		        		if (j - beginIndex >= 4)
		        			totalStr += str.substring(beginIndex, j).trim() + " ";
		        		beginIndex = -1;
		        	}
		        }
		        j += Character.charCount(codepoint);
			}
		}
		Field htmlTextField = new Field("htmlText" , 
				totalStr ,Field.Store.YES, Field.Index.ANALYZED);
		averageLength += totalStr.length();
		document.add(htmlTextField);
		in.close();
		break;
	}
```
同时在```ImageServer```搜索的时候在两个域中都进行搜索并合并结果
```
	TopDocs[] resultsArray = new TopDocs[2];
	resultsArray[0]=search.searchQuery(queryString, "abstract", 100);
	resultsArray[1] = search.searchQuery(queryString, "htmlText", 100);
	TopDocs results = TopDocs.merge(null, 100, resultsArray);
```
