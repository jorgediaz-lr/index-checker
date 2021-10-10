# Index Checker

Index checker allows Liferay admins to check index status.

It scans both database and index, displaying:
 - missing objects
 - outdated ones
 - orphan data in index.

### How it works?

In order to obtain the necessary data, this portlet compares primary keys, modified dates, status, version and other related data data of both database and index.

After executing index check, the wrong data is displayed:
 - you will be able to reindex the missing or out dated objects
 - you can also remove orphan data from index.

### Filtering data:

Before executing the analysis, you can apply several filters:
 - Filter by entities
 - Filter by sities
 - Filter by modified date (objects modified last hour, last week, last month, etc.)

This will help you in case your system has a lot of data.

For example, if you have a lot of webcontents in your system: _it is possible to reindex the webcontents from only one site_

### Aditional options

In the configuration page you will able to:
 - Group the output by sites.
 - Execute queries site by site: You can save memory executing, but this will be slower.
 
 In the output you can also display the correctly indexed information just in case you want to double check it.
 
