Jieba Analysis for ElasticSearch
================================

项目基于 https://github.com/huaban/elasticsearch-analysis-jieba

插件基于[jieba分词](https://github.com/huaban/jieba-analysis)实现了elasticsearch的接口，由于花瓣的[插件](https://github.com/huaban/elasticsearch-analysis-jieba)没有实现tokenizer，参见[issue](https://github.com/huaban/elasticsearch-analysis-jieba/issues/5)，这里自己实现了一个jieba tokenizer，从而可以使用Lucene自带的synonym filter。


Installation
------------

**compile and package current project**

```
git clone https://github.com/yilee/elasticsearch-analysis-jieba.git
cd elasticsearch-analysis-jieba
mvn package
```

**make a direcotry in elasticsearch' plugin directory**

```
cd {your_es_path}
mkdir plugins/jieba
```

**copy jieba-analysis-1.0.2.jar and elasticsearch-analysis-jieba-0.0.4-SNAPSHOT.jar commons-lang3-3.3.2.jar to plugins/jieba**

```
cp jieba-analysis-1.0.2.jar {your_es_path}/plugins/jieba
cd elasticsearch-analysis-jieba
cp target/elasticsearch-analysis-jieba-0.0.4-SNAPSHOT.jar {your_es_path}/plugins/jieba
```

**copy user dict to config/jieba**

```
cp -r data/jieba {your_es_path}/config/
```

that's all!


Usage
-----

elasticsearch配置示例

```javascript
index:
    analysis:
        tokenizer:
            jieba:
                type: jieba
        filter:
            synonym:
                type: synonym
                ignore_case: true
                expand: true
                synonyms_path: "synonym.txt"
        analyzer:
            jieba: 
                tokenizer: jieba
                filter: [synonym]
                seg_mode : "index"
                stop : true


```

使用示例

```sh
curl 'http://localhost:9200/useridx/_analyze?analyzer=jieba&pretty' -d '北京交通大学linkedin';echo
```

返回

```javascript
{
  "tokens" : [ {
    "token" : "北京",
    "start_offset" : 0,
    "end_offset" : 2,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "交通",
    "start_offset" : 2,
    "end_offset" : 4,
    "type" : "word",
    "position" : 2
  }, {
    "token" : "大学",
    "start_offset" : 4,
    "end_offset" : 6,
    "type" : "word",
    "position" : 3
  }, {
    "token" : "北交",
    "start_offset" : 0,
    "end_offset" : 6,
    "type" : "SYNONYM",
    "position" : 4
  }, {
    "token" : "北京交通大学",
    "start_offset" : 0,
    "end_offset" : 6,
    "type" : "SYNONYM",
    "position" : 4
  }, {
    "token" : "领英中国",
    "start_offset" : 6,
    "end_offset" : 14,
    "type" : "SYNONYM",
    "position" : 5
  }, {
    "token" : "linkedin",
    "start_offset" : 6,
    "end_offset" : 14,
    "type" : "SYNONYM",
    "position" : 5
  } ]
}
```

License
-------

```
This software is licensed under the Apache 2 license, quoted below.

Copyright (C) 2013 libin and Huaban Inc<http://www.huaban.com>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
