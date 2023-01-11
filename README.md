Elasticsearch Library for Spring Boot
-------
This library provides utilities that make it easy to integrate Elasticsearch into spring boot project

Feature List:
* [Handle search](#Handle-search)
* [Multi search](#Multi-search)
* [Match phrase prefix search](#Match-phrase-prefix-search)
* [Regexp search](#Regexp-search)
* [Match phrase search](#Match-phrase-search)
* [Boosting search](#Boosting-search)
* [Fuzzy search](#Fuzzy-search)
* [Wildcard search](#Wildcard-search)

Quick start
-------
* Just add the dependency to an existing Spring Boot project
```xml
<dependency>
    <groupId>com.atviettelsolutions</groupId>
    <artifactId>vts-kit-lib-elasticsearch-data</artifactId>
    <version>1.0.0</version>
</dependency>
```

* Then, add the following properties to your `application.properties` file.
```yaml
spring.elasticsearch.username=
spring.elasticsearch.password=
spring.elasticsearch.uris= localhost #host name : 9200 #port number
```

Usage
-------
We wrapped several function: First at all, you must inject ElasticsearchService
```java
@Autowired
ElasticsearchService elasticsearchService;
```
##### Handle search
```java
elasticsearchService.handleSearch(String indexName, searchRequestDTO, boolQueryBuilder, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "iphone",
"sortBy" : "id",
"orderBy" : "ASC",
"page" : 0,
"size" : 10
}
indexName = "phone"
BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("description", textSearch).slop(10).maxExpansions(10));
responseClass = PhoneResponse.class
```

##### Multi search
```java
elasticsearchService.multiSearch(String indexName, searchRequestDTO, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "iphone",
"fields" : ["id","description","number"],
"sortBy" : "id",
"orderBy" : "ASC",
"page" : 0,
"size" : 10
}
indexName = "phone"
responseClass = PhoneResponse.class
```
Note:
- If you leave an empty fields("fields" : []), it will search on all fields.

##### Match phrase prefix search
```java
elasticsearchService.matchPhrasePrefixSearch(String indexName, searchRequestDTO, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "iphone 7 pr",
"fields" : ["name"],
"sortBy" : "id",
"orderBy" : "ASC",
"page" : 0,
"size" : 10,
"slop" : 10,
"maxExpansions" : 20
}
indexName = "phone"
responseClass = PhoneResponse.class
```
Note:
- If you don't enter the slop, maxExpansions attributes, the default return slop = 10 and maxExpansions = 10.

##### Regexp search
```java
elasticsearchService.regexpSearch(String indexName, searchRequestDTO, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "t[a-z]*y"
"fields" : ["name"]
"sortBy" : "id"
"orderBy" : "ASC"
"page" : 0
"size" : 10
}
indexName = "phone"
responseClass = PhoneResponse.class
```

##### Match phrase search
```java
elasticsearchService.matchPhraseSearch(String indexName, searchRequestDTO, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "iphone pro max",
"fields" : ["name"],
"sortBy" : "id",
"orderBy" : "ASC",
"page" : 0,
"size" : 10,
"slop" : 20
}
indexName = "phone"
responseClass = PhoneResponse.class
```
Note:
- If you don't enter the slop attribute, the default return slop = 10

##### Boosting search
```java
elasticsearchService.boostingSearch(String indexName, searchRequestDTO, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "iphone",
"fieldsAndWeights" : ["name" : 5, "description" : 2],
"sortBy" : "id",
"orderBy" : "ASC",
"page" : 0,
"size" : 10
}
indexName = "phone"
responseClass = PhoneResponse.class
```

##### Fuzzy search
```java
elasticsearchService.fuzzySearch(String indexName, searchRequestDTO, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "iphone",
"fields" : ["name", "description"],
"sortBy" : "id",
"orderBy" : "ASC",
"page" : 0,
"size" : 10
}
indexName = "phone"
responseClass = PhoneResponse.class
```
Note:
- Default value of fuzziness = "AUTO"

##### Wildcard search
```java
elasticsearchService.wildCardSearch(String indexName, searchRequestDTO, Class responseClass);
```

Example:
```yaml
SearchRequestDTO.class
{
"textSearch" : "iph*",
"fields" : ["name", "description"],
"sortBy" : "id",
"orderBy" : "ASC",
"page" : 0,
"size" : 10
}
indexName = "phone"
responseClass = PhoneResponse.class
```

* Build with Unittest
```shell script
mvn clean install
```

* Build without Unittest
```shell script
mvn clean install -DskipTests
```

Notes
-------
- If you don't enter the page, size attributes, the default returns page = 0 and size = 50 with default values.
- If you don't enter the sortBy, orderBy attributes, the default return type DESC based on the score of the search result.
- You must use SearchRequestDTO to be provided by us.

Contribute
-------
Please refer [Contributors Guide](CONTRIBUTING.md)

License
-------
This code is under the [MIT License](https://opensource.org/licenses/MIT).

See the [LICENSE](LICENSE) file for required notices and attributions.
