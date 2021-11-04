# Index Checker

Index checker allows Liferay admins to check index status.

It scans both database and index, displaying:
 - missing objects
 - outdated ones
 - orphan data in the index.

## How it works?

In order to obtain the necessary data, this portlet compares primary keys, modified dates, status, version, and other related data data of both database and index.

After executing index check, the wrong data is displayed:
 - you will be able to reindex the missing or outdated objects
 - you can also remove orphan data from the index.

### Filtering data:

Before executing the analysis, you can apply several filters:
 - Filter by entities
 - Filter by sites
 - Filter by modified date (objects modified last hour, last week, last month, etc.)

This will help you in case your system has a lot of data.

For example, if you have a lot of web contents in your system: _it is possible to reindex the web contents from only one site_

### Additional options

On the configuration page you will be able to:
 - Group the output by sites.
 - Execute queries site by site: You can save memory executing, but this will be slower.
 
 In the output, you can also display the correctly indexed information just in case you want to double-check it.

## Installation

**Index Checker 1.0.0:**
   - Download [jorgediazest.indexchecker.portlet.jar](https://github.com/jorgediaz-lr/index-checker/releases/download/1.0.0/jorgediazest.indexchecker.portlet.jar) and copy it to Liferay `deploy` folder.
   - For more information [see 1.0.0 release](https://github.com/jorgediaz-lr/index-checker/releases/tag/1.0.0)

_Index Checker 1.0.0_ works in Liferay DXP/Portal from 7.1 to 7.4

**Index Checker 0.9:**
   - _**Liferay 6.2:**_ Download [index_checker-portlet-6.2.0.9.war](https://github.com/jorgediaz-lr/index-checker/releases/download/0.9_release_6.2/index_checker-portlet-6.2.0.9.war) and copy it to Liferay `deploy` folder.
   - _**Liferay 7.0-7.2:**_ Download [index_checker-portlet-7.0.0.9.war](https://github.com/jorgediaz-lr/index-checker/releases/download/0.9_release_7.0/index_checker-portlet-7.0.0.9.war) and copy it to Liferay `deploy` folder.
   - For more information see 0.9 [6.2](https://github.com/jorgediaz-lr/index-checker/releases/tag/0.9_release_6.2) and [7.x](https://github.com/jorgediaz-lr/index-checker/releases/tag/0.9_release_7.0) releases

_Index Checker 0.9_ works in:
  - Liferay Portal 6.2
  - Liferay DXP/Portal 7.0 to 7.2
 

