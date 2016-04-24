# ��������ڶ�����ҵ����
## ��������

###	��ͨ����
ѧϰ��Tomcat��MyEclipse��Lucene��ʹ�ã���ͨ��ImageIndexer������������Ҫע����Ǵ�ŵ�·����
Ϊ����ȷ����Tomcat��forIndex�ļ���Ӧ������Tomcat��װĿ¼�£���ͼƬӦ���ŵ�TOMCAT_PATH\webapps\ROOT\pictures\��

###	ʵ��BM25�����㷨
* Ϊ��ʹ��lucene4.0���¹��ܣ���������ֲ��Lucene4.0����������
  	1. ����Lucene4.0��jar��lucene-core-4.0.0.jar�����滻ԭ�ȵ�3.5�汾jar����ע���κθ���4.0�İ汾������ּ��������⣬�κε���4.0�İ汾Ҳ����ּ���������
	2. ��https://code.google.com/archive/p/ik-analyzer/����IKAnalyzer��IK Analyzer 2012FF_hf1 �����汾��lucene4.0�����ݡ��κ������汾�������Զ����ڼ���������
	3. ��ΪLucene3.x��Lucene4.x���ش�Ľӿڱ仯����Ҫ��Դ������Ӧ�����޸ģ����罫```ImageIndexer```���캯���޸�Ϊ
	```
	    	IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    		Directory dir = FSDirectory.open(new File(indexDir));
    		iwc.setSimilarity(new BM25Similarity());
    		indexWriter = new IndexWriter(dir,iwc);
    ```
* ��ʱʹ��BM25�ͷǳ������ˣ�ֻ��Ҫ�Ѷ�Ӧ��Similarity�޸�Ϊ```BM25Similarity()```
* ���Ҫ����ԭ�ȵļܹ���Ҳ�ǳ��򵥣�ֻ��Ҫͨ����```SimpleScorer.score```���������´���
```
		float docNorm = SimpleSimilarity.decodeNorm(norms[doc]);
		float docLen = 1.0f / (docNorm * docNorm);
		float k = K1 * (1 - b + b * docLen / avgLength);
		float r = termDocs.freq() * (K1 + 1) / (termDocs.freq() + k);
		float result = idf * r;
```

###	ʵ��VSMģ������
* ʵ��VSMҲ�ǳ��򵥣�ֻ��Ҫ��```BM25Similarity```�滻Ϊ```TFIDFSimilarity```

# ��չ����
## HTML����
��ImageIndexer��ͬʱ����������html�ļ����н���������������ImageSearch�ж�html��Ҳ����������
�ؼ������ڶ�ȡhtml�ļ��ı��벢��ȷ���룬ͬʱ��ȡ���ݲ�ɾ����ǩ�Ϳ����ֶΡ�

����ͼ������html������ʹ����û���ڱ�ǩ�г��ֵĴʻ�Ҳ���Լ�������ؽ����
����ȱ������html�ļ��޹����ݽ϶࣬����ȷʶ����ͼƬ������ص��ֶαȽ����ѣ�����������html�ı���������ͼƬ��ȷ�ȱȽϵ�
![alt tag](https://github.com/ShengjiaZhao/ImageSearch/blob/master/reportimg/html_compare.jpg)

* ʵ�ַ���Ϊ��```ImageIndexer```�м������´���
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
ͬʱ��```ImageServer```������ʱ�����������ж������������ϲ����
```
	TopDocs[] resultsArray = new TopDocs[2];
	resultsArray[0]=search.searchQuery(queryString, "abstract", 100);
	resultsArray[1] = search.searchQuery(queryString, "htmlText", 100);
	TopDocs results = TopDocs.merge(null, 100, resultsArray);
```
