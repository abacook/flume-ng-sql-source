flume-ng-sql-source
================

该项目是基于https://github.com/keedio/flume-ng-sql-source 5.3版本基础上进行的修改，支持自定义消息格式（可删除增量字段、定义分割符、替换分割符）；由于非专业java开发，代码可能有不足之处，请谅解

Current sql database engines supported
-------------------------------
- After the last update the code has been integrated with hibernate, so all databases supported by this technology should work.

Compilation and packaging
----------
```
  $ mvn package
```

Deployment
----------

Copy flume-ng-sql-source-<version>.jar in target folder into flume plugins dir folder
```
  $ mkdir -p $FLUME_HOME/plugins.d/sql-source/lib $FLUME_HOME/plugins.d/sql-source/libext
  $ cp flume-ng-sql-source-0.8.jar $FLUME_HOME/plugins.d/sql-source/lib
```

### Specific installation by database engine

##### MySQL
Download the official mysql jdbc driver and copy in libext flume plugins directory:
```
$ wget https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.35.tar.gz
$ tar xzf mysql-connector-java-5.1.35.tar.gz
$ cp mysql-connector-java-5.1.35-bin.jar $FLUME_HOME/plugins.d/sql-source/libext
```

##### Microsoft SQLServer
Download the official Microsoft 4.1 Sql Server jdbc driver and copy in libext flume plugins directory:  
Download URL: https://www.microsoft.com/es-es/download/details.aspx?id=11774  
```
$ tar xzf sqljdbc_4.1.5605.100_enu.tar.gz
$ cp sqljdbc_4.1/enu/sqljdbc41.jar $FLUME_HOME/plugins.d/sql-source/libext
```

##### IBM DB2
Download the official IBM DB2 jdbc driver and copy in libext flume plugins directory:
Download URL: http://www-01.ibm.com/support/docview.wss?uid=swg21363866

Configuration of SQL Source:
----------
Mandatory properties in <b>bold</b>

| Property Name | Default | Description |
| ----------------------- | :-----: | :---------- |
| <b>channels</b> | - | Connected channel names |
| <b>type</b> | - | The component type name, needs to be org.keedio.flume.source.SQLSource  |
| <b>hibernate.connection.url</b> | - | Url to connect with the remote Database |
| <b>hibernate.connection.user</b> | - | Username to connect with the database |
| <b>hibernate.connection.password</b> | - | Password to connect with the database |
| <b>status.file.path</b>| /var/lib/flume | Path to save the status file |
| <b>status.file.name</b> | - | Local file name to save last row number read |
| run.query.delay | 10000 | ms to wait between run queries |
| batch.size| 100 | Batch size to send events to flume channel |
| max.rows | 10000| Max rows to import per query |
| read.only | false| Sets read only session with DDBB |
| table | - | Table to export data |
| columns.to.select | * | 查询字段|
| incremental.column.alias.name | - | 增量字段，可以自定义查询内容 |
| incremental.column.alias.name | - | 增量字段别名 |
| incremental.column.value| - | 增量字段查询开始值 |
| delimiter.entry | - | 消息分割符 |
| start.from | 0 | 增量字段开始值 |
| custom.query | - | 自定义查询sql ， $@$ 为替换增量字段标识， 必须有 $@$ ，否则会一直查询全量并sink |
| delimiter.replace | true | 是否替换分割符 |
| delimiter.replace.entry | - | 用于替换的分隔符 |
| hibernate.connection.driver_class | -| Driver class to use by hibernate, if not specified the framework will auto asign one |
| hibernate.dialect | - | Dialect to use by hibernate, if not specified the framework will auto asign one. Check https://docs.jboss.org/hibernate/orm/4.3/manual/en-US/html/ch03.html#configuration-optional-dialects for a complete list of available dialects |
| hibernate.connection.provider_class | - | Set to org.hibernate.connection.C3P0ConnectionProvider to use C3P0 connection pool (recommended for production) |
| hibernate.c3p0.min_size | - | Min connection pool size |
| hibernate.c3p0.max_size | - | Max connection pool size |
| default.charset.resultset | UTF-8 | Result set from DB converted to charset character encoding |

