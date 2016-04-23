# Image Indexing
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