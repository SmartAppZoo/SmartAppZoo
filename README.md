# SmartAppZoo

The **SmartAppZoo** dataset contains a total of 3,526 SmartThings apps, including 184 SmartThings official apps, 468 IoTBench third-party apps, and 2,874 Github third-party apps. We always welcome contributions to the dataset from the community.


## Detail

1. We crawled SmartApps from Github repositories between January 1, 2013 and December 31, 2022. 

2. We obtained 184 official apps from the  [**SmartThings Public GitHub Repo**](https://github.com/SmartThingsCommunity/SmartThingsPublic).

3. We obtained 468 cleaned third-party apps from the [**IoTBench dataset**](https://github.com/IoTBench/IoTBench-test-suite), excluding hand-crafted malicious SmartApps.

4. We obtained 2,874 cleaned third-party apps from other Github repositories, avoiding those used for adversarial SmartApp research.

## Conversion

1. We developed a converter tool [**ConvertGroovyToNodeJS.ipynb**](https://github.com/SmartAppZoo/ConvertGroovyToNodeJS/blob/main/ConvertGroovyToNodeJS.ipynb) that converts Groovy SmartApps into Node.js SmartApps.

2. The converter can translate the fixed-format parts of Groovy SmartApps, such as definition/preference blocks, subscription/schedule functions, and device commands. from Groovy to Node.js. However, the conversion of other user-defined functions requires the help of the research community. 

3. Currently, the converter can accurately translate approximately 50 simple Groovy SmartApps into Node.js SmartApps. The converted results are located in the repository [**ConvertGroovyToNodeJS**](https://github.com/SmartAppZoo/ConvertGroovyToNodeJS).

## Warning

When visiting the [**Github Third-Party**](Github&#32;Third-Party) folder via a browser, you may receive the following warning:

```
Sorry, we had to truncate this directory to 1,000 files. 1,874 entries were omitted from the list.
```

This is a common practice on Github. Github limits the number of files that can be listed in the webview to 1000. The warning only appears when using a browser, but you can still fork, clone, or download the repository without issues.

## Contributing

We welcome submissions of issues and pull requests. 

## Credit

SmartAppZoo uses SmartApps from 1207 repositories. We have listed all the Github usernames and repository names in [**Credit.md**](/Credit.md).

