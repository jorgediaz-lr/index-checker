# Index Checker

Index checker allows Liferay admins to check index status.

It scans both database and index, displaying:
 - missing objects
 - outdated ones
 - orphan data in the index.

<img src="screenshots/index-checker-screenshot_1.png" height="400">

## How does it work?

In order to obtain the necessary data, this portlet compares primary keys, modified dates, status, version, and other related data of both database and index.

After executing index check, the wrong data is displayed:
 - you will be able to reindex the missing or outdated objects
 - you can also remove orphan data from the index.

<img src="screenshots/index-checker-screenshot_2.png" height="400">

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

Install the correct Index Checker version depending on the Liferay version you are using.

**Liferay Quarterly Releases and Liferay 7.4 Update 100+**
   - Use Index Checker version 1.1.0
   - Download [jorgediazest.indexchecker.portlet.jar](https://github.com/jorgediaz-lr/index-checker/releases/download/1.1.0/jorgediazest.indexchecker.portlet.jar) and copy it to Liferay `deploy` folder.
   - For more information [see the 1.1.0 release documentation](https://github.com/jorgediaz-lr/index-checker/releases/tag/1.1.0)

_Index Checker 1.1.0_ works in the Liferay Quarterly Releases and Liferay 7.4 Update 100+

**Liferay DXP/Portal from 7.1 to 7.4 (up to Update 99)**
   - Use Index Checker version 1.0.6
   - Download [jorgediazest.indexchecker.portlet.jar](https://github.com/jorgediaz-lr/index-checker/releases/download/1.0.6/jorgediazest.indexchecker.portlet.jar) and copy it to Liferay `deploy` folder.
   - For more information [see the 1.0.6 release documentation](https://github.com/jorgediaz-lr/index-checker/releases/tag/1.0.6)

_Index Checker 1.0.6_ works in Liferay DXP/Portal from 7.1 to 7.4 (up to Update 99)

**Liferay 6.2 and 7.0**
   - Use Index Checker version 0.9
   - _**6.2:**_ Download [index_checker-portlet-6.2.0.9.war](https://github.com/jorgediaz-lr/index-checker/releases/download/0.9_release_6.2/index_checker-portlet-6.2.0.9.war) and copy it to Liferay `deploy` folder.
   - _**7.0:**_ Download [index_checker-portlet-7.0.0.9.war](https://github.com/jorgediaz-lr/index-checker/releases/download/0.9_release_7.0/index_checker-portlet-7.0.0.9.war) and copy it to Liferay `deploy` folder.
   - For more information see 0.9 [6.2](https://github.com/jorgediaz-lr/index-checker/releases/tag/0.9_release_6.2) and [7.x](https://github.com/jorgediaz-lr/index-checker/releases/tag/0.9_release_7.0) releases

_Index Checker 0.9_ works in:
  - Liferay Portal 6.2
  - Liferay DXP/Portal 7.0
 