Standard Query
-------------
If no custom query is set, ```SELECT <columns.to.select> FROM <table>``` will be executed each ```run.query.delay``` milliseconds configured

Custom Query
-------------
A custom query is supported to bring the possibility of using the entire SQL language. This is powerful, but risky, be careful with the custom queries used.  

To avoid row export repetitions use the $@$ special character in WHERE clause, to incrementaly export not processed rows and the new ones inserted.

IMPORTANT: For proper operation of Custom Query ensure that incremental field will be returned in the first position of the Query result.

Example:
```
agent.sources.sql-source.custom.query = SELECT incrementalField,field2 FROM table1 WHERE incrementalField > $@$ 
```

Configuration example
--------------------

```properties
###########sources#################
####s1######
a1.sources.s1.type = org.keedio.flume.source.SQLSourceOriginal
a1.sources.s1.hibernate.connection.url = jdbc:postgresql://xxx.xxx.xxx.xxx:5432/postgres
a1.sources.s1.hibernate.connection.user = xxx
a1.sources.s1.hibernate.connection.password = xxx
a1.sources.s1.hibernate.connection.autocommit = flase
a1.sources.s1.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
a1.sources.s1.hibernate.connection.driver_class = org.postgresql.Driver
a1.sources.s1.run.query.delay = 300000
a1.sources.s1.status.file.path = ./status/
a1.sources.s1.status.file.name = test.status


#### table config ##### 
# 默认查询会将增量字段放在第一列，发送时第一列会被剔除 
# a1.sources.s1.table = xxxxx
# a1.sources.s1.columns.to.select = *
# a1.sources.s1.incremental.column.name = tid
# a1.sources.s1.incremental.column.alias.name = ttttid 
# a1.sources.s1.incremental.column.value =10
# a1.sources.s1.delimiter.entry = ,
# a1.sources.s1.delimiter.replace = true
# a1.sources.s1.delimiter.replace.entry = "|||"


#### custom query ##### 
# 第一列必须为增量字段，发送时第一列会被剔除 
a1.sources.s1.start.from = 0
a1.sources.s1.custom.query =  select tid, row_to_json(t)||'' from (select tid, a1 as "A1", a1 as "A2" from test where tid > $@$ ) t 
a1.sources.s1.delimiter.entry = ,
a1.sources.s1.delimiter.replace = false


a1.sources.s1.batch.size = 10
a1.sources.s1.max.rows = 10
a1.sources.s1.hibernate.connection.provider_class = org.hibernate.connection.C3P0ConnectionProvider
a1.sources.s1.hibernate.c3p0.min_size=1
a1.sources.s1.hibernate.c3p0.max_size=10

# The channel can be defined as follows.
a1.channels.c1.type = memory
a1.channels.c1.capacity = 10000
a1.channels.c1.transactionCapacity = 10000
a1.channels.c1.byteCapacityBufferPercentage = 20
a1.channels.c1.byteCapacity = 800000
```

Known Issues
---------
An issue with Java SQL Types and Hibernate Types could appear Using SQL Server databases and SQL Server Dialect coming with Hibernate.  
  
Something like:
```
org.hibernate.MappingException: No Dialect mapping for JDBC type: -15
```

Use ```org.keedio.flume.source.SQLServerCustomDialect``` in flume configuration file to solve this problem.

Special thanks
---------------

I used flume-ng-kafka to guide me (https://github.com/baniuyao/flume-ng-kafka-source.git).
Thanks to [Frank Yao](https://github.com/baniuyao).

Version History
---------------
+ Version 1.5.1 added charset encoding for result set is now configurable.
+ Stable version is 1.5.0 (compatible with Apache Flume 1.8.0)
+ Previous stable version is 1.4.3 (compatible with Apache Flume prior to 1.7.0)
